package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.GlobalMessage
import com.altomedia.beruang.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagesRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val messages = db.collection("messages")
    private val global = db.collection("global_messages")
    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    // ---- 1:1 ----
    suspend fun conversationList(): List<ConversationSummary> {
        val sent = messages.whereEqualTo("sender_id", uid())
            .orderBy("created_at", Query.Direction.DESCENDING).limit(200).get().await()
        val recv = messages.whereEqualTo("receiver_id", uid())
            .orderBy("created_at", Query.Direction.DESCENDING).limit(200).get().await()
        val all = (sent.documents + recv.documents)
            .mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.created_at?.seconds ?: 0 }
        val byPartner = LinkedHashMap<String, Message>()
        val unread = HashMap<String, Int>()
        for (m in all) {
            val partner = if (m.sender_id == uid()) m.receiver_id else m.sender_id
            byPartner.getOrPut(partner) { m }
            if (m.receiver_id == uid() && !m.read) unread[partner] = (unread[partner] ?: 0) + 1
        }
        return byPartner.map { (pid, m) ->
            ConversationSummary(pid, m, unread[pid] ?: 0)
        }
    }

    suspend fun threadWith(partner: String): List<Message> {
        val sent = messages.whereEqualTo("sender_id", uid()).whereEqualTo("receiver_id", partner)
            .orderBy("created_at", Query.Direction.ASCENDING).limit(200).get().await()
        val recv = messages.whereEqualTo("sender_id", partner).whereEqualTo("receiver_id", uid())
            .orderBy("created_at", Query.Direction.ASCENDING).limit(200).get().await()
        val all = (sent.documents + recv.documents)
            .mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) }
            .sortedBy { it.created_at?.seconds ?: 0 }
        // mark received as read
        val unread = all.filter { it.receiver_id == uid() && !it.read }
        if (unread.isNotEmpty()) {
            val batch = db.batch()
            unread.forEach { batch.update(messages.document(it.id), "read", true) }
            batch.commit().await()
        }
        return all
    }

    suspend fun send(to: String, text: String, feedRepo: FeedRepository) {
        val m = Message(sender_id = uid(), receiver_id = to, content = text)
        messages.add(m).await()
        feedRepo.createNotif(to, "message", null, "sent you a message")
    }

    // ---- global ----
    suspend fun globalMessages(): List<GlobalMessage> {
        val snap = global.orderBy("created_at", Query.Direction.ASCENDING).limit(200).get().await()
        return snap.documents.mapNotNull { it.toObject(GlobalMessage::class.java)?.copy(id = it.id) }
    }

    suspend fun sendGlobal(text: String) {
        global.add(GlobalMessage(user_id = uid(), content = text)).await()
    }
}

data class ConversationSummary(
    val partnerId: String,
    val lastMessage: Message,
    val unread: Int
)
