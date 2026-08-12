package com.altomedia.beruang.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Post
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<Post> = emptyList(),
    val postAuthors: Map<String, Profile> = emptyMap(),
    val friendIds: List<String> = emptyList(),
    val friendProfiles: List<Profile> = emptyList(),
    val loading: Boolean = true,
    val toast: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val feed: FeedRepository,
    private val friends: FriendsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state
    private var loadedUid: String? = null

    fun load(uid: String) = viewModelScope.launch {
        if (loadedUid == uid && _state.value.profile != null) return@launch
        loadedUid = uid
        _state.value = _state.value.copy(loading = true, toast = null)
        try {
            val p = profiles.get(uid)
            val posts = runCatching { feed.postsByUser(uid) }.getOrDefault(emptyList())
            val fs = runCatching { friends.state() }.getOrDefault(
                com.altomedia.beruang.data.repo.FriendState(emptySet(), emptyList(), emptySet())
            )
            val friendIds = fs.accepted.filter { it != profiles.currentUid }
            val friendProfiles = friendIds.mapNotNull { runCatching { profiles.get(it) }.getOrNull() }
            _state.value = ProfileUiState(p, posts, mapOf(uid to p), friendIds, friendProfiles, loading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, toast = "Gagal memuat profil: ${e.message}")
        }
    }

    fun updateProfile(name: String, bio: String, avatarPreset: String?) = viewModelScope.launch {
        try {
            profiles.update(name, bio, avatarPreset)
            _state.value = _state.value.copy(toast = "Profil diperbarui")
            loadedUid?.let { load(it) }
        } catch (e: Exception) { _state.value = _state.value.copy(toast = "Update failed: ${e.message}") }
    }

    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
