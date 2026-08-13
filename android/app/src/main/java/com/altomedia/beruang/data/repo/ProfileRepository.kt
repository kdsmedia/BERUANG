package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val profiles = db.collection("profiles")
    private val cache = HashMap<String, Profile>()

    val currentUid get() = auth.currentUser?.uid
    private var currentProfile: Profile? = null

    suspend fun loadMyProfile(): Profile? {
        val uid = currentUid ?: return null
        val snap = profiles.document(uid).get().await()
        val p = snap.toObject(Profile::class.java)?.copy(id = uid)
        if (p != null) { currentProfile = p; cache[uid] = p; return p }
        // trigger fallback if missing
        val fallback = Profile(
            id = uid,
            full_name = auth.currentUser?.displayName ?: "New Goat",
            bio = "Hey there! I am using BERUANG.",
            avatar_url = Profile.dicebearAvatar(uid)
        )
        profiles.document(uid).set(fallback).await()
        currentProfile = fallback; cache[uid] = fallback
        return fallback
    }

    fun myProfile() = currentProfile

    suspend fun get(uid: String): Profile {
        cache[uid]?.let { return it }
        val snap = profiles.document(uid).get().await()
        val p = snap.toObject(Profile::class.java)?.copy(id = uid)
            ?: Profile(id = uid, full_name = "User", avatar_url = Profile.dicebearAvatar(uid))
        cache[uid] = p
        return p
    }

    suspend fun list(limit: Int = 60): List<Profile> {
        val snaps = profiles.limit(limit.toLong()).get().await()
        return snaps.documents.mapNotNull {
            it.toObject(Profile::class.java)?.copy(id = it.id)?.also { p -> cache[p.id] = p }
        }
    }

    suspend fun update(
        name: String?, bio: String?, avatarUrl: String?,
        phone: String? = null, email: String? = null, gender: String? = null
    ): Profile? {
        val uid = currentUid ?: return null
        // Use set+merge (upsert) instead of update() so editing works even when
        // the profile document doesn't exist yet (update() throws on a missing
        // doc, which was breaking Edit Profile).
        val base = cache[uid] ?: runCatching { profiles.document(uid).get().await() }
            .getOrNull()?.toObject(Profile::class.java)?.copy(id = uid)
        // Treat email as "clear" only when explicitly set to empty; null means "leave unchanged".
        val emailVal = email?.ifBlank { null }
        val merged = (base ?: Profile(id = uid)).copy(
            full_name = name ?: (base?.full_name),
            bio = bio ?: (base?.bio),
            avatar_url = avatarUrl ?: (base?.avatar_url),
            phone = phone ?: (base?.phone),
            email = emailVal ?: (base?.email),
            gender = gender ?: (base?.gender)
        )
        profiles.document(uid).set(merged, com.google.firebase.firestore.SetOptions.merge()).await()
        cache[uid] = merged
        currentProfile = merged
        return merged
    }

    fun clearCache() { cache.clear(); currentProfile = null }
}
