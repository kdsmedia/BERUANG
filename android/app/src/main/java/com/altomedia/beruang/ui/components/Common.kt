package com.altomedia.beruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.beruang.ui.theme.*

/** Inline hashtag highlighter: renders "#word" runs in green-yellow. */
@Composable
fun HashtagText(text: String) {
    val styled = androidx.compose.ui.text.buildAnnotatedString {
        val pattern = Regex("(^|\\s)(#\\w+)")
        var idx = 0
        for (m in pattern.findAll(text)) {
            append(text.substring(idx, m.range.first))
            pushStyle(androidx.compose.ui.text.SpanStyle(color = GreenBright, fontWeight = FontWeight.SemiBold))
            append(m.groupValues[2])
            pop()
            idx = m.range.last + 1
        }
        if (idx < text.length) append(text.substring(idx))
    }
    Text(styled, color = Text, fontSize = 15.sp)
}

@Composable
fun EmptyState(icon: String, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 34.sp)
        Spacer(Modifier.height(12.dp))
        Text(text, color = Muted, fontSize = 14.sp)
    }
}

@Composable
fun Chip(
    text: String,
    color: androidx.compose.ui.graphics.Color = Surface3,
    fg: androidx.compose.ui.graphics.Color = Text,
    onClick: () -> Unit
) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 7.dp)
            .clickableNoRipple(onClick)
    ) { Text(text, color = fg, fontSize = 13.sp) }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    )
