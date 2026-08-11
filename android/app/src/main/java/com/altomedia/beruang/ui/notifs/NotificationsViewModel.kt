package com.altomedia.beruang.ui.notifs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Notification
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.NotificationsRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotifsUiState(
    val items: List<Notification> = emptyList(),
    val fromProfiles: Map<String, Profile> = emptyMap(),
    val loading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository,
    private val profiles: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotifsUiState())
    val state: StateFlow<NotifsUiState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        val list = repo.list()
        val ids = list.mapNotNull { it.from_user_id }.distinct()
        val map = ids.map { it to profiles.get(it) }.toMap()
        _state.value = NotifsUiState(list, map, loading = false)
    }

    fun markRead(id: String) = viewModelScope.launch { repo.markRead(id); refresh() }
    fun markAll() = viewModelScope.launch { repo.markAllRead(); refresh() }
}
