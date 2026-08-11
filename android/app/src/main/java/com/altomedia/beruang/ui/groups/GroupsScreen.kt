package com.altomedia.beruang.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    Scaffold(
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Groups", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, contentDescription = "create", tint = Green) }
                    }
                    TabRow(selectedTabIndex = if (tab == "mine") 0 else 1, containerColor = Surface, contentColor = Green) {
                        Tab(selected = tab == "mine", onClick = { tab = "mine" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("My Groups", Modifier.padding(10.dp)) }
                        Tab(selected = tab == "discover", onClick = { tab = "discover" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("Discover", Modifier.padding(10.dp)) }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p)) {
            if (s.loading) { item { EmptyState("⏳", "Loading…") }; return@LazyColumn }
            val mine = s.groups.filter { it.id in s.myGroupIds }
            val discover = s.groups.filter { it.id !in s.myGroupIds }
            val list = if (tab == "mine") mine else discover
            if (list.isEmpty()) item { EmptyState(if (tab == "mine") "👥" else "🧭", if (tab == "mine") "No groups joined yet." else "No groups to discover.") }
            else items(list) { g -> GroupCard(g, isMember = g.id in s.myGroupIds, onJoin = { vm.join(g.id) }, onLeave = { vm.leave(g.id) }) }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            confirmButton = { Button(onClick = { vm.create(name, desc); showCreate = false; name = ""; desc = "" }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White)) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel", color = Muted) } },
            title = { Text("Create Group", color = Text, fontWeight = FontWeight.Bold) },
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
    val sessionUid = androidx.hilt.navigation.compose.hiltViewModel<com.altomedia.beruang.ui.profile.SessionViewModel>().uid.collectAsStateWithLifecycle().value
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp)).background(Surface)
    ) {
        Box(
            Modifier.fillMaxWidth().height(70.dp)
                .background(Brush.linearGradient(listOf(Green, Gold))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Group, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, color = Text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (g.created_by == sessionUid) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(GoldSoft).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("admin", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            g.description?.let { Text(it, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                if (isMember) {
                    OutlinedButton(onClick = onLeave, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = androidx.compose.foundation.BorderStroke(1.dp, Danger.copy(alpha = .4f)), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Leave", fontSize = 12.sp)
                    }
                } else {
                    Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) { Text("Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
