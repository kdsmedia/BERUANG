package com.altomedia.beruang.ui.components

import androidx.annotation.DrawableRes
import com.altomedia.beruang.R

/** A ready-to-use 2D cartoon avatar preset (no upload, no Firebase Storage). */
data class PresetAvatar(val key: String, val name: String, @DrawableRes val resId: Int)

/** The 5 bundled cartoon bear avatars users can pick from. */
val PRESET_AVATARS = listOf(
    PresetAvatar("bear_green", "Green Bear", R.drawable.avatar_bear_green),
    PresetAvatar("bear_gold", "Gold Bear", R.drawable.avatar_bear_gold),
    PresetAvatar("bear_panda", "Panda", R.drawable.avatar_bear_panda),
    PresetAvatar("bear_brown", "Smart Bear", R.drawable.avatar_bear_brown),
    PresetAvatar("bear_cap", "Cap Bear", R.drawable.avatar_bear_cap)
)

/** Marker prefix stored in profile.avatar_url when a preset is chosen. */
const val PRESET_PREFIX = "preset:"

/** Returns the drawable resource for a stored avatar_url, or null if not a preset. */
fun presetDrawableFor(avatarUrl: String?): Int? {
    if (avatarUrl == null || !avatarUrl.startsWith(PRESET_PREFIX)) return null
    val key = avatarUrl.removePrefix(PRESET_PREFIX)
    return PRESET_AVATARS.firstOrNull { it.key == key }?.resId
}

fun presetKeyToUrl(key: String) = PRESET_PREFIX + key
