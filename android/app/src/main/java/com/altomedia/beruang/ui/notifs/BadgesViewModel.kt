package com.altomedia.beruang.ui.notifs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.altomedia.beruang.data.repo.FriendsRepository
import com.altomedia.beruang.data.repo.MessagesRepository
import com.altomedia.beruang.data.repo.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val notifsRepo: NotificationsRepository,
    private val messagesRepo: MessagesRepository,
    private val friendsRepo: FriendsRepository
) : ViewModel() {
    private val _notifUnread = MutableStateFlow(0)
    val notifUnread: StateFlow<Int> = _notifUnread

    private val _msgUnread = MutableStateFlow(0)
    val msgUnread: StateFlow<Int> = _msgUnread

    private val _frPending = MutableStateFlow(0)
    val frPending: StateFlow<Int> = _frPending

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        // All three calls hit Firestore; any one can throw on network/permission
        // errors. Guard each independently so a failure never crashes the app
        // (this ViewModel is mounted on every RootNav composition).
        _notifUnread.value = runCatching { notifsRepo.unreadCount() }.getOrDefault(0)
        _msgUnread.value = runCatching { messagesRepo.conversationList().sumOf { it.unread } }.getOrDefault(0)
        _frPending.value = runCatching { friendsRepo.state().pendingIn.size }.getOrDefault(0)
    }
}
