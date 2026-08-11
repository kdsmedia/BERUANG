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
            bio = "Hey there! I am using JavaGoat.",
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

    suspend fun update(name: String?, bio: String?, avatarUrl: String?): Profile? {
        val uid = currentUid ?: return null
        val data = buildMap {
            name?.let { put("full_name", it) }
            bio?.let { put("bio", it) }
            avatarUrl?.let { put("avatar_url", it) }
        }
        if (data.isNotEmpty()) profiles.document(uid).update(data).await()
        val updated = get(uid)
        currentProfile = updated
        return updated
    }

    fun clearCache() { cache.clear(); currentProfile = null }
}
