package com.altomedia.beruang.data.repo

import android.net.Uri
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Like
import com.altomedia.beruang.data.model.Notification
import com.altomedia.beruang.data.model.Post
import com.altomedia.beruang.data.model.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {
    private val posts = db.collection("posts")
    private val likes = db.collection("likes")
    private val comments = db.collection("comments")
    private val stories = db.collection("stories")
    private val notifs = db.collection("notifications")

    private fun uid() = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")

    // ---------- posts ----------
    suspend fun feed(limit: Long = 50): List<Post> {
        val snap = posts.orderBy("created_at", Query.Direction.DESCENDING).limit(limit).get().await()
        return snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
    }

    suspend fun postsByUser(userId: String): List<Post> {
        val snap = posts.whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).get().await()
        return snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
    }

    suspend fun createPost(content: String?, image: Uri?, video: Uri?, location: String?): String {
        val uid = uid()
        var imageUrl: String? = null
        var videoUrl: String? = null
        if (image != null) imageUrl = uploadMedia(image, "image")
        if (video != null) videoUrl = uploadMedia(video, "video")
        val post = Post(
            user_id = uid,
            content = content?.ifBlank { null },
            image_url = imageUrl,
            video_url = videoUrl,
            location = location?.ifBlank { null }
        )
        val ref = posts.add(post).await()
        return ref.id
    }

    private suspend fun uploadMedia(uri: Uri, kind: String): String {
        val uid = uid()
        val ext = if (kind == "image") "img" else "mp4"
        val path = "posts/$uid/post-${UUID.randomUUID()}.$ext"
        val ref = storage.getReference(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
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

    // ---------- stories ----------
    suspend fun recentStories(): List<Story> {
        val cutoff = System.currentTimeMillis() / 1000 - 24 * 3600
        val snap = stories.orderBy("created_at", Query.Direction.DESCENDING).get().await()
        return snap.documents.mapNotNull { it.toObject(Story::class.java)?.copy(id = it.id) }
            .filter { (it.created_at?.seconds ?: 0) >= cutoff }
            .distinctBy { it.user_id }
    }

    suspend fun addStory(uri: Uri) {
        val uid = uid()
        val path = "posts/$uid/story-${UUID.randomUUID()}.img"
        val ref = storage.getReference(path)
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        stories.add(Story(user_id = uid, image_url = url)).await()
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
