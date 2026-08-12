package com.altomedia.beruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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

/**
 * Lightweight, dependency-free pull-to-refresh. Wrap a scrollable (e.g. a
 * LazyColumn that uses the provided [content] with the given [listState]) and
 * drag down at the top to trigger [onRefresh]. Shows a spinner while
 * [isRefreshing] is true. Uses nested-scroll so it cooperates with the child's
 * own scrolling.
 */
@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { 88.dp.toPx() }
    val maxPullPx = with(density) { 140.dp.toPx() }
    var pull by remember { mutableStateOf(0f) }

    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Child couldn't scroll up further (we're at the top) and the
                // user is still dragging down → accumulate pull distance.
                val dy = available.y
                if (dy > 0f) {
                    pull = (pull + dy).coerceAtMost(maxPullPx)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pull >= refreshThresholdPx && !isRefreshing) onRefresh()
                pull = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier
            .nestedScroll(nestedScroll)
    ) {
        val shown = if (isRefreshing) refreshThresholdPx else pull
        val progress = (shown / refreshThresholdPx).coerceIn(0f, 1f)
        CircularProgressIndicator(
            progress = { progress },
            color = Green,
            strokeWidth = 2.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, (shown / 2).roundToInt()) }
                .size(28.dp)
                .graphicsLayer { this.alpha = if (isRefreshing || pull > 0f) 1f else 0f }
        )
        Box(Modifier.offset { IntOffset(0, shown.roundToInt()) }) { content() }
    }
}
