package com.altomedia.beruang.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.altomedia.beruang.ui.theme.Bg
import kotlinx.coroutines.delay

@Composable
fun StoryViewer(url: String, onClose: () -> Unit) {
    LaunchedEffect(Unit) { delay(6000); onClose() }
    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(model = url, contentDescription = "story", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Text("✕", color = Color.White, fontSize = 26.sp)
        }
    }
}
