package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import android.util.Log
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountsRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val profiles get() = postgrest.from("profiles")
    private val wallets get() = postgrest.from("wallets")
    private val transactions get() = postgrest.from("transactions")

    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    /** SHA-256 hash of a 4-digit PIN (we never store the plaintext). */
    fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(("beruang:$pin").toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Ensures the current user has a unique 6-digit account_id (generates if missing). */
    private suspend fun ensureAccountId(profile: Profile): Profile {
        if (!profile.account_id.isNullOrBlank()) return profile
        var attempts = 0
        var newId = ""
        while (attempts < 12) {
            newId = random6()
            val snap = runCatching {
                profiles.select { filter { eq("account_id", newId) } }.decodeList<Profile>()
            }.getOrDefault(emptyList())
            if (snap.isEmpty()) break
            attempts++
        }
        profiles.update({ set("account_id", newId) }) { filter { eq("id", profile.id) } }
        return profile.copy(account_id = newId)
    }

    private fun random6(): String = (100000 + (Math.random() * 900000).toInt()).toString()

    /** Ensures the current user has a 6-digit account_id; returns the profile. */
    suspend fun ensureMyAccountId(): Profile {
        val me = uid()
        val p = runCatching { profiles.select { filter { eq("id", me) } }.decodeSingle<Profile>() }.getOrNull()
            ?: Profile(id = me, full_name = "User")
        return ensureAccountId(p)
    }

    /** Reads a wallet balance, defaulting to 0. Creates the row for the current user if missing. */
    suspend fun getBalance(userId: String): Long {
        val rows = runCatching {
            wallets.select { filter { eq("id", userId) } }.decodeList<WalletRow>()
        }.getOrDefault(emptyList())
        if (rows.isEmpty() && userId == uid()) {
            runCatching { wallets.insert(WalletRow(id = userId, balance = 0)) }
            return 0L
        }
        return rows.firstOrNull()?.balance ?: 0L
    }

    /** Sets (or changes) the current user's 4-digit transaction PIN. */
    suspend fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN harus 4 digit angka." }
        profiles.update({ set("points_pin", hashPin(pin)) }) { filter { eq("id", uid()) } }
    }

    suspend fun hasPin(): Boolean {
        val me = uid()
        val p = runCatching { profiles.select { filter { eq("id", me) } }.decodeSingle<Profile>() }.getOrNull()
        return !p?.points_pin.isNullOrBlank()
    }

    /**
     * Awards points to a user via the award_points RPC (the RLS blocks direct
     * wallet updates from clients, so we go through a security-definer function).
     */
    suspend fun awardPoints(userId: String, amount: Long) {
        try {
            postgrest.rpc("award_points", AwardPointsParams(p_user = userId, p_amount = amount))
        } catch (e: Exception) {
            // Best-effort: never let a reward failure break the user's action,
            // but log it so silent rule/permission regressions are visible.
            Log.w("AccountsRepo", "awardPoints failed (uid=$userId, +$amount)", e)
        }
    }

    /** Looks up a profile by its 6-digit account_id (used after QR scan). */
    suspend fun findByAccountId(accountId: String): Profile? {
        if (accountId.isBlank()) return null
        return runCatching {
            profiles.select { filter { eq("account_id", accountId) } }.decodeList<Profile>()
        }.getOrDefault(emptyList()).firstOrNull()
    }

    /**
     * Transfers [amount] points from the current user to [toAccountId] via the
     * atomic transfer_points RPC (security definer) — the PIN check and balance
     * debit/credit all happen server-side in one transaction.
     */
    suspend fun transfer(toAccountId: String, amount: Long, pin: String): TransferResult {
        if (amount <= 0) return TransferResult.Error("Nominal harus lebih dari 0.")
        if (pin.length != 4 || !pin.all { it.isDigit() }) return TransferResult.Error("PIN harus 4 digit angka.")
        val me = uid()
        if (toAccountId.isBlank()) return TransferResult.Error("QR tujuan tidak valid.")

        val result = runCatching {
            postgrest.rpc(
                "transfer_points",
                TransferParams(p_from = me, p_to_account = toAccountId, p_amount = amount, p_pin = hashPin(pin)),
            ).decodeAs<String>()
        }.getOrNull() ?: return TransferResult.Error("Transfer gagal.")

        if (result != "OK") return TransferResult.Error(result)
        val recipient = findByAccountId(toAccountId)
            ?: return TransferResult.Success("Recipient", amount)
        return TransferResult.Success(recipient.displayName, amount)
    }
}

@Serializable
private data class WalletRow(val id: String = "", val balance: Long = 0L)

@Serializable
private data class AwardPointsParams(val p_user: String, val p_amount: Long)

@Serializable
private data class TransferParams(
    val p_from: String,
    val p_to_account: String,
    val p_amount: Long,
    val p_pin: String,
)

sealed class TransferResult {
    data class Success(val recipientName: String, val amount: Long) : TransferResult()
    data class Error(val message: String) : TransferResult()
}
