package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    @DocumentId val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val content: String = "",
    val read: Boolean = false,
    @ServerTimestamp val created_at: Timestamp? = null
)

data class GlobalMessage(
    @DocumentId val id: String = "",
    val user_id: String = "",
    val content: String = "",
    @ServerTimestamp val created_at: Timestamp? = null
)

data class Friendship(
    @DocumentId val id: String = "",
    val user_id: String = "",
    val friend_id: String = "",
    val status: String = "pending", // "pending" | "accepted"
    @ServerTimestamp val created_at: Timestamp? = null
)
