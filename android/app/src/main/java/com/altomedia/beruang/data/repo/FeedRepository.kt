package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.isoNow
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Like
import com.altomedia.beruang.data.model.Notification
import com.altomedia.beruang.data.model.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val client: SupabaseClient,
    private val accounts: AccountsRepository
) {
    private val auth: Auth get() = client.auth
    private val postgrest: Postgrest get() = client.postgrest
    private val posts get() = postgrest.from("posts")
    private val likes get() = postgrest.from("likes")
    private val comments get() = postgrest.from("comments")
    private val notifs get() = postgrest.from("notifications")

    private fun uid() = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")

    // ---------- posts ----------
    suspend fun feed(limit: Long = 50): List<Post> =
        runCatching {
            posts.select { order("created_at", Order.DESCENDING); limit(limit) }.decodeList<Post>()
        }.getOrDefault(emptyList())

    suspend fun postsByUser(userId: String): List<Post> =
        runCatching {
            posts.select { filter { eq("user_id", userId) }; order("created_at", Order.DESCENDING) }.decodeList<Post>()
        }.getOrDefault(emptyList())

    /** Live stream of new/changed posts so the feed updates in real time. */
    suspend fun feedChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("feed_posts")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "posts" }
    }

    /** Text-only post (photo/video/location features removed). */
    suspend fun createPost(content: String?): String {
        val u = uid()
        val post = Post(
            user_id = u,
            content = content?.ifBlank { null },
            created_at = isoNow()
        )
        val created = posts.insert(post) { select() }.decodeSingle<Post>()
        runCatching { accounts.awardPoints(u, 20) } // +20 poin per posting
        return created.id
    }

    suspend fun deletePost(postId: String, ownerId: String) {
        if (ownerId != uid()) throw SecurityException("Not post owner")
        posts.delete { filter { eq("id", postId); eq("user_id", ownerId) } }
    }

    // ---------- likes ----------
    suspend fun likesForPosts(postIds: List<String>): Map<String, List<Like>> {
        if (postIds.isEmpty()) return emptyMap()
        val all = runCatching {
            likes.select { filter { isIn("post_id", postIds) } }.decodeList<Like>()
        }.getOrDefault(emptyList())
        return all.groupBy { it.post_id }
    }

    suspend fun isLiked(postId: String, uid: String): Boolean =
        runCatching {
            likes.select { filter { eq("post_id", postId); eq("user_id", uid) } }.decodeList<Like>()
        }.getOrDefault(emptyList()).isNotEmpty()

    suspend fun toggleLike(post: Post): Boolean {
        val u = uid()
        val existing = runCatching {
            likes.select { filter { eq("post_id", post.id); eq("user_id", u) } }.decodeList<Like>()
        }.getOrDefault(emptyList())
        return if (existing.isNotEmpty()) {
            likes.delete { filter { eq("id", existing.first().id) } }
            false
        } else {
            likes.insert(Like(post_id = post.id, user_id = u))
            if (post.user_id != u) createNotif(post.user_id, "like", post.id, "liked your post")
            true
        }
    }

    /** Live stream of like changes (for live like counters). */
    suspend fun likeChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("feed_likes")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "likes" }
    }

    // ---------- comments ----------
    suspend fun commentsForPost(postId: String): List<Comment> =
        runCatching {
            comments.select { filter { eq("post_id", postId) }; order("created_at", Order.ASCENDING) }.decodeList<Comment>()
        }.getOrDefault(emptyList())

    /** Live stream of comments on a post. */
    suspend fun commentChanges(realtime: Realtime): Flow<PostgresAction> {
        val channel = realtime.channel("feed_comments")
        channel.subscribe()
        return channel.postgresChangeFlow<PostgresAction>("public") { table = "comments" }
    }

    suspend fun addComment(postId: String, postOwner: String, text: String): Comment {
        val u = uid()
        val c = Comment(post_id = postId, user_id = u, content = text, created_at = isoNow())
        val created = comments.insert(c) { select() }.decodeSingle<Comment>()
        runCatching { accounts.awardPoints(u, 50) } // +50 poin per komentar
        if (postOwner != u) createNotif(postOwner, "comment", postId, "commented on your post")
        return created
    }

    suspend fun deleteComment(commentId: String) {
        comments.delete { filter { eq("id", commentId) } }
    }

    // ---------- notifications ----------
    suspend fun createNotif(toUserId: String, type: String, referenceId: String?, content: String) {
        if (toUserId == uid()) return
        val n = Notification(
            user_id = toUserId,
            type = type,
            from_user_id = uid(),
            reference_id = referenceId,
            content = content,
            read = false
        )
        runCatching { notifs.insert(n) }
    }
}
