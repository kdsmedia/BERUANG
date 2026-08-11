package com.altomedia.beruang.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Comment
import com.altomedia.beruang.ui.components.Avatar
import com.altomedia.beruang.ui.components.HashtagText
import com.altomedia.beruang.ui.components.relTime
import com.altomedia.beruang.ui.home.FeedItem
import com.altomedia.beruang.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PostCard(
    item: FeedItem,
    comments: List<Comment>?,
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

    Surface(
        Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column {
            // Header
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(item.author, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.author.displayName, color = Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(" ✔", color = Green, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(relTime(post.created_at), color = Muted, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = "menu", tint = Muted)
                }
            }
            if (menuOpen) {
                Popup(onDismissRequest = { menuOpen = false }) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Surface2,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                    ) {
                        Column(Modifier.padding(6.dp)) {
                            if (currentUid == post.user_id) {
                                DropdownItem("Delete", Icons.Filled.Delete, Danger) { menuOpen = false; onDeletePost() }
                            }
                            DropdownItem("Save", Icons.Outlined.Send, Muted) { menuOpen = false }
                            DropdownItem("Cancel", Icons.Outlined.Send, Muted) { menuOpen = false }
                        }
                    }
                }
            }

            // Body
            post.content?.takeIf { it.isNotBlank() }?.let {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { HashtagText(it) }
            }
            // Media
            if (post.image_url != null || post.video_url != null) {
                Box(Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = post.image_url ?: post.video_url,
                        contentDescription = "media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    )
                }
            }
            // counts
            Row(Modifier.padding(12.dp, 6.dp)) {
                Text("${item.likeCount} likes", color = Green, fontSize = 12.sp)
                Spacer(Modifier.width(12.dp))
                Text("· ${comments?.size ?: 0} comments", color = Muted, fontSize = 12.sp)
            }
            HorizontalDivider(color = Line)
            // actions
            Row(Modifier.fillMaxWidth().padding(4.dp)) {
                Action("Like", if (item.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, if (item.isLiked) Green else Muted, onLike, Modifier.weight(1f))
                Action("Comment", Icons.Outlined.Comment, Muted, onToggleComments, Modifier.weight(1f))
                Action("Share", Icons.Outlined.Share, Muted, { /* noop */ }, Modifier.weight(1f))
            }

            if (commentsExpanded) {
                Column(Modifier.padding(12.dp, 4.dp)) {
                    if (comments == null) {
                        Text("Loading…", color = Muted, fontSize = 13.sp)
                    } else if (comments.isEmpty()) {
                        Text("No comments yet.", color = Muted, fontSize = 13.sp)
                    } else {
                        comments.forEach { c ->
                            CommentRow(c, currentUid, onDeleteComment)
                        }
                    }
                }
                Row(Modifier.padding(12.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText, onValueChange = { commentText = it },
                        placeholder = { Text("Write a comment…") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
                            focusedBorderColor = Green, unfocusedBorderColor = Line,
                            focusedTextColor = Text, unfocusedTextColor = Text
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    IconButton(onClick = { if (commentText.isNotBlank()) { onComment(commentText.trim()); commentText = "" } }) {
                        Icon(Icons.Filled.Send, contentDescription = "send", tint = Green)
                    }
                }
            } else {
                Text(
                    "View comments",
                    color = Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp, 4.dp).clickable { onToggleComments() }
                )
            }
        }
    }
}

@Composable
private fun DropdownItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        Modifier.clickable { onClick() }.padding(10.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = tint, fontSize = 14.sp)
    }
}

@Composable
private fun Action(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.then(Modifier.clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 13.sp)
    }
}

@Composable
private fun CommentRow(c: Comment, currentUid: String?, onDelete: (String) -> Unit) {
    Row(Modifier.padding(vertical = 6.dp)) {
        com.altomedia.beruang.ui.components.AvatarFromUrl(
            com.altomedia.beruang.data.model.Profile.dicebearAvatar(c.user_id), 32.dp
        )
        Spacer(Modifier.width(8.dp))
        Surface(color = Surface2, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(8.dp, 6.dp)) {
                Text("User", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                HashtagText(c.content)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(relTime(c.created_at), color = Muted, fontSize = 11.sp)
                    if (currentUid == c.user_id) {
                        Text("  ✕", color = Danger, fontSize = 11.sp, modifier = Modifier.clickable { onDelete(c.id) })
                    }
                }
            }
        }
    }
}
