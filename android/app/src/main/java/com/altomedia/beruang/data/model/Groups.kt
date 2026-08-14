package com.altomedia.beruang.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val cover_url: String? = null,
    val created_by: String? = null,
    val created_at: String? = null
)

@Serializable
data class GroupMember(
    val id: String = "",
    val group_id: String = "",
    val user_id: String = "",
    val role: String = "member", // "admin" | "member"
    val created_at: String? = null
)

@Serializable
data class Notification(
    val id: String = "",
    val user_id: String = "",
    val type: String = "", // like | comment | friend_request | friend_accept | message
    val from_user_id: String? = null,
    val reference_id: String? = null,
    val content: String? = null,
    val read: Boolean = false,
    val created_at: String? = null
)
