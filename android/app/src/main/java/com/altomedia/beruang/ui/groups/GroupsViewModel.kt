package com.altomedia.beruang.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.Group
import com.altomedia.beruang.data.model.GroupMember
import com.altomedia.beruang.data.repo.GroupsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val myGroupIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val toast: String? = null
)

@HiltViewModel
class GroupsViewModel @Inject constructor(private val repo: GroupsRepository) : ViewModel() {
    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        try {
            val groups = runCatching { repo.allGroups() }.getOrDefault(emptyList())
            val mine = runCatching { repo.myMemberships() }.getOrDefault(emptyList()).map { it.group_id }.toSet()
            _state.value = GroupsUiState(groups, mine, loading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, toast = "Gagal memuat grup")
        }
    }

    fun create(name: String, desc: String?) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        runCatching { repo.create(name, desc) }
            .onSuccess { _state.value = _state.value.copy(toast = "Group created") }
            .onFailure { _state.value = _state.value.copy(toast = "Gagal membuat grup") }
        refresh()
    }

    fun join(id: String) = viewModelScope.launch { runCatching { repo.join(id) }; refresh() }
    fun leave(id: String) = viewModelScope.launch { runCatching { repo.leave(id) }; refresh() }
    fun toastShown() { _state.value = _state.value.copy(toast = null) }
}
