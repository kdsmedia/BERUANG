package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Friendship
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendsRepository @Inject constructor(
    private val client: SupabaseClient,
    private val accounts: AccountsRepository
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val fs get() = postgrest.from("friendships")
    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    /** All friendship rows where I am user_id OR friend_id. */
    private suspend fun mine(): List<Friendship> {
        val a = runCatching {
            fs.select { filter { eq("user_id", uid()) } }.decodeList<Friendship>()
        }.getOrDefault(emptyList())
        val b = runCatching {
            fs.select { filter { eq("friend_id", uid()) } }.decodeList<Friendship>()
        }.getOrDefault(emptyList())
        return (a + b).distinctBy { it.id }
    }

    suspend fun state(): FriendState {
        val rows = mine()
        val accepted = mutableSetOf<String>()
        val pendingIn = mutableListOf<Friendship>()
        val pendingOut = mutableSetOf<String>()
        rows.forEach { f ->
            when {
                f.status == "accepted" -> accepted.add(if (f.user_id == uid()) f.friend_id else f.user_id)
                f.status == "pending" && f.friend_id == uid() -> pendingIn.add(f)
                f.status == "pending" && f.user_id == uid() -> pendingOut.add(f.friend_id)
            }
        }
        return FriendState(accepted, pendingIn, pendingOut)
    }

    /** Live stream of friendship changes so the Friends tab updates in real time. */
    suspend fun friendshipChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("friendships")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "friendships" }
    }

    suspend fun sendRequest(targetUid: String, repo: FeedRepository) {
        if (targetUid == uid()) return
        val existing = runCatching {
            fs.select { filter { eq("user_id", uid()); eq("friend_id", targetUid) } }.decodeList<Friendship>()
        }.getOrDefault(emptyList())
        if (existing.isEmpty()) {
            fs.insert(Friendship(user_id = uid(), friend_id = targetUid, status = "pending"))
            repo.createNotif(targetUid, "friend_request", null, "sent you a friend request")
        }
    }

    suspend fun accept(friendship: Friendship, repo: FeedRepository) {
        fs.update({ set("status", "accepted") }) { filter { eq("id", friendship.id) } }
        val other = if (friendship.user_id == uid()) friendship.friend_id else friendship.user_id
        runCatching { accounts.awardPoints(uid(), 10); accounts.awardPoints(other, 10) }
        repo.createNotif(other, "friend_accept", null, "accepted your friend request")
    }

    suspend fun decline(friendship: Friendship) {
        fs.delete { filter { eq("id", friendship.id) } }
    }

    suspend fun remove(otherUid: String) {
        val rows = mine().filter { it.user_id == otherUid || it.friend_id == otherUid }
        rows.forEach { fs.delete { filter { eq("id", it.id) } } }
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
