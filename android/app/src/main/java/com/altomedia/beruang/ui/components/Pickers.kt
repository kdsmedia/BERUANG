package com.altomedia.beruang.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.beruang.ui.theme.*
import com.altomedia.beruang.util.EMOJI_CATEGORIES
import com.altomedia.beruang.util.FEELINGS
import com.altomedia.beruang.util.LOCATIONS

/** Bottom sheet style wrapper used by pickers. */
@Composable
fun AppBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val dismissSource = remember { MutableInteractionSource() }
    val stopSource = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Bg.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss, interactionSource = dismissSource, indication = null),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            Modifier.fillMaxWidth().fillMaxHeight(0.7f)
                .clickable(onClick = {}, interactionSource = stopSource, indication = null),
            color = Surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    TextButton(onClick = onDismiss) { Text("Close", color = Muted) }
                }
                HorizontalDivider(color = Line)
                content()
            }
        }
    }
}

@Composable
fun EmojiPickerSheet(onInsert: (String) -> Unit, onDismiss: () -> Unit) {
    var cat by remember { mutableStateOf(EMOJI_CATEGORIES.keys.first()) }
    var query by remember { mutableStateOf("") }
    AppBottomSheet("Emoji", onDismiss) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            placeholder = { Text("Search…") },
            singleLine = true,
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(50)
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            EMOJI_CATEGORIES.keys.forEach { c ->
                Chip(text = c, color = if (c == cat) GreenSoft else Surface2, fg = if (c == cat) GreenBright else Muted) { cat = c }
                Spacer(Modifier.width(6.dp))
            }
        }
        val list = EMOJI_CATEGORIES[cat] ?: emptyList()
        val filtered = if (query.isBlank()) list else list // emoji are not searchable by text
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            items(filtered) { e ->
                TextButton(onClick = { onInsert(e); onDismiss() }, modifier = Modifier.padding(0.dp)) {
                    Text(e, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun FeelingPickerSheet(onPick: (com.altomedia.beruang.util.Feeling) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AppBottomSheet("How are you feeling?", onDismiss) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            placeholder = { Text("Search feelings…") },
            singleLine = true,
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(50)
        )
        val filtered = FEELINGS.filter { it.label.contains(query.lowercase()) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 18.dp)) {
            filtered.forEach { f ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(f); onDismiss() }.padding(12.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(f.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("is feeling ${f.label}", color = Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun LocationPickerSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    AppBottomSheet("Add location", onDismiss) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            placeholder = { Text("Search cities…") },
            singleLine = true,
            colors = outlinedFieldColors(),
            shape = RoundedCornerShape(50)
        )
        val filtered = LOCATIONS.filter { it.contains(query, ignoreCase = true) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 18.dp)) {
            filtered.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(c); onDismiss() }.padding(12.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📍", fontSize = 18.sp, color = Blue)
                    Spacer(Modifier.width(10.dp))
                    Text(c, color = Text, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
    focusedBorderColor = Green, unfocusedBorderColor = Line,
    focusedTextColor = Text, unfocusedTextColor = Text,
    cursorColor = Green
)
