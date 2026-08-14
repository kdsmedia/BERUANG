package com.altomedia.beruang.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Post
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedItem(
    val post: Post,
    val author: Profile,
    val likeCount: Int,
    val isLiked: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feed: FeedRepository,
    private val profiles: ProfileRepository,
    private val realtime: Realtime
) : ViewModel() {

    private val _items = MutableStateFlow<List<FeedItem>>(emptyList())
    val items: StateFlow<List<FeedItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // comments cache per post
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    // profiles of commenters, keyed by user id, for showing real names
    private val _commentProfiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val commentProfiles: StateFlow<Map<String, Profile>> = _commentProfiles.asStateFlow()

    private val _expandedComments = MutableStateFlow<Set<String>>(emptySet())
    val expandedComments: StateFlow<Set<String>> = _expandedComments.asStateFlow()

    fun toastShown() { _toast.value = null }

    init {
        refresh()
        // Real-time: reload the feed whenever a post, like, or comment changes
        // so the home feed updates live (new posts, like counts, comments).
        viewModelScope.launch {
            runCatching {
                kotlinx.coroutines.flow.merge(
                    feed.feedChanges(realtime),
                    feed.likeChanges(realtime),
                    feed.commentChanges(realtime),
                ).collect { refresh() }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        try { loadFeed() } finally { _loading.value = false }
    }

    private suspend fun loadFeed() {
        val posts = try { feed.feed() } catch (e: Exception) { return }
        val fetchedIds = posts.map { it.id }.toHashSet()
        val ids = posts.map { it.user_id }.distinct()
        val byId = ids.mapNotNull { uid -> runCatching { uid to profiles.get(uid) }.getOrNull() }.toMap()
        val likeMap = runCatching { feed.likesForPosts(posts.map { it.id }) }.getOrDefault(emptyMap())
        val uid = profiles.currentUid
        val fetched = posts.mapNotNull { p ->
            val author = byId[p.user_id] ?: Profile(id = p.user_id, full_name = "User", avatar_url = Profile.dicebearAvatar(p.user_id))
            val likes = likeMap[p.id] ?: emptyList()
            FeedItem(p, author, likes.size, likes.any { it.user_id == uid })
        }
        // Keep optimistic/local posts that the server query hasn't returned yet
        // (e.g. a just-created post whose server timestamp isn't indexed) so
        // they don't vanish on refresh.
        val localOnly = _items.value.filter { it.post.id !in fetchedIds }
        _items.value = localOnly + fetched
    }

    fun createPost(content: String) = viewModelScope.launch {
        val text = content.trim()
        if (text.isEmpty()) { _toast.value = "Tulis sesuatu dulu"; return@launch }
        val uid = profiles.currentUid
        // Optimistically prepend FIRST so the post shows instantly even if the
        // subsequent server write/query lags or fails.
        val me = profiles.myProfile() ?: uid?.let { Profile(id = it, full_name = "Me", avatar_url = Profile.dicebearAvatar(it)) }
        if (me != null) {
            val optimisticPost = Post(
                id = "local-${System.currentTimeMillis()}",
                user_id = me.id,
                content = text,
                created_at = com.altomedia.beruang.data.isoNow()
            )
            _items.value = listOf(FeedItem(optimisticPost, me, 0, false)) + _items.value
        }
        try {
            val newId = feed.createPost(text)
            // Replace the optimistic placeholder with the real server id so it
            // persists across refreshes (merge keeps posts not returned by server).
            _items.value = _items.value.map {
                if (it.post.id.startsWith("local-")) it.copy(post = it.post.copy(id = newId)) else it
            }
            _toast.value = "Posted"
            refresh()
        } catch (e: Exception) {
            _toast.value = "Post failed: ${e.message}"
            // Remove the optimistic post if the write actually failed.
            _items.value = _items.value.filterNot { it.post.id.startsWith("local-") }
        }
    }

    fun toggleLike(item: FeedItem) = viewModelScope.launch {
        try {
            val liked = feed.toggleLike(item.post)
            _items.value = _items.value.map {
                if (it.post.id == item.post.id) it.copy(
                    isLiked = liked,
                    likeCount = it.likeCount + (if (liked) 1 else -1)
                ) else it
            }
        } catch (e: Exception) { _toast.value = "Like failed" }
    }

    fun toggleComments(postId: String) {
        val set = _expandedComments.value.toMutableSet()
        if (postId in set) set.remove(postId) else set.add(postId)
        _expandedComments.value = set
        if (postId in set && _comments.value[postId] == null) loadComments(postId)
    }

    private fun loadComments(postId: String) = viewModelScope.launch {
        try {
            val c = feed.commentsForPost(postId)
            _comments.value = _comments.value + (postId to c)
            // fetch commenter profiles for real names
            val ids = c.map { it.user_id }.distinct().filter { it !in _commentProfiles.value }
            if (ids.isNotEmpty()) {
                val fetched = ids.map { it to profiles.get(it) }
                _commentProfiles.value = _commentProfiles.value + fetched
            }
        } catch (e: Exception) { _toast.value = "Comments failed" }
    }

    fun addComment(post: Post, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        try {
            feed.addComment(post.id, post.user_id, text)
            loadComments(post.id)
        } catch (e: Exception) { _toast.value = "Comment failed" }
    }

    fun deleteComment(commentId: String, postId: String) = viewModelScope.launch {
        try {
            feed.deleteComment(commentId)
            loadComments(postId)
        } catch (e: Exception) { _toast.value = "Delete failed" }
    }

    fun deletePost(post: Post) = viewModelScope.launch {
        try {
            feed.deletePost(post.id, post.user_id)
            _items.value = _items.value.filterNot { it.post.id == post.id }
            _toast.value = "Post deleted"
        } catch (e: Exception) { _toast.value = e.message ?: "Delete failed" }
    }
}
