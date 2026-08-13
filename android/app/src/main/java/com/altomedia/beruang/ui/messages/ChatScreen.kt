package com.altomedia.beruang.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun ChatScreen(partnerUid: String, onBack: () -> Unit, vm: MessagesViewModel = hiltViewModel()) {
    LaunchedEffect(partnerUid) { vm.openThread(partnerUid) }
    val partner by vm.partner.collectAsStateWithLifecycle()
    val thread by vm.thread.collectAsStateWithLifecycle()
    val myUid by androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.profile.SessionViewModel>().uid.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = Text) }
                    Avatar(partner, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(partner?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                HorizontalDivider(color = Line, thickness = 0.5.dp)
            }
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            if (thread.isEmpty()) {
                EmptyState("👋", "Say hello to start the conversation.")
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(thread) { m ->
                        val mine = m.sender_id == myUid
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                            Surface(color = if (mine) Green else Surface2, shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.padding(12.dp, 7.dp)) {
                                    Text(m.content, color = if (mine) androidx.compose.ui.graphics.Color.White else Text, fontSize = 14.sp)
                                    Text(relTime(m.created_at), color = if (mine) androidx.compose.ui.graphics.Color.White.copy(alpha = .7f) else Muted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(thread.size) { if (thread.isNotEmpty()) listState.animateScrollToItem(thread.size - 1) }
            }
            com.altomedia.beruang.ui.messages.InputBar(text, onText = { text = it }, onSend = { if (text.isNotBlank()) { vm.send(partnerUid, text); text = "" } })
        }
    }
}
