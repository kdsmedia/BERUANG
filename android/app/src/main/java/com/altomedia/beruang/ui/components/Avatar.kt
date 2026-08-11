package com.altomedia.beruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.theme.Bg
import com.altomedia.beruang.ui.theme.Green
import com.altomedia.beruang.ui.theme.Surface3

@Composable
fun Avatar(profile: Profile?, size: Dp = 42.dp) {
    val url = profile?.avatarOrDefault
    Box(
        Modifier.size(size).clip(CircleShape).background(Surface3),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            Text(profile?.displayName?.firstOrNull()?.toString() ?: "B", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AvatarFromUrl(url: String?, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Surface3),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
        } else {
            Text("B", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
