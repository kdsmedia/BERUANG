package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Profile(
    @DocumentId val id: String = "",
    val full_name: String? = null,
    val bio: String? = "Hey there! I am using BERUANG.",
    val avatar_url: String? = null,
    val cover_url: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gender: String? = null, // "male" | "female" | "other"
    val points: Long = 0,
    val points_pin: String? = null, // SHA-256 hash of the 4-digit PIN
    val account_id: String? = null, // 6-digit virtual account number
    @ServerTimestamp val created_at: Timestamp? = null
) {
    val displayName get() = full_name ?: "New Goat"
    /** True when the avatar is one of the bundled presets (stored as "preset:<key>"). */
    val isPreset get() = avatar_url?.startsWith("preset:") == true
    /** Legacy/default avatar: dicebear generated image (needs internet). */
    val avatarOrDefault get() = avatar_url?.ifBlank { null } ?: dicebearAvatar(id)
    companion object {
        fun dicebearAvatar(seed: String) =
            "https://api.dicebear.com/7.x/avataaars/svg?seed=${seed.ifBlank { "guest" }}"
    }
}
