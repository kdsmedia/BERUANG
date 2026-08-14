package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val table get() = postgrest.from("profiles")
    private val cache = HashMap<String, Profile>()

    val currentUid get() = auth.currentUserOrNull()?.id
    private var currentProfile: Profile? = null

    suspend fun loadMyProfile(): Profile? {
        val uid = currentUid ?: return null
        val p = runCatching { table.select { filter { eq("id", uid) } }.decodeSingle<Profile>() }.getOrNull()
        if (p != null) { currentProfile = p; cache[uid] = p; return p }
        // fallback if missing (trigger should have created it; safety net)
        val fallback = Profile(
            id = uid,
            full_name = "New Goat",
            bio = "Hey there! I am using BERUANG.",
            avatar_url = Profile.dicebearAvatar(uid)
        )
        table.upsert(fallback, onConflict = "id")
        currentProfile = fallback; cache[uid] = fallback
        return fallback
    }

    fun myProfile() = currentProfile

    suspend fun get(uid: String): Profile {
        cache[uid]?.let { return it }
        val p = runCatching { table.select { filter { eq("id", uid) } }.decodeSingle<Profile>() }.getOrNull()
            ?: Profile(id = uid, full_name = "User", avatar_url = Profile.dicebearAvatar(uid))
        cache[uid] = p
        return p
    }

    suspend fun list(limit: Int = 60): List<Profile> {
        val rows = runCatching {
            table.select { limit(limit.toLong()) }.decodeList<Profile>()
        }.getOrDefault(emptyList())
        rows.forEach { p -> cache[p.id] = p }
        return rows
    }

    suspend fun update(
        name: String?, bio: String?, avatarUrl: String?,
        phone: String? = null, email: String? = null, gender: String? = null
    ): Profile? {
        val uid = currentUid ?: return null
        val base = cache[uid] ?: runCatching { table.select { filter { eq("id", uid) } }.decodeSingle<Profile>() }.getOrNull()
        val emailVal = email?.ifBlank { null }
        val merged = (base ?: Profile(id = uid)).copy(
            full_name = name ?: (base?.full_name),
            bio = bio ?: (base?.bio),
            avatar_url = avatarUrl ?: (base?.avatar_url),
            phone = phone ?: (base?.phone),
            email = emailVal ?: (base?.email),
            gender = gender ?: (base?.gender)
        )
        table.upsert(merged, onConflict = "id")
        cache[uid] = merged
        currentProfile = merged
        return merged
    }

    fun clearCache() { cache.clear(); currentProfile = null }
}
