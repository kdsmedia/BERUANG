package com.altomedia.beruang.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.data.model.Story
import com.altomedia.beruang.ui.components.EmojiPickerSheet
import com.altomedia.beruang.ui.components.EmptyState
import com.altomedia.beruang.ui.components.FeelingPickerSheet
import com.altomedia.beruang.ui.components.LocationPickerSheet
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.feed.PostCard
import com.altomedia.beruang.ui.theme.*
import com.altomedia.beruang.util.Feeling

@Composable
fun HomeScreen(openStory: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val stories by vm.stories.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val expanded by vm.expandedComments.collectAsStateWithLifecycle()

    LaunchedEffect(toast) { toast?.let { /* show snackbar handled by host */ } }

    var composerText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    var location by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("foryou") }

    var showEmoji by remember { mutableStateOf(false) }
    var showFeeling by remember { mutableStateOf(false) }
    var showLocation by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri = uri }
    val storyPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { vm.addStory(it) } }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snackbarHost.showSnackbar(it); vm.toastShown() } }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p)) {
            // Stories
            item {
                LazyRow(Modifier.padding(8.dp, 6.dp)) {
                    item {
                        AddStoryTile { storyPicker.launch("image/*") }
                    }
                    items(stories) { (s, profile) ->
                        StoryTile(s, profile) { openStory(s.image_url) }
                    }
                }
            }
            // Composer
            item {
                Composer(
                    text = composerText, onText = { composerText = it },
                    imageUri = imageUri, videoUri = videoUri, feeling = feeling, location = location,
                    onPhoto = { photoPicker.launch("image/*") },
                    onVideo = { videoPicker.launch("video/*") },
                    onFeeling = { showFeeling = true },
                    onLocation = { showLocation = true },
                    onEmoji = { showEmoji = true },
                    onClearMedia = { imageUri = null; videoUri = null },
                    onClearFeeling = { feeling = null },
                    onClearLocation = { location = null },
                    onPost = {
                        val feelingText = feeling?.let { "${it.emoji} is feeling ${it.label}" } ?: ""
                        val content = listOf(composerText.trim(), feelingText).filter { it.isNotBlank() }.joinToString("\n")
                        vm.createPost(content, imageUri, videoUri, location)
                        composerText = ""; imageUri = null; videoUri = null; feeling = null; location = null
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
    if (showLocation) LocationPickerSheet(onPick = { location = it }, onDismiss = { showLocation = false })
}

@Composable
private fun AddStoryTile(onAdd: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onAdd() }) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(Surface3), contentAlignment = Alignment.Center) {
            Text("+", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("Add Story", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun StoryTile(story: Story, profile: Profile, onOpen: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onOpen() }) {
        Box(
            Modifier.size(60.dp).clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Green, Gold)))
                .padding(2.dp)
        ) {
            AsyncImage(
                model = profile.avatarOrDefault,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Text(profile.displayName.split(" ").first(), color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun Composer(
    text: String, onText: (String) -> Unit,
    imageUri: Uri?, videoUri: Uri?, feeling: Feeling?, location: String?,
    onPhoto: () -> Unit, onVideo: () -> Unit, onFeeling: () -> Unit, onLocation: () -> Unit, onEmoji: () -> Unit,
    onClearMedia: () -> Unit, onClearFeeling: () -> Unit, onClearLocation: () -> Unit,
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
            if (feeling != null || location != null) {
                Row(Modifier.padding(top = 8.dp)) {
                    if (feeling != null) {
                        Tag("${feeling.emoji} is feeling ${feeling.label}", GoldSoft, Gold, onClearFeeling)
                    }
                    if (location != null) {
                        Spacer(Modifier.width(8.dp))
                        Tag("📍 $location", GreenSoft, Blue, onClearLocation)
                    }
                }
            }
            if (imageUri != null || videoUri != null) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))) {
                    if (imageUri != null) AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp))
                    else AsyncImage(model = videoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp))
                    TextButton(onClick = onClearMedia, modifier = Modifier.align(Alignment.TopEnd)) { Text("✕", color = Text) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    ComposerChip("Photo", Icons.Outlined.Image, Green, onPhoto)
                    ComposerChip("Video", Icons.Outlined.Videocam, Pink, onVideo)
                    ComposerChip("Feeling", Icons.Outlined.Mood, Gold, onFeeling)
                    ComposerChip("Location", Icons.Outlined.LocationOn, Blue, onLocation)
                    ComposerChip("Emoji", Icons.Outlined.Send, GreenBright, onEmoji)
                }
                Button(onClick = onPost, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg), shape = RoundedCornerShape(10.dp)) {
                    Text("Post", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ComposerChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
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
