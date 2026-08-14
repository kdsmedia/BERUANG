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
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repo: MessagesRepository,
    private val profiles: ProfileRepository,
    private val feed: FeedRepository,
    private val realtime: Realtime
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

    private var activePartner: String? = null

    init {
        refresh()
        // Real-time global chat: append incoming global messages live.
        viewModelScope.launch {
            runCatching {
                repo.globalChanges(realtime).collect { loadGlobal() }
            }
        }
        // Real-time 1:1 threads: refresh the open thread + conversation list live.
        viewModelScope.launch {
            runCatching {
                repo.threadChanges(realtime).collect {
                    refresh()
                    activePartner?.let { p ->
                        _thread.value = repo.threadWith(p)
                    }
                }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        _loading.value = true
        try {
            _convos.value = runCatching { repo.conversationList() }.getOrDefault(emptyList())
            val ids = _convos.value.map { it.partnerId }
            _profiles.value = ids.mapNotNull { runCatching { it to profiles.get(it) }.getOrNull() }.toMap()
        } finally { _loading.value = false }
    }

    fun loadGlobal() = viewModelScope.launch {
        try {
            _global.value = repo.globalMessages()
            val ids = _global.value.map { it.user_id }.distinct()
            _globalProfiles.value = ids.mapNotNull { runCatching { it to profiles.get(it) }.getOrNull() }.toMap()
        } catch (e: Exception) { /* keep empty, don't crash */ }
    }

    fun sendGlobal(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        try { repo.sendGlobal(text); loadGlobal() } catch (e: Exception) { /* ignore */ }
    }

    // ---- 1:1 thread ----
    private val _thread = MutableStateFlow<List<Message>>(emptyList())
    val thread: StateFlow<List<Message>> = _thread
    private val _partner = MutableStateFlow<Profile?>(null)
    val partner: StateFlow<Profile?> = _partner

    fun openThread(partnerUid: String) = viewModelScope.launch {
        activePartner = partnerUid
        try {
            _partner.value = profiles.get(partnerUid)
            _thread.value = repo.threadWith(partnerUid)
        } catch (e: Exception) { /* keep empty, don't crash */ }
    }

    fun send(partnerUid: String, text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        try {
            repo.send(partnerUid, text, feed)
            _thread.value = repo.threadWith(partnerUid)
        } catch (e: Exception) { /* ignore */ }
    }
}
