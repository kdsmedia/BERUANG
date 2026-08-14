package com.altomedia.beruang.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val content: String = "",
    val read: Boolean = false,
    val created_at: String? = null
)

@Serializable
data class GlobalMessage(
    val id: String = "",
    val user_id: String = "",
    val content: String = "",
    val created_at: String? = null
)

@Serializable
data class Friendship(
    val id: String = "",
    val user_id: String = "",
    val friend_id: String = "",
    val status: String = "pending", // "pending" | "accepted"
    val created_at: String? = null
)
