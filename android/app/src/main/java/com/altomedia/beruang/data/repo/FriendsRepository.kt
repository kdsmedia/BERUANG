package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Friendship
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val fs = db.collection("friendships")
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    /** All friendship rows where I am user_id OR friend_id. */
    private suspend fun mine(): List<Friendship> {
        val a = fs.whereEqualTo("user_id", uid()).get().await()
        val b = fs.whereEqualTo("friend_id", uid()).get().await()
        val aRows = a.documents.mapNotNull { it.toObject(Friendship::class.java)?.copy(id = it.id) }
        val bRows = b.documents.mapNotNull { it.toObject(Friendship::class.java)?.copy(id = it.id) }
        return (aRows + bRows).distinctBy { it.id }
    }

    suspend fun state(): FriendState {
        val rows = mine()
        val accepted = mutableSetOf<String>()
        val pendingIn = mutableListOf<Friendship>()
        val pendingOut = mutableSetOf<String>()
        rows.forEach { f ->
            when {
                f.status == "accepted" -> {
                    accepted.add(if (f.user_id == uid()) f.friend_id else f.user_id)
                }
                f.status == "pending" && f.friend_id == uid() -> pendingIn.add(f)
                f.status == "pending" && f.user_id == uid() -> pendingOut.add(f.friend_id)
            }
        }
        return FriendState(accepted, pendingIn, pendingOut)
    }

    suspend fun sendRequest(targetUid: String, repo: FeedRepository) {
        if (targetUid == uid()) return
        val existing = fs.whereEqualTo("user_id", uid()).whereEqualTo("friend_id", targetUid).limit(1).get().await()
        if (existing.isEmpty) {
            fs.add(Friendship(user_id = uid(), friend_id = targetUid, status = "pending")).await()
            repo.createNotif(targetUid, "friend_request", null, "sent you a friend request")
        }
    }

    suspend fun accept(friendship: Friendship, repo: FeedRepository) {
        fs.document(friendship.id).update("status", "accepted").await()
        val other = if (friendship.user_id == uid()) friendship.friend_id else friendship.user_id
        repo.createNotif(other, "friend_accept", null, "accepted your friend request")
    }

    suspend fun decline(friendship: Friendship) {
        fs.document(friendship.id).delete().await()
    }

    suspend fun remove(otherUid: String) {
        val rows = mine().filter { it.user_id == otherUid || it.friend_id == otherUid }
        rows.forEach { fs.document(it.id).delete().await() }
    }
}

data class FriendState(
    val accepted: Set<String>,
    val pendingIn: List<Friendship>,
    val pendingOut: Set<String>
) {
    fun isFriend(uid: String) = accepted.contains(uid)
    fun pendingIncomingIds() = pendingIn.map { it.user_id }.toSet()
}
