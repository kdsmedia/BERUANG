package com.altomedia.beruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.theme.Green
import com.altomedia.beruang.ui.theme.Surface3

/**
 * Resolves the avatar model to show. Order:
 * 1) Bundled preset ("preset:<key>") → local drawable (works offline, no Storage).
 * 2) Legacy file:// path → only on the device that created it.
 * 3) http(s) URL → remote (dicebear) avatar.
 * 4) null → dicebear fallback.
 */
@Composable
private fun effectiveAvatar(profile: Profile?): Any? {
    if (profile == null) return null
    val url = profile.avatar_url?.ifBlank { null }
    if (url == null) return Profile.dicebearAvatar(profile.id)
    presetDrawableFor(url)?.let { return it }
    if (url.startsWith("file://")) {
        val exists = remember(url) { runCatching { java.io.File(url.removePrefix("file://")).exists() }.getOrDefault(false) }
        return if (exists) url else Profile.dicebearAvatar(profile.id)
    }
    return url
}

@Composable
fun Avatar(profile: Profile?, size: Dp = 42.dp) {
    val model = effectiveAvatar(profile)
    Box(
        Modifier.size(size).clip(CircleShape).background(Surface3),
        contentAlignment = Alignment.Center
    ) {
        when (model) {
            is Int -> androidx.compose.foundation.Image(
                painter = painterResource(model),
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(CircleShape)
            )
            else -> if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            } else {
                Text(profile?.displayName?.firstOrNull()?.toString() ?: "B", color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
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
