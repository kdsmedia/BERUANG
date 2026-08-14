package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Notification
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
class NotificationsRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val notifs get() = postgrest.from("notifications")
    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    suspend fun list(): List<Notification> =
        runCatching {
            notifs.select { filter { eq("user_id", uid()) }; order("created_at", Order.DESCENDING) }.decodeList<Notification>()
        }.getOrDefault(emptyList())

    suspend fun unreadCount(): Int =
        runCatching {
            notifs.select { filter { eq("user_id", uid()); eq("read", false) } }.decodeList<Notification>().size
        }.getOrDefault(0)

    /** Live stream of my notifications (for live bell badge + list updates). */
    suspend fun myNotifChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("my_notifications")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "notifications" }
    }

    suspend fun markRead(id: String) {
        runCatching { notifs.update({ set("read", true) }) { filter { eq("id", id) } } }
    }

    suspend fun markAllRead() {
        val unread = runCatching {
            notifs.select { filter { eq("user_id", uid()); eq("read", false) } }.decodeList<Notification>()
        }.getOrDefault(emptyList())
        unread.forEach { n ->
            runCatching { notifs.update({ set("read", true) }) { filter { eq("id", n.id) } } }
        }
    }
}
