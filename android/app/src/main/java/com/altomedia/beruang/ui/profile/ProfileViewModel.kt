package com.altomedia.beruang.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Post
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.AccountsRepository
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import com.altomedia.beruang.data.repo.TransferResult
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
    val hasPin: Boolean = false,
    val loading: Boolean = true,
    val toast: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val feed: FeedRepository,
    private val friends: FriendsRepository,
    private val accounts: AccountsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state
    private var loadedUid: String? = null

    fun load(uid: String) = viewModelScope.launch {
        if (loadedUid == uid && _state.value.profile != null) return@launch
        loadedUid = uid
        _state.value = _state.value.copy(loading = true, toast = null)
        try {
            var p = runCatching { profiles.get(uid) }.getOrNull()
                ?: Profile(id = uid, full_name = "User", avatar_url = Profile.dicebearAvatar(uid))
            // Ensure the signed-in user always has a 6-digit account_id, and
            // use the wallet as the source of truth for the points balance.
            if (uid == profiles.currentUid) {
                p = runCatching { accounts.ensureMyAccountId() }.getOrDefault(p)
            }
            val balance = runCatching { accounts.getBalance(uid) }.getOrDefault(p.points)
            val displayed = p.copy(points = balance)
            val posts = runCatching { feed.postsByUser(uid) }.getOrDefault(emptyList())
            val fs = runCatching { friends.state() }.getOrDefault(
                com.altomedia.beruang.data.repo.FriendState(emptySet(), emptyList(), emptySet())
            )
            val friendIds = fs.accepted.filter { it != profiles.currentUid }
            val friendProfiles = friendIds.mapNotNull { runCatching { profiles.get(it) }.getOrNull() }
            val pin = if (uid == profiles.currentUid) runCatching { accounts.hasPin() }.getOrDefault(false) else false
            _state.value = ProfileUiState(displayed, posts, mapOf(uid to displayed), friendIds, friendProfiles, hasPin = pin, loading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, toast = "Gagal memuat profil: ${e.message}")
        }
    }

    fun updateProfile(
        name: String, bio: String, avatarPreset: String?,
        phone: String, email: String, gender: String?
    ) = viewModelScope.launch {
        try {
            profiles.update(name, bio, avatarPreset, phone, email, gender)
            _state.value = _state.value.copy(toast = "Profil diperbarui")
            // Force a reload so the new fields show immediately.
            loadedUid = null
            profiles.currentUid?.let { load(it) }
        } catch (e: Exception) { _state.value = _state.value.copy(toast = "Update failed: ${e.message}") }
    }

    fun setPin(pin: String) = viewModelScope.launch {
        try {
            accounts.setPin(pin)
            _state.value = _state.value.copy(toast = "PIN transaksi disimpan")
            loadedUid = null
            profiles.currentUid?.let { load(it) }
        } catch (e: Exception) { _state.value = _state.value.copy(toast = "Gagal menyimpan PIN: ${e.message}") }
    }

    fun transfer(toAccountId: String, amount: Long, pin: String) = viewModelScope.launch {
        try {
            when (val r = accounts.transfer(toAccountId, amount, pin)) {
                is TransferResult.Success -> {
                    _state.value = _state.value.copy(toast = "Berhasil kirim ${r.amount} poin ke ${r.recipientName}")
                    loadedUid = null
                    profiles.currentUid?.let { load(it) }
                }
                is TransferResult.Error -> _state.value = _state.value.copy(toast = r.message)
            }
        } catch (e: Exception) { _state.value = _state.value.copy(toast = "Transfer gagal: ${e.message}") }
    }

    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
