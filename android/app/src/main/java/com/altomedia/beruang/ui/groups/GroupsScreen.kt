package com.altomedia.beruang.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
fun GroupsScreen(vm: GroupsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("mine") }
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.toast) { s.toast?.let { snackbar.showSnackbar(it); vm.toastShown() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            TabRow(selectedTabIndex = if (tab == "mine") 0 else 1, containerColor = Surface, contentColor = GreenBright) {
                Tab(selected = tab == "mine", onClick = { tab = "mine" }) { Text("My Groups", Modifier.padding(10.dp)) }
                Tab(selected = tab == "discover", onClick = { tab = "discover" }) { Text("Discover", Modifier.padding(10.dp)) }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = { showCreate = true }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Create Group")
                }
            }
            LazyColumn {
                if (s.loading) { item { EmptyState("⏳", "Loading…") }; return@LazyColumn }
                val mine = s.groups.filter { it.id in s.myGroupIds }
                val discover = s.groups.filter { it.id !in s.myGroupIds }
                val list = if (tab == "mine") mine else discover
                if (list.isEmpty()) item { EmptyState(if (tab == "mine") "👥" else "🧭", if (tab == "mine") "No groups joined." else "No groups to discover.") }
                else items(list) { g -> GroupCard(g, isMember = g.id in s.myGroupIds, onJoin = { vm.join(g.id) }, onLeave = { vm.leave(g.id) }) }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = {
                Button(onClick = { vm.create(name, desc); showCreate = false; name = ""; desc = "" }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel", color = Muted) } },
            title = { Text("Create Group", color = Text) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                }
            }
        )
    }
}

@Composable
private fun GroupCard(g: com.altomedia.beruang.data.model.Group, isMember: Boolean, onJoin: () -> Unit, onLeave: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(8.dp, 4.dp), shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column {
            Box(Modifier.fillMaxWidth().height(80.dp).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Gold))))
            Column(Modifier.padding(12.dp)) {
                Text(g.name, color = Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                g.description?.let { Text(it, color = Muted, fontSize = 13.sp) }
                Text("Group · ${relTime(g.created_at)}", color = Muted, fontSize = 12.sp)
                Row(Modifier.padding(top = 8.dp)) {
                    if (isMember) TextButton(onClick = onLeave, colors = ButtonDefaults.buttonColors(containerColor = Danger.copy(alpha = .2f), contentColor = Danger)) { Text("Leave") }
                    else TextButton(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Join") }
                    if (g.created_by == androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.profile.SessionViewModel>().uid.collectAsStateWithLifecycle().value) {
                        Spacer(Modifier.width(8.dp))
                        Text("admin", color = Gold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
