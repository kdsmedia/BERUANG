package com.altomedia.beruang.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.feed.PostCard
import com.altomedia.beruang.ui.theme.*
import com.altomedia.beruang.util.Feeling

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val expanded by vm.expandedComments.collectAsStateWithLifecycle()

    var composerText by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    var activeTab by remember { mutableStateOf("foryou") }

    var showEmoji by remember { mutableStateOf(false) }
    var showFeeling by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snackbarHost.showSnackbar(it); vm.toastShown() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { p ->
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
private fun Composer(
    text: String, onText: (String) -> Unit,
    feeling: Feeling?,
    onFeeling: () -> Unit, onEmoji: () -> Unit,
    onClearFeeling: () -> Unit,
    onPost: () -> Unit
) {
    Surface(Modifier.fillMaxWidth().padding(8.dp), shape = RoundedCornerShape(16.dp), color = Surface, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(14.dp)) {
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
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    ComposerChip("Feeling", Icons.Outlined.Mood, Gold, onFeeling)
                    ComposerChip("Emoji", Icons.Outlined.SentimentSatisfied, GreenBright, onEmoji)
                }
                // Send (post) icon button
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPost, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post", tint = Bg, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerChip(label: String, icon: ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Surface2).clickable { onClick() }.padding(8.dp, 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Text, fontSize = 12.sp)
    }
}

@Composable
private fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, onClear: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(6.dp, 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 12.sp)
        Text("  ✕", color = fg, fontSize = 12.sp, modifier = Modifier.clickable { onClear() })
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) GreenSoft else Surface2)
            .clickable { onClick() }.padding(8.dp, 7.dp)
    ) {
        Text(label, color = if (selected) GreenBright else Muted, fontSize = 13.sp)
    }
}
