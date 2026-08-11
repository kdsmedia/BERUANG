package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    @DocumentId val id: String = "",
    val user_id: String = "",
    val content: String? = null,
    val image_url: String? = null,
    val video_url: String? = null,
    val location: String? = null,
    @ServerTimestamp val created_at: Timestamp? = null
)

data class Like(
    @DocumentId val id: String = "",
    val post_id: String = "",
    val user_id: String = ""
)

data class Comment(
    @DocumentId val id: String = "",
    val post_id: String = "",
    val user_id: String = "",
    val content: String = "",
    @ServerTimestamp val created_at: Timestamp? = null
)

data class Story(
    @DocumentId val id: String = "",
    val user_id: String = "",
    val image_url: String = "",
    @ServerTimestamp val created_at: Timestamp? = null
)
