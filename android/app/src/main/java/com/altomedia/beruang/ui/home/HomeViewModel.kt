package com.altomedia.beruang.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Post
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val profiles: ProfileRepository
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

    private val _expandedComments = MutableStateFlow<Set<String>>(emptySet())
    val expandedComments: StateFlow<Set<String>> = _expandedComments.asStateFlow()

    fun toastShown() { _toast.value = null }

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        try { loadFeed() } finally { _loading.value = false }
    }

    private suspend fun loadFeed() {
        val posts = feed.feed()
        val ids = posts.map { it.user_id }.distinct()
        val byId = ids.map { it to profiles.get(it) }.toMap()
        val likeMap = feed.likesForPosts(posts.map { it.id })
        val uid = profiles.currentUid
        _items.value = posts.map { p ->
            val likes = likeMap[p.id] ?: emptyList()
            FeedItem(p, byId[p.user_id]!!, likes.size, likes.any { it.user_id == uid })
        }
    }

    fun createPost(content: String) = viewModelScope.launch {
        try {
            feed.createPost(content)
            _toast.value = "Posted"
            refresh()
        } catch (e: Exception) { _toast.value = "Post failed: ${e.message}" }
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
