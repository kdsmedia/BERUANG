package com.altomedia.beruang.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Friendship
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendState
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val state: FriendState = FriendState(emptySet(), emptyList(), emptySet()),
    val acceptedProfiles: List<Profile> = emptyList(),
    val suggestedProfiles: List<Profile> = emptyList(),
    val pendingInProfiles: List<Profile> = emptyList(),
    val loading: Boolean = true,
    val toast: String? = null
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friends: FriendsRepository,
    private val profiles: ProfileRepository,
    private val feed: FeedRepository,
    private val realtime: Realtime
) : ViewModel() {
    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state

    init {
        refresh()
        // Real-time: live friend requests/accepts so the Friends tab updates immediately.
        viewModelScope.launch {
            runCatching { friends.friendshipChanges(realtime).collect { refresh() } }
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        try {
            val fs = runCatching { friends.state() }.getOrDefault(
                com.altomedia.beruang.data.repo.FriendState(emptySet(), emptyList(), emptySet())
            )
            val acceptedP = fs.accepted.mapNotNull { runCatching { profiles.get(it) }.getOrNull() }
            val pendingInP = fs.pendingIn.mapNotNull { runCatching { profiles.get(it.user_id) }.getOrNull() }
            val all = runCatching { profiles.list(60) }.getOrDefault(emptyList())
            val excluded = fs.accepted + fs.pendingOut + fs.pendingIncomingIds()
            val suggested = all.filter { it.id !in excluded && it.id != profiles.currentUid }
            _state.value = FriendsUiState(fs, acceptedP, suggested, pendingInP, loading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, toast = "Gagal memuat teman")
        }
    }

    fun send(uid: String) = viewModelScope.launch {
        runCatching { friends.sendRequest(uid, feed) }
            .onSuccess { _state.value = _state.value.copy(toast = "Request sent") }
            .onFailure { _state.value = _state.value.copy(toast = "Gagal mengirim permintaan") }
        refresh()
    }
    fun accept(f: Friendship) = viewModelScope.launch { runCatching { friends.accept(f, feed) }; refresh() }
    fun decline(f: Friendship) = viewModelScope.launch { runCatching { friends.decline(f) }; refresh() }
    fun remove(uid: String) = viewModelScope.launch {
        runCatching { friends.remove(uid) }
            .onSuccess { _state.value = _state.value.copy(toast = "Removed") }
        refresh()
    }
    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
