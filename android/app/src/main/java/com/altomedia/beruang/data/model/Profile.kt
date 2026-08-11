package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Profile(
    @DocumentId val id: String = "",
    val full_name: String? = null,
    val bio: String? = "Hey there! I am using JavaGoat.",
    val avatar_url: String? = null,
    val cover_url: String? = null,
    @ServerTimestamp val created_at: Timestamp? = null
) {
    val displayName get() = full_name ?: "New Goat"
    val avatarOrDefault get() = avatar_url?.ifBlank { null } ?: dicebearAvatar(id)
    companion object {
        fun dicebearAvatar(seed: String) =
            "https://api.dicebear.com/7.x/avataaars/svg?seed=${seed.ifBlank { "guest" }}"
    }
}
