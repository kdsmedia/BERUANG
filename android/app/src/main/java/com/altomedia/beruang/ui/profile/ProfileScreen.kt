package com.altomedia.beruang.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*
import java.io.File

@Composable
private fun profileAvatarModel(prof: Profile?): Any? {
    if (prof == null) return null
    val url = prof.avatar_url?.ifBlank { null } ?: return Profile.dicebearAvatar(prof.id)
    if (url.startsWith("file://")) {
        val exists = remember(url) { runCatching { File(url.removePrefix("file://")).exists() }.getOrDefault(false) }
        return if (exists) url else Profile.dicebearAvatar(prof.id)
    }
    return url
}

@Composable
fun ProfileScreen(uid: String?, vm: ProfileViewModel = hiltViewModel()) {
    val sessionVm: SessionViewModel = hiltViewModel()
    val myUid by sessionVm.uid.collectAsStateWithLifecycle()
    val targetUid = uid ?: myUid
    LaunchedEffect(targetUid) { targetUid?.let { vm.load(it) } }
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("posts") }
    var showEdit by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.toast) { s.toast?.let { snackbar.showSnackbar(it); vm.toastShown() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Profile", color = Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                HorizontalDivider(color = Line, thickness = 0.5.dp)
            }
        }
    ) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p)) {
            item {
                val prof = s.profile
                Column(Modifier.fillMaxWidth().background(Surface).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(88.dp).clip(CircleShape).background(Surface3)) {
                            val avatarModel = profileAvatarModel(prof)
                            avatarModel?.let {
                                AsyncImage(model = it, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround) {
                            Stat(s.posts.size.toString(), "Posts")
                            Stat(s.friendProfiles.size.toString(), "Friends")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prof?.displayName ?: "User", color = Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, contentDescription = "verified", tint = Green, modifier = Modifier.size(15.dp))
                    }
                    Text(prof?.bio ?: "", color = Text, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(12.dp))
                    if (targetUid == myUid) {
                        Row(Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showEdit = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Edit Profile", fontWeight = FontWeight.SemiBold) }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Text),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                                modifier = Modifier.weight(1f)
                            ) { Text("Log out") }
                        }
                    }
                }
                HorizontalDivider(color = Line, thickness = 6.dp)
            }
            item {
                TabRow(selectedTabIndex = listOf("posts","about","friends").indexOf(tab), containerColor = Surface, contentColor = Green) {
                    Tab(selected = tab == "posts", onClick = { tab = "posts" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("Posts", Modifier.padding(10.dp)) }
                    Tab(selected = tab == "about", onClick = { tab = "about" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("About", Modifier.padding(10.dp)) }
                    Tab(selected = tab == "friends", onClick = { tab = "friends" }, selectedContentColor = Green, unselectedContentColor = Muted) { Text("Friends", Modifier.padding(10.dp)) }
                }
            }
            when (tab) {
                "posts" -> {
                    if (s.posts.isEmpty()) item { EmptyState("📰", "No posts yet.") }
                    else items(s.posts) { post ->
                        Surface(Modifier.fillMaxWidth().padding(8.dp, 4.dp), shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                            Column {
                                post.content?.let { Text(it, color = Text, modifier = Modifier.padding(12.dp)) }
                                post.image_url?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) }
                                Text(relTime(post.created_at), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(12.dp, 4.dp))
                            }
                        }
                    }
                }
                "about" -> item {
                    Column(Modifier.padding(14.dp)) {
                        AboutRow("Bio", s.profile?.bio ?: "No bio yet.")
                        AboutRow("Name", s.profile?.displayName ?: "User")
                        AboutRow("Joined", relTime(s.profile?.created_at))
                    }
                }
                "friends" -> {
                    if (s.friendProfiles.isEmpty()) item { EmptyState("🧑", "No friends yet.") }
                    else items(s.friendProfiles) { f ->
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(f, 42.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(f.displayName, color = Text)
                        }
                    }
                }
            }
        }
    }

    if (showEdit) EditProfileDialog(prof = s.profile, onDismiss = { showEdit = false }, onSave = { n, b, uri -> vm.updateProfile(n, b, uri); showEdit = false })
}

@Composable
private fun Stat(n: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(n, color = Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Surface(Modifier.fillMaxWidth().padding(0.dp, 4.dp), shape = RoundedCornerShape(14.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Text, fontWeight = FontWeight.SemiBold)
            Text(value, color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EditProfileDialog(prof: Profile?, onDismiss: () -> Unit, onSave: (String, String, Uri?) -> Unit) {
    var name by remember { mutableStateOf(prof?.displayName ?: "") }
    var bio by remember { mutableStateOf(prof?.bio ?: "") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSave(name, bio, avatarUri) }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White), shape = RoundedCornerShape(8.dp)) { Text("Save", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        title = { Text("Edit Profile", color = Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(Modifier.size(96.dp).clip(CircleShape).background(Surface3), contentAlignment = Alignment.Center) {
                    val url = avatarUri?.toString() ?: prof?.avatarOrDefault
                    if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                }
                TextButton(onClick = { picker.launch("image/*") }) { Text("Change avatar", color = Green, fontWeight = FontWeight.SemiBold) }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
            }
        }
    )
}
