package com.altomedia.beruang.ui.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search your friends…") },
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
                    item { SectionTitle("Friend Requests (${s.pendingInProfiles.size})") }
                    items(s.pendingInProfiles) { p ->
                        val f = s.state.pendingIn.first { it.user_id == p.id }
                        FriendRow(p, "wants to be your friend", accept = { vm.accept(f) }, decline = { vm.decline(f) })
                    }
                }
                // accepted
                val friends = s.acceptedProfiles.filter { q.isBlank() || it.displayName.contains(q, true) }
                item { SectionTitle("Your Friends (${friends.size})") }
                if (friends.isEmpty()) item { EmptyState("🧑", if (q.isBlank()) "No friends yet." else "No friends match.") }
                else items(friends) { p ->
                    FriendRow(
                        p, p.bio ?: "",
                        message = { openChat(p.id) },
                        remove = { vm.remove(p.id) }
                    )
                }
                // suggested
                val sugg = s.suggestedProfiles.filter { q.isBlank() || it.displayName.contains(q, true) }
                item { SectionTitle("People You May Know") }
                if (sugg.isEmpty()) item { EmptyState("➕", "No suggestions.") }
                else items(sugg) { p -> FriendRow(p, p.bio ?: "", add = { vm.send(p.id) }) }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(14.dp, 12.dp, 14.dp, 6.dp))
}

@Composable
private fun FriendRow(
    p: com.altomedia.beruang.data.model.Profile,
    sub: String,
    add: (() -> Unit)? = null,
    accept: (() -> Unit)? = null,
    decline: (() -> Unit)? = null,
    message: (() -> Unit)? = null,
    remove: (() -> Unit)? = null
) {
    Surface(Modifier.fillMaxWidth().padding(8.dp, 4.dp, 8.dp, 4.dp), shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.displayName, color = Text, fontWeight = FontWeight.SemiBold)
                Text(sub, color = Muted, fontSize = 12.sp)
            }
            Row {
                if (add != null) TextButton(onClick = add, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Add") }
                if (accept != null) TextButton(onClick = accept, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Accept") }
                if (decline != null) TextButton(onClick = decline, colors = ButtonDefaults.buttonColors(containerColor = Danger.copy(alpha = .2f), contentColor = Danger)) { Text("Decline") }
                if (message != null) TextButton(onClick = message) { Text("💬") }
                if (remove != null) TextButton(onClick = remove, colors = ButtonDefaults.buttonColors(containerColor = Danger.copy(alpha = .2f), contentColor = Danger)) { Text("Remove") }
            }
        }
    }
}
