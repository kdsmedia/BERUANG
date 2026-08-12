package com.altomedia.beruang.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.PRESET_AVATARS
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.components.presetDrawableFor
import com.altomedia.beruang.ui.components.presetKeyToUrl
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.theme.*

@Composable
private fun profileAvatarModel(prof: Profile?): Any? {
    if (prof == null) return null
    presetDrawableFor(prof.avatar_url)?.let { return it }
    val url = prof.avatar_url?.ifBlank { null } ?: return Profile.dicebearAvatar(prof.id)
    if (url.startsWith("file://")) {
        val exists = remember(url) { runCatching { java.io.File(url.removePrefix("file://")).exists() }.getOrDefault(false) }
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

    // Points wallet dialogs (own profile only)
    var showMyQr by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var scannedAccountId by remember { mutableStateOf<String?>(null) }
    var transferBusy by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Bg,
        topBar = {
            Surface(color = Surface) {
                Column(Modifier.statusBarsPadding()) {
                    Text("Profile", color = Text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    HorizontalDivider(color = Line, thickness = 0.5.dp)
                }
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
                            avatarModel?.let { model ->
                                if (model is Int) {
                                    Image(painter = painterResource(model), contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                } else {
                                    AsyncImage(model = model, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                }
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
            // Points wallet — only on the signed-in user's own profile.
            if (targetUid == myUid) {
                item {
                    ProfilePointsSection(
                        profile = s.profile,
                        onShowQr = { showMyQr = true },
                        onScan = { scanning = true }
                    )
                }
            }
            item {
                TabRow(selectedTabIndex = listOf("posts", "about", "friends").indexOf(tab), containerColor = Surface, contentColor = Green) {
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
                                Text(relTime(post.created_at), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(12.dp, 4.dp))
                            }
                        }
                    }
                }
                "about" -> item {
                    Column(Modifier.padding(14.dp)) {
                        AboutRow("Bio", s.profile?.bio ?: "No bio yet.")
                        AboutRow("Name", s.profile?.displayName ?: "User")
                        AboutRow("Phone", s.profile?.phone?.ifBlank { null } ?: "-")
                        AboutRow("Email", s.profile?.email?.ifBlank { null } ?: "-")
                        AboutRow("Gender", when (s.profile?.gender) { "male" -> "Pria"; "female" -> "Wanita"; "other" -> "Lainnya"; else -> "-" })
                        AboutRow("Account ID", s.profile?.account_id ?: "-")
                        AboutRow("Points", "${s.profile?.points ?: 0} pts")
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

    if (showEdit) {
        EditProfileDialog(
            prof = s.profile,
            onDismiss = { showEdit = false },
            onSave = { n, b, preset, phone, email, gender ->
                vm.updateProfile(n, b, preset, phone, email, gender); showEdit = false
            }
        )
    }

    if (showMyQr) {
        MyQrDialog(profile = s.profile, onDismiss = { showMyQr = false })
    }

    if (scanning) {
        QrScannerScreen(
            onScanned = { value ->
                scanning = false
                scannedAccountId = value
            },
            onBack = { scanning = false }
        )
    }

    val target = scannedAccountId
    if (target != null) {
        TransferDialog(
            recipientAccountId = target,
            hasPin = s.hasPin,
            busy = transferBusy,
            onDismiss = { scannedAccountId = null },
            onCreatePin = { pin ->
                vm.setPin(pin)
                scannedAccountId = null // PIN dibuat; scan ulang untuk transfer
            },
            onTransfer = { amount, pin ->
                transferBusy = true
                vm.transfer(target, amount, pin)
                scannedAccountId = null
                transferBusy = false
            }
        )
    }
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
private fun EditProfileDialog(
    prof: Profile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(prof?.displayName ?: "") }
    var bio by remember { mutableStateOf(prof?.bio ?: "") }
    var phone by remember { mutableStateOf(prof?.phone ?: "") }
    var email by remember { mutableStateOf(prof?.email ?: "") }
    var gender by remember { mutableStateOf(prof?.gender) }
    // current selected preset key (null = keep existing / dicebear)
    var selectedPreset by remember { mutableStateOf(prof?.avatar_url?.takeIf { it.startsWith("preset:") }?.removePrefix("preset:")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSave(name, bio, selectedPreset?.let { presetKeyToUrl(it) }, phone.trim(), email.trim(), gender) },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        title = { Text("Edit Profile", color = Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Choose avatar", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(PRESET_AVATARS) { pa ->
                        val selected = selectedPreset == pa.key
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(Surface2)
                                .then(if (selected) Modifier.border(3.dp, Green, CircleShape) else Modifier.border(1.dp, Line, CircleShape))
                                .clickable { selectedPreset = pa.key },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painter = painterResource(pa.resId), contentDescription = pa.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() } }, label = { Text("Nomor HP (08xxx)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (opsional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                Text("Jenis kelamin", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("male" to "Pria", "female" to "Wanita", "other" to "Lainnya").forEach { (k, label) ->
                        val selected = gender == k
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(50))
                                .then(if (selected) Modifier.background(Green) else Modifier.background(Surface2))
                                .clickable { gender = if (gender == k) null else k }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = if (selected) androidx.compose.ui.graphics.Color.White else Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), colors = outlinedFieldColors(), shape = RoundedCornerShape(12.dp))
            }
        }
    )
}
