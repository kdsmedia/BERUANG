package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.isoNow
import com.altomedia.beruang.data.model.GlobalMessage
import com.altomedia.beruang.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagesRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val messages get() = postgrest.from("messages")
    private val global get() = postgrest.from("global_messages")
    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    // ---- 1:1 ----
    suspend fun conversationList(): List<ConversationSummary> {
        val sent = runCatching {
            messages.select { filter { eq("sender_id", uid()) }; order("created_at", Order.DESCENDING) }.decodeList<Message>()
        }.getOrDefault(emptyList())
        val recv = runCatching {
            messages.select { filter { eq("receiver_id", uid()) }; order("created_at", Order.DESCENDING) }.decodeList<Message>()
        }.getOrDefault(emptyList())
        val all = (sent + recv).sortedByDescending { it.created_at ?: "" }
        val byPartner = LinkedHashMap<String, Message>()
        val unread = HashMap<String, Int>()
        for (m in all) {
            val partner = if (m.sender_id == uid()) m.receiver_id else m.sender_id
            byPartner.getOrPut(partner) { m }
            if (m.receiver_id == uid() && !m.read) unread[partner] = (unread[partner] ?: 0) + 1
        }
        return byPartner.map { (pid, m) -> ConversationSummary(pid, m, unread[pid] ?: 0) }
    }

    suspend fun threadWith(partner: String): List<Message> {
        val sent = runCatching {
            messages.select { filter { eq("sender_id", uid()); eq("receiver_id", partner) }; order("created_at", Order.ASCENDING) }.decodeList<Message>()
        }.getOrDefault(emptyList())
        val recv = runCatching {
            messages.select { filter { eq("sender_id", partner); eq("receiver_id", uid()) }; order("created_at", Order.ASCENDING) }.decodeList<Message>()
        }.getOrDefault(emptyList())
        val all = (sent + recv).sortedBy { it.created_at ?: "" }
        // mark received as read
        val unread = all.filter { it.receiver_id == uid() && !it.read }
        unread.forEach { m ->
            runCatching { messages.update({ set("read", true) }) { filter { eq("id", m.id) } } }
        }
        return all
    }

    /** Live stream of messages where I'm sender or receiver, for live chat. */
    suspend fun threadChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("thread_messages")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "messages" }
    }

    suspend fun send(to: String, text: String, feedRepo: FeedRepository) {
        val m = Message(sender_id = uid(), receiver_id = to, content = text, created_at = isoNow())
        messages.insert(m)
        feedRepo.createNotif(to, "message", null, "sent you a message")
    }

    // ---- global ----
    suspend fun globalMessages(): List<GlobalMessage> =
        runCatching {
            global.select { order("created_at", Order.ASCENDING) }.decodeList<GlobalMessage>()
        }.getOrDefault(emptyList())

    /** Live stream of global chat messages. */
    suspend fun globalChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("global_messages")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "global_messages" }
    }

    suspend fun sendGlobal(text: String) {
        global.insert(GlobalMessage(user_id = uid(), content = text, created_at = isoNow()))
    }
}

data class ConversationSummary(
    val partnerId: String,
    val lastMessage: Message,
    val unread: Int
)
