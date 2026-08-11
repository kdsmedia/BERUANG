package com.altomedia.beruang.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.EmojiPickerSheet
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun MessagesScreen(openChat: (String) -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf("chats") }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (tab == "chats") 0 else 1, containerColor = Surface, contentColor = GreenBright) {
            Tab(selected = tab == "chats", onClick = { tab = "chats" }) { Text("Chats", modifier = Modifier.padding(10.dp)) }
            Tab(selected = tab == "global", onClick = { tab = "global"; vm.loadGlobal() }) { Text("Global Chat", modifier = Modifier.padding(10.dp)) }
        }
        if (tab == "chats") ChatsTab(openChat, vm)
        else GlobalTab(vm)
    }
}

@Composable
private fun ChatsTab(openChat: (String) -> Unit, vm: MessagesViewModel) {
    val convos by vm.convos.collectAsStateWithLifecycle()
    val profilesMap by vm.profilesMap.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    if (loading) {
        EmptyState("⏳", "Loading chats…")
        return
    }
    if (convos.isEmpty()) {
        EmptyState("💬", "No conversations yet. Start one from a friend's profile.")
        return
    }
    LazyColumn {
        items(convos) { c ->
            val p = profilesMap[c.partnerId]
            Surface(Modifier.fillMaxWidth().padding(8.dp, 4.dp), shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Row(Modifier.padding(12.dp).clickableNoArg { openChat(c.partnerId) }, verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold)
                        Text(c.lastMessage.content.take(60), color = Muted, fontSize = 12.sp)
                    }
                    if (c.unread > 0) {
                        Surface(color = Gold, shape = RoundedCornerShape(50)) {
                            Text("${c.unread}", color = Bg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp, 2.dp))
                        }
                    }
                    Text(relTime(c.lastMessage.created_at), color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun GlobalTab(vm: MessagesViewModel) {
    LaunchedEffect(Unit) { vm.loadGlobal() }
    val msgs by vm.global.collectAsStateWithLifecycle()
    val profs by vm.globalProfiles.collectAsStateWithLifecycle()
    val myUid = androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.profile.SessionViewModel>().uid.collectAsStateWithLifecycle().value
    var text by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            if (msgs.isEmpty()) item { EmptyState("🌐", "Be the first to say hi in global chat.") }
            items(msgs) { m ->
                val mine = m.user_id == myUid
                val p = profs[m.user_id]
                Row(Modifier.fillMaxWidth().padding(8.dp, 4.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (mine) Green else Surface2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(10.dp, 6.dp)) {
                            Text(p?.displayName ?: "User", color = if (mine) Bg else Muted, fontSize = 11.sp)
                            Text(m.content, color = if (mine) Bg else Text, fontSize = 14.sp)
                            Text(relTime(m.created_at), color = if (mine) Bg else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Say something to everyone…") },
                modifier = Modifier.weight(1f), singleLine = true,
                colors = outlinedFieldColors(), shape = RoundedCornerShape(50)
            )
            IconButton(onClick = { showEmoji = true }) { Text("😀", fontSize = 22.sp) }
            IconButton(onClick = { if (text.isNotBlank()) { vm.sendGlobal(text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = Green)
            }
        }
    }
    if (showEmoji) EmojiPickerSheet(onInsert = { text += it }, onDismiss = { showEmoji = false })
}

@Composable
private fun Modifier.clickableNoArg(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
