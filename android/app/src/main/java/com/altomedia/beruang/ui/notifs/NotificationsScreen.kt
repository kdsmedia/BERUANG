package com.altomedia.beruang.ui.notifs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
fun NotificationsScreen(openProfile: (String) -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.markAll() }) {
                        Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mark all read", color = Green, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = Line, thickness = 0.5.dp)
            }
        }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            if (s.loading) EmptyState("⏳", "Loading…")
            else if (s.items.isEmpty()) EmptyState("🔔", "No notifications yet.")
            else {
                LazyColumn {
                    items(s.items) { n ->
                        val p = n.from_user_id?.let { s.fromProfiles[it] }
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.markRead(n.id); n.from_user_id?.let(openProfile) }
                                .background(if (!n.read) GreenSoft else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (n.from_user_id != null) {
                                Avatar(p, 44.dp)
                            } else {
                                Box(Modifier.size(44.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) {
                                    Icon(iconForType(n.type), contentDescription = null, tint = Green, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${p?.displayName ?: ""} ${n.content ?: "You have a new notification"}".trim(),
                                    color = Text, fontSize = 14.sp)
                                Text(relTime(n.created_at), color = Muted, fontSize = 11.sp)
                            }
                            if (!n.read) Box(Modifier.size(8.dp).clip(CircleShape).background(Danger))
                        }
                        HorizontalDivider(color = Line.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

private fun iconForType(type: String?): ImageVector = when (type) {
    "like" -> Icons.Filled.Favorite
    "comment" -> Icons.Outlined.Comment
    "friend_request" -> Icons.Outlined.GroupAdd
    "friend_accept" -> Icons.Outlined.Handshake
    "message" -> Icons.Outlined.Mail
    else -> Icons.Outlined.NotificationsActive
}
