package com.altomedia.beruang.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.HashtagText
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.home.FeedItem
import com.altomedia.beruang.ui.theme.*

@Composable
fun PostCard(
    item: FeedItem,
    comments: List<Comment>?,
    commentProfiles: Map<String, Profile>,
    commentsExpanded: Boolean,
    onLike: () -> Unit,
    onToggleComments: () -> Unit,
    onComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDeletePost: () -> Unit
) {
    val post = item.post
    var menuOpen by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val profilesVm: com.altomedia.beruang.ui.profile.SessionViewModel = hiltViewModel()
    val currentUid by profilesVm.uid.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxWidth().background(Surface).padding(vertical = 8.dp)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(item.author, 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.author.displayName, color = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Filled.Verified, contentDescription = "verified", tint = Green, modifier = Modifier.size(14.dp))
                }
                Text(relTime(post.created_at), color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "menu", tint = Text)
            }
        }
        if (menuOpen) {
            Popup(onDismissRequest = { menuOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Surface,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Column(Modifier.padding(4.dp)) {
                        if (currentUid == post.user_id) {
                            DropdownItem("Delete", Icons.Filled.Delete, Danger) { menuOpen = false; onDeletePost() }
                        }
                        DropdownItem("Share", Icons.Outlined.Share, Text) {
                            menuOpen = false
                            val shareText = buildString {
                                append(item.author.displayName); append(": ")
                                post.content?.let { append(it); append("\n") }
                                append("\nShared from BERUANG")
                            }
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share post"))
                        }
                        DropdownItem("Cancel", Icons.Filled.Close, Muted) { menuOpen = false }
                    }
                }
            }
        }

        // Body (content)
        post.content?.takeIf { it.isNotBlank() }?.let {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) { HashtagText(it) }
        }

        // Media (kept for cross-platform posts; Android composer no longer creates these)
        if (post.image_url != null || post.video_url != null) {
            AsyncImage(
                model = post.image_url ?: post.video_url,
                contentDescription = "media",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action row
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (item.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "like",
                    tint = if (item.isLiked) Danger else Text
                )
            }
            IconButton(onClick = onToggleComments, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Comment, contentDescription = "comment", tint = Text)
            }
            IconButton(onClick = {
                val shareText = buildString {
                    append(item.author.displayName); append(": ")
                    post.content?.let { append(it); append("\n") }
                    append("\nShared from BERUANG")
                }
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share post"))
            }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Share, contentDescription = "share", tint = Text)
            }
            Spacer(Modifier.weight(1f))
        }

        // Likes count
        if (item.likeCount > 0) {
            Text("${item.likeCount} likes", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp))
        }

        // Comments
        if (commentsExpanded) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                if (comments == null) {
                    Text("Loading…", color = Muted, fontSize = 13.sp)
                } else if (comments.isEmpty()) {
                    Text("No comments yet.", color = Muted, fontSize = 13.sp)
                } else {
                    comments.forEach { c ->
                        CommentRow(c, commentProfiles[c.user_id], currentUid, onDeleteComment)
                    }
                }
            }
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText, onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment…", color = Muted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Text, unfocusedTextColor = Text,
                        cursorColor = Green
                    ),
                    shape = RoundedCornerShape(50)
                )
                IconButton(onClick = { if (commentText.isNotBlank()) { onComment(commentText.trim()); commentText = "" } }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = Green)
                }
            }
        } else if ((comments?.size ?: 0) > 0) {
            Text(
                "View all ${comments?.size ?: 0} comments",
                color = Muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).clickable { onToggleComments() }
            )
        }
    }
}

@Composable
private fun DropdownItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(12.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = tint, fontSize = 14.sp)
    }
}

@Composable
private fun CommentRow(c: Comment, profile: Profile?, currentUid: String?, onDelete: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Avatar(profile, 28.dp)
        Spacer(Modifier.width(8.dp))
        Surface(color = Surface2, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(10.dp, 6.dp)) {
                Text(profile?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                HashtagText(c.content)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(relTime(c.created_at), color = Muted, fontSize = 11.sp)
            if (currentUid == c.user_id) {
                IconButton(onClick = { onDelete(c.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "delete comment", tint = Muted, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
