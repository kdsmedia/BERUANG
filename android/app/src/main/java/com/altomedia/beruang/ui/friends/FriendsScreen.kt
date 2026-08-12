package com.altomedia.beruang.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
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
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.theme.*

@Composable
fun FriendsScreen(
    openChat: (String) -> Unit,
    openProfile: (String) -> Unit,
    vm: FriendsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.toast) { s.toast?.let { snackbar.showSnackbar(it); vm.toastShown() } }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Column(Modifier.statusBarsPadding()) {
                    Text("Friends", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    HorizontalDivider(color = Line, thickness = 0.5.dp)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search friends…", color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(14.dp, 10.dp),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(50)
            )
            LazyColumn(Modifier.weight(1f)) {
                if (s.loading) { item { EmptyState("⏳", "Loading…") }; return@LazyColumn }
                val q = query.lowercase()
                // requests
                if (s.pendingInProfiles.isNotEmpty()) {
                    item { SectionTitle("Friend Requests") }
                    items(s.pendingInProfiles) { p ->
                        val f = s.state.pendingIn.firstOrNull { it.user_id == p.id } ?: return@items
                        FriendRow(p, "wants to be your friend", accept = { vm.accept(f) }, decline = { vm.decline(f) })
                    }
                }
                // accepted
                val friends = s.acceptedProfiles.filter { q.isBlank() || it.displayName.contains(q, true) }
                item { SectionTitle("Your Friends · ${friends.size}") }
                if (friends.isEmpty()) item { EmptyState("🧑", if (q.isBlank()) "No friends yet." else "No friends match.") }
                else items(friends) { p ->
                    FriendRow(p, p.bio ?: "", message = { openChat(p.id) }, remove = { vm.remove(p.id) })
                }
                // suggested
                val sugg = s.suggestedProfiles.filter { q.isBlank() || it.displayName.contains(q, true) }
                if (sugg.isNotEmpty()) {
                    item { SectionTitle("People You May Know") }
                    items(sugg) { p -> FriendRow(p, p.bio ?: "", add = { vm.send(p.id) }) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@Composable
private fun FriendRow(
    p: Profile,
    sub: String,
    add: (() -> Unit)? = null,
    accept: (() -> Unit)? = null,
    decline: (() -> Unit)? = null,
    message: (() -> Unit)? = null,
    remove: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.displayName, color = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (sub.isNotBlank()) Text(sub, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
        if (add != null) {
            Button(onClick = add, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Add", fontSize = 12.sp) }
        }
        if (accept != null) {
            Button(onClick = accept, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("Accept", fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            OutlinedButton(onClick = decline ?: {}, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Text("Decline", fontSize = 12.sp) }
        }
        if (message != null) {
            IconButton(onClick = message, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Outlined.Comment, contentDescription = "message", tint = Text) }
        }
        if (remove != null) {
            IconButton(onClick = remove, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.PersonRemove, contentDescription = "remove", tint = Muted) }
        }
    }
    HorizontalDivider(color = Line.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 76.dp))
}
