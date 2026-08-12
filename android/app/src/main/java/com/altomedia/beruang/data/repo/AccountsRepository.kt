package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val profiles = db.collection("profiles")
    private val wallets = db.collection("wallets")
    private val transactions = db.collection("transactions")

    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

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
            val snap = profiles.whereEqualTo("account_id", newId).limit(1).get().await()
            if (snap.isEmpty) break
            attempts++
        }
        profiles.document(profile.id).set(mapOf("account_id" to newId), com.google.firebase.firestore.SetOptions.merge()).await()
        return profile.copy(account_id = newId)
    }

    private fun random6(): String = (100000 + (Math.random() * 900000).toInt()).toString()

    /** Ensures the current user has a 6-digit account_id; returns the profile. */
    suspend fun ensureMyAccountId(): Profile {
        val me = uid()
        val snap = profiles.document(me).get().await()
        val p = snap.toObject(Profile::class.java)?.copy(id = me)
            ?: Profile(id = me, full_name = auth.currentUser?.displayName ?: "User")
        return ensureAccountId(p)
    }

    /** Reads a wallet balance, defaulting to 0 (creates the doc for the current user if missing). */
    suspend fun getBalance(userId: String): Long {
        val snap = wallets.document(userId).get().await()
        val bal = (snap.toObject(WalletDoc::class.java)?.balance ?: 0L)
        if (!snap.exists() && userId == uid()) {
            wallets.document(userId).set(WalletDoc(balance = 0L)).await()
        }
        return bal
    }

    /** Sets (or changes) the current user's 4-digit transaction PIN. */
    suspend fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN harus 4 digit angka." }
        profiles.document(uid()).set(mapOf("points_pin" to hashPin(pin)), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun hasPin(): Boolean {
        val me = uid()
        val snap = profiles.document(me).get().await()
        return !snap.toObject(Profile::class.java)?.points_pin.isNullOrBlank()
    }

    /** Awards points to a user (used on post/comment/friend). Writes to wallets. */
    suspend fun awardPoints(userId: String, amount: Long) {
        val current = getBalance(userId)
        wallets.document(userId).set(WalletDoc(balance = current + amount)).await()
    }

    /** Looks up a profile by its 6-digit account_id (used after QR scan). */
    suspend fun findByAccountId(accountId: String): Profile? {
        if (accountId.isBlank()) return null
        val snap = profiles.whereEqualTo("account_id", accountId).limit(1).get().await()
        val doc = snap.documents.firstOrNull() ?: return null
        return doc.toObject(Profile::class.java)?.copy(id = doc.id)
    }

    /**
     * Transfers [amount] points from the current user to [toAccountId].
     * Validates the sender's PIN and sufficient balance. Returns a result.
     */
    suspend fun transfer(toAccountId: String, amount: Long, pin: String): TransferResult {
        if (amount <= 0) return TransferResult.Error("Nominal harus lebih dari 0.")
        if (pin.length != 4 || !pin.all { it.isDigit() }) return TransferResult.Error("PIN harus 4 digit angka.")
        val me = uid()
        if (toAccountId.isBlank()) return TransferResult.Error("QR tujuan tidak valid.")
        if (toAccountId == me) return TransferResult.Error("Tidak bisa transfer ke akun sendiri.")

        val myProfileSnap = profiles.document(me).get().await()
        val myProfile = myProfileSnap.toObject(Profile::class.java)?.copy(id = me)
            ?: return TransferResult.Error("Profil tidak ditemukan.")
        if (myProfile.points_pin.isNullOrBlank()) return TransferResult.Error("Anda belum mengatur PIN transaksi.")
        if (hashPin(pin) != myProfile.points_pin) return TransferResult.Error("PIN salah.")
        if (myProfile.account_id == toAccountId) return TransferResult.Error("Tidak bisa transfer ke akun sendiri.")

        val recipient = findByAccountId(toAccountId)
            ?: return TransferResult.Error("Akun tujuan tidak ditemukan.")

        val myBalance = getBalance(me)
        if (myBalance < amount) return TransferResult.Error("Poin tidak cukup. Saldo: $myBalance")

        // Debit sender, credit recipient, record transaction.
        wallets.document(me).set(WalletDoc(balance = myBalance - amount)).await()
        awardPoints(recipient.id, amount)
        transactions.add(Transaction(from_id = me, to_id = recipient.id, amount = amount)).await()
        return TransferResult.Success(recipient.displayName, amount)
    }
}

/** Internal mapping for the wallets collection. */
private data class WalletDoc(val balance: Long = 0L) {
    constructor() : this(0L)
}

sealed class TransferResult {
    data class Success(val recipientName: String, val amount: Long) : TransferResult()
    data class Error(val message: String) : TransferResult()
}
