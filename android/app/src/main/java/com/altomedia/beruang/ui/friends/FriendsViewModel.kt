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
    private val feed: FeedRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        val fs = friends.state()
        val acceptedP = fs.accepted.map { profiles.get(it) }
        val pendingInP = fs.pendingIn.map { profiles.get(it.user_id) }
        val all = profiles.list(60)
        val excluded = fs.accepted + fs.pendingOut + fs.pendingIncomingIds()
        val suggested = all.filter { it.id !in excluded }
        _state.value = FriendsUiState(fs, acceptedP, suggested, pendingInP, loading = false)
    }

    fun send(uid: String) = viewModelScope.launch {
        friends.sendRequest(uid, feed); _state.value = _state.value.copy(toast = "Request sent"); refresh()
    }
    fun accept(f: Friendship) = viewModelScope.launch { friends.accept(f, feed); refresh() }
    fun decline(f: Friendship) = viewModelScope.launch { friends.decline(f); refresh() }
    fun remove(uid: String) = viewModelScope.launch { friends.remove(uid); _state.value = _state.value.copy(toast = "Removed"); refresh() }
    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
