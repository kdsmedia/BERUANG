package com.altomedia.beruang.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.model.GlobalMessage
import com.altomedia.beruang.data.model.Message
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.repo.ConversationSummary
import com.altomedia.beruang.data.repo.FeedRepository
import com.altomedia.beruang.data.repo.MessagesRepository
import com.altomedia.beruang.data.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repo: MessagesRepository,
    private val profiles: ProfileRepository,
    private val feed: FeedRepository
) : ViewModel() {
    private val _convos = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val convos: StateFlow<List<ConversationSummary>> = _convos
    private val _profiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val profilesMap: StateFlow<Map<String, Profile>> = _profiles
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _global = MutableStateFlow<List<GlobalMessage>>(emptyList())
    val global: StateFlow<List<GlobalMessage>> = _global
    private val _globalProfiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val globalProfiles: StateFlow<Map<String, Profile>> = _globalProfiles

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        _convos.value = repo.conversationList()
        val ids = _convos.value.map { it.partnerId }
        val map = ids.map { it to profiles.get(it) }.toMap()
        _profiles.value = map
        _loading.value = false
    }

    fun loadGlobal() = viewModelScope.launch {
        _global.value = repo.globalMessages()
        val ids = _global.value.map { it.user_id }.distinct()
        _globalProfiles.value = ids.map { it to profiles.get(it) }.toMap()
    }

    fun sendGlobal(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        repo.sendGlobal(text); loadGlobal()
    }

    // ---- 1:1 thread ----
    private val _thread = MutableStateFlow<List<Message>>(emptyList())
    val thread: StateFlow<List<Message>> = _thread
    private val _partner = MutableStateFlow<Profile?>(null)
    val partner: StateFlow<Profile?> = _partner

    fun openThread(partnerUid: String) = viewModelScope.launch {
        _partner.value = profiles.get(partnerUid)
        _thread.value = repo.threadWith(partnerUid)
    }

    fun send(partnerUid: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        repo.send(partnerUid, text, feed)
        _thread.value = repo.threadWith(partnerUid)
    }
}
