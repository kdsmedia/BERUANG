package com.altomedia.beruang.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun MessagesScreen(openChat: (String) -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf("chats") }
    Scaffold(
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Column(Modifier.statusBarsPadding()) {
                    Text("Messages", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    TabRow(selectedTabIndex = if (tab == "chats") 0 else 1, containerColor = Surface, contentColor = Green) {
                        Tab(selected = tab == "chats", onClick = { tab = "chats" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("Chats", modifier = Modifier.padding(10.dp)) }
                        Tab(selected = tab == "global", onClick = { tab = "global"; vm.loadGlobal() }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("Global Chat", modifier = Modifier.padding(10.dp)) }
                    }
                }
            }
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            if (tab == "chats") ChatsTab(openChat, vm)
            else GlobalTab(vm)
        }
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
        EmptyState("💬", "No conversations yet.\nStart one from a friend's profile.")
        return
    }
    LazyColumn {
        items(convos) { c ->
            val p = profilesMap[c.partnerId]
            Row(
                Modifier.fillMaxWidth().clickableNoArg { openChat(c.partnerId) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(p, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(c.lastMessage.content.take(50), color = Muted, fontSize = 13.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(relTime(c.lastMessage.created_at), color = Muted, fontSize = 11.sp)
                    if (c.unread > 0) {
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(20.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                            Text("${c.unread}", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            HorizontalDivider(color = Line.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 80.dp))
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (mine) Green else Surface2, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(12.dp, 7.dp)) {
                            if (!mine) Text(p?.displayName ?: "User", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(m.content, color = if (mine) androidx.compose.ui.graphics.Color.White else Text, fontSize = 14.sp)
                            Text(relTime(m.created_at), color = if (mine) androidx.compose.ui.graphics.Color.White.copy(alpha = .7f) else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }
        InputBar(text, onText = { text = it }, onEmoji = { showEmoji = true }, onSend = { if (text.isNotBlank()) { vm.sendGlobal(text); text = "" } })
    }
    if (showEmoji) com.altomedia.beruang.ui.components.EmojiPickerSheet(onInsert = { text += it }, onDismiss = { showEmoji = false })
}

@Composable
fun InputBar(text: String, onText: (String) -> Unit, onEmoji: () -> Unit, onSend: () -> Unit) {
    Surface(color = Surface, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { onText(it) },
                placeholder = { Text("Message…", color = Muted) },
                modifier = Modifier.weight(1f), singleLine = true,
                colors = outlinedFieldColors(), shape = RoundedCornerShape(50)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEmoji) { Text("😀", fontSize = 22.sp) }
            IconButton(onClick = onSend) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = Green) }
        }
    }
}

@Composable
private fun Modifier.clickableNoArg(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
