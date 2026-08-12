package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val notifs = db.collection("notifications")
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    suspend fun list(): List<Notification> {
        // whereEqualTo("user_id") + orderBy("created_at") needs a composite
        // index; fetch plain and sort in memory so notifications always load.
        val snap = notifs.whereEqualTo("user_id", uid()).limit(80).get().await()
        return snap.documents.mapNotNull { it.toObject(Notification::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.created_at?.seconds ?: 0 }
    }

    suspend fun unreadCount(): Int {
        val snap = notifs.whereEqualTo("user_id", uid()).whereEqualTo("read", false).get().await()
        return snap.size()
    }

    suspend fun markRead(id: String) {
        notifs.document(id).update("read", true).await()
    }

    suspend fun markAllRead() {
        val snap = notifs.whereEqualTo("user_id", uid()).whereEqualTo("read", false).get().await()
        if (snap.isEmpty) return
        val batch = db.batch()
        snap.documents.forEach { batch.update(it.reference, "read", true) }
        batch.commit().await()
    }
}
