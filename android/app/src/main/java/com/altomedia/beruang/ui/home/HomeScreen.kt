package com.altomedia.beruang.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.SentimentSatisfied
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
import com.altomedia.beruang.ui.components.EmojiPickerSheet
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.FeelingPickerSheet
import com.altomedia.beruang.ui.components.clickableNoRipple
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.feed.PostCard
import com.altomedia.beruang.ui.theme.*
import com.altomedia.beruang.util.Feeling

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel(), onAlerts: () -> Unit = {}) {
    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val commentProfiles by vm.commentProfiles.collectAsStateWithLifecycle()
    val expanded by vm.expandedComments.collectAsStateWithLifecycle()

    val badgesVm: com.altomedia.beruang.ui.notifs.BadgesViewModel = hiltViewModel()
    val notifUnread by badgesVm.notifUnread.collectAsStateWithLifecycle()

    var composerText by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    var activeTab by remember { mutableStateOf("foryou") }

    var showEmoji by remember { mutableStateOf(false) }
    var showFeeling by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snackbarHost.showSnackbar(it); vm.toastShown() } }

    Scaffold(
        topBar = { HomeTopBar(notifUnread = notifUnread, onAlerts = onAlerts) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Bg
    ) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p)) {
            // Composer
            item {
                Composer(
                    text = composerText, onText = { composerText = it },
                    feeling = feeling,
                    onFeeling = { showFeeling = true },
                    onEmoji = { showEmoji = true },
                    onClearFeeling = { feeling = null },
                    onPost = {
                        val feelingText = feeling?.let { "${it.emoji} is feeling ${it.label}" } ?: ""
                        val content = listOf(composerText.trim(), feelingText).filter { it.isNotBlank() }.joinToString("\n")
                        if (content.isBlank()) return@Composer
                        vm.createPost(content)
                        composerText = ""; feeling = null
                    }
                )
            }
            // Filter tabs
            item {
                val tabs = listOf("foryou" to "For You", "friends" to "Friends", "groups" to "Groups", "trending" to "Trending")
                LazyRow(Modifier.padding(8.dp, 8.dp)) {
                    items(tabs) { (id, label) ->
                        Chip(label, selected = activeTab == id) { activeTab = id }
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
            // Feed
            if (loading && items.isEmpty()) {
                item { EmptyState("⏳", "Loading feed…") }
            } else if (items.isEmpty()) {
                item { EmptyState("📰", "No posts yet. Be the first to share!") }
            } else {
                items(items) { item ->
                    PostCard(
                        item = item,
                        comments = comments[item.post.id],
                        commentProfiles = commentProfiles,
                        commentsExpanded = item.post.id in expanded,
                        onLike = { vm.toggleLike(item) },
                        onToggleComments = { vm.toggleComments(item.post.id) },
                        onComment = { text -> vm.addComment(item.post, text) },
                        onDeleteComment = { cid -> vm.deleteComment(cid, item.post.id) },
                        onDeletePost = { vm.deletePost(item.post) }
                    )
                }
            }
        }
    }

    if (showEmoji) EmojiPickerSheet(onInsert = { composerText += it }, onDismiss = { showEmoji = false })
    if (showFeeling) FeelingPickerSheet(onPick = { feeling = it }, onDismiss = { showFeeling = false })
}

@Composable
private fun HomeTopBar(notifUnread: Int, onAlerts: () -> Unit) {
    Surface(color = Surface, shadowElevation = 0.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text("BERU", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("ANG", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = onAlerts) {
                        Icon(Icons.Filled.Notifications, contentDescription = "alerts", tint = Text, modifier = Modifier.size(26.dp))
                    }
                    if (notifUnread > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                                .size(16.dp).clip(CircleShape).background(Danger),
                            contentAlignment = Alignment.Center
                        ) { Text(if (notifUnread > 99) "99+" else notifUnread.toString(), fontSize = 9.sp, color = Surface, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            HorizontalDivider(color = Line, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun Composer(
    text: String, onText: (String) -> Unit,
    feeling: Feeling?,
    onFeeling: () -> Unit, onEmoji: () -> Unit,
    onClearFeeling: () -> Unit,
    onPost: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Surface).padding(14.dp)) {
        OutlinedTextField(
            value = text, onValueChange = onText,
            placeholder = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(12.dp)
        )
        if (feeling != null) {
            Row(Modifier.padding(top = 8.dp)) {
                Tag("${feeling.emoji} is feeling ${feeling.label}", GoldSoft, Gold, onClearFeeling)
            }
        }
        HorizontalDivider(color = Line, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row {
                ComposerChip("Feeling", Icons.Outlined.Mood, Gold, onFeeling)
                Spacer(Modifier.width(8.dp))
                ComposerChip("Emoji", Icons.Outlined.SentimentSatisfied, GreenBright, onEmoji)
            }
            Button(
                onClick = onPost,
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "post", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Post", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
    HorizontalDivider(color = Line, thickness = 6.dp)
}

@Composable
private fun ComposerChip(label: String, icon: ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Surface2).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, onClear: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(6.dp))
        Text("✕", color = fg, fontSize = 12.sp, modifier = Modifier.clickable { onClear() })
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) GreenSoft else Surface2)
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Green else Muted, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
