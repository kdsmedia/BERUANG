package com.altomedia.beruang.ui.notifs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun NotificationsScreen(openProfile: (String) -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", color = Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.markAll() }) { Text("Mark all read", color = Muted) }
        }
        HorizontalDivider(color = Line)
        if (s.loading) EmptyState("⏳", "Loading…")
        else if (s.items.isEmpty()) EmptyState("🔔", "No notifications yet.")
        else {
            LazyColumn {
                items(s.items) { n ->
                    val p = n.from_user_id?.let { s.fromProfiles[it] }
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.markRead(n.id); n.from_user_id?.let(openProfile) }
                            .background(if (!n.read) GreenSoft else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (n.from_user_id != null) {
                            com.altomedia.beruang.ui.components.Avatar(p, 42.dp)
                        } else {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
                                Text(iconForType(n.type), fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${p?.displayName ?: ""} ${n.content ?: "You have a new notification"}".trim(),
                                color = Text, fontSize = 14.sp)
                            Text(relTime(n.created_at), color = Muted, fontSize = 11.sp)
                        }
                        if (!n.read) Box(Modifier.size(9.dp).clip(CircleShape).background(Gold))
                    }
                }
            }
        }
    }
}

private fun iconForType(type: String): String = when (type) {
    "like" -> "👍"; "comment" -> "💬"; "friend_request" -> "➕"; "friend_accept" -> "🤝"; "message" -> "✉️"; else -> "🔔"
}
