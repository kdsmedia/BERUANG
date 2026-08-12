package com.altomedia.beruang.data.repo

import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Like
import com.altomedia.beruang.data.model.Notification
import com.altomedia.beruang.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val posts = db.collection("posts")
    private val likes = db.collection("likes")
    private val comments = db.collection("comments")
    private val notifs = db.collection("notifications")

    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    // ---------- posts ----------
    suspend fun feed(limit: Long = 50): List<Post> {
        // Prefer orderBy("created_at") for an already-sorted feed; if the
        // single-field index is unavailable this throws, so fall back to a
        // plain get() sorted in memory so the feed never appears empty.
        val snap = try {
            posts.orderBy("created_at", Query.Direction.DESCENDING).limit(limit).get().await()
        } catch (e: Exception) {
            posts.limit(limit).get().await()
        }
        return snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.created_at?.seconds ?: 0 }
    }

    suspend fun postsByUser(userId: String): List<Post> {
        // NOTE: querying whereEqualTo + orderBy on a different field requires a
        // composite index in Firestore, which is often missing and throws
        // FAILED_PRECONDITION (crashing the profile screen). To avoid that,
        // we fetch by user_id only and sort in memory.
        val snap = posts.whereEqualTo("user_id", userId).get().await()
        return snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.created_at?.seconds ?: 0 }
    }

    /** Text-only post (photo/video/location features removed). */
    suspend fun createPost(content: String?): String {
        val uid = uid()
        // Set created_at on the client so the doc is immediately present in the
        // cache and indexed for the orderBy("created_at") feed query — relying
        // on @ServerTimestamp leaves it null locally until the server round-trip,
        // which can cause freshly posted posts to be missing from the feed.
        val post = Post(
            user_id = uid,
            content = content?.ifBlank { null },
            created_at = com.google.firebase.Timestamp.now()
        )
        val ref = posts.add(post).await()
        return ref.id
    }

    suspend fun deletePost(postId: String, ownerId: String) {
        if (ownerId != uid()) throw SecurityException("Not post owner")
        posts.document(postId).delete().await()
    }

    // ---------- likes ----------
    suspend fun likesForPosts(postIds: List<String>): Map<String, List<Like>> {
        if (postIds.isEmpty()) return emptyMap()
        val snap = likes.whereIn("post_id", postIds).get().await()
        val all = snap.documents.mapNotNull { it.toObject(Like::class.java)?.copy(id = it.id) }
        return all.groupBy { it.post_id }
    }

    suspend fun isLiked(postId: String, uid: String): Boolean {
        val snap = likes.whereEqualTo("post_id", postId).whereEqualTo("user_id", uid).limit(1).get().await()
        return !snap.isEmpty
    }

    suspend fun toggleLike(post: Post): Boolean {
        val uid = uid()
        if (isLiked(post.id, uid)) {
            likes.whereEqualTo("post_id", post.id).whereEqualTo("user_id", uid)
                .limit(1).get().await().documents.firstOrNull()?.reference?.delete()?.await()
            return false
        } else {
            likes.add(Like(post_id = post.id, user_id = uid)).await()
            if (post.user_id != uid) createNotif(post.user_id, "like", post.id, "liked your post")
            return true
        }
    }

    // ---------- comments ----------
    suspend fun commentsForPost(postId: String): List<Comment> {
        val snap = comments.whereEqualTo("post_id", postId)
            .orderBy("created_at", Query.Direction.ASCENDING).get().await()
        return snap.documents.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) }
    }

    suspend fun addComment(postId: String, postOwner: String, text: String): Comment {
        val uid = uid()
        val c = Comment(post_id = postId, user_id = uid, content = text)
        val ref = comments.add(c).await()
        if (postOwner != uid) createNotif(postOwner, "comment", postId, "commented on your post")
        return c.copy(id = ref.id)
    }

    suspend fun deleteComment(commentId: String) {
        comments.document(commentId).delete().await()
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
        notifs.add(n).await()
    }
}
