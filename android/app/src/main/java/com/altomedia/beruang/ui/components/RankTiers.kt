package com.altomedia.beruang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Account rank tiers based on accumulated points. */
enum class RankTier(val key: String, val label: String, val emoji: String, val color: Color, val min: Long) {
    Start("start", "Start", "🌱", Color(0xFF8E8E8E), 0),
    Bronze("bronze", "Bronze", "🥉", Color(0xFFCD7F32), 100),
    Silver("silver", "Silver", "🥈", Color(0xFFB0B0B0), 500),
    Gold("gold", "Gold", "🥇", Color(0xFFF59E0B), 2000),
    Master("master", "Master", "👑", Color(0xFF9333EA), 10000);

    companion object {
        fun forPoints(points: Long): RankTier {
            // Iterate from highest tier downwards so the highest reached tier wins.
            var result = Start
            for (t in entries) if (points >= t.min) result = t
            return result
        }

        /** Points still needed to reach the next tier (0 if already at Master). */
        fun nextTier(points: Long): RankTier? = entries.firstOrNull { it.min > points }
    }
}

/** A small pill showing the rank emoji + label, colored by tier. */
@Composable
fun RankBadge(points: Long, modifier: Modifier = Modifier) {
    val tier = RankTier.forPoints(points)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(tier.color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(tier.emoji, fontSize = 13.sp)
        Spacer(Modifier.width(5.dp))
        Text(tier.label, color = tier.color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
