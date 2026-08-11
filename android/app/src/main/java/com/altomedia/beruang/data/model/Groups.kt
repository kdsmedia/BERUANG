package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Group(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String? = null,
    val cover_url: String? = null,
    val created_by: String? = null,
    @ServerTimestamp val created_at: Timestamp? = null
)

data class GroupMember(
    @DocumentId val id: String = "",
    val group_id: String = "",
    val user_id: String = "",
    val role: String = "member", // "admin" | "member"
    @ServerTimestamp val created_at: Timestamp? = null
)

data class Notification(
    @DocumentId val id: String = "",
    val user_id: String = "",
    val type: String = "", // like | comment | friend_request | friend_accept | message
    val from_user_id: String? = null,
    val reference_id: String? = null,
    val content: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val created_at: Timestamp? = null
)
