package com.altomedia.beruang.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun ChatScreen(partnerUid: String, onBack: () -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    LaunchedEffect(partnerUid) { vm.openThread(partnerUid) }
    val partner by vm.partner.collectAsStateWithLifecycle()
    val thread by vm.thread.collectAsStateWithLifecycle()
    val myUid by androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.profile.SessionViewModel>().uid.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Text) }
            Avatar(partner, 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(partner?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showEmoji = true }) { Text("😀", fontSize = 22.sp) }
        }
        HorizontalDivider(color = Line)
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            if (thread.isEmpty()) item { Text("Say hello 👋", color = Muted, modifier = Modifier.padding(16.dp)) }
            items(thread) { m ->
                val mine = m.sender_id == myUid
                Row(Modifier.fillMaxWidth().padding(8.dp, 4.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (mine) Green else Surface2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(10.dp, 6.dp)) {
                            Text(m.content, color = if (mine) Bg else Text, fontSize = 14.sp)
                            Text(relTime(m.created_at), color = if (mine) Bg else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        LaunchedEffect(thread.size) { if (thread.isNotEmpty()) listState.animateScrollToItem(thread.size - 1) }
        Row(Modifier.padding(10.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Message…") },
                modifier = Modifier.weight(1f), singleLine = true,
                colors = outlinedFieldColors(), shape = RoundedCornerShape(50)
            )
            IconButton(onClick = { if (text.isNotBlank()) { vm.send(partnerUid, text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = Green)
            }
        }
    }
    if (showEmoji) EmojiPickerSheet(onInsert = { text += it }, onDismiss = { showEmoji = false })
}
