package com.altomedia.beruang.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String = "",
    val user_id: String = "",
    val content: String? = null,
    val image_url: String? = null,
    val video_url: String? = null,
    val location: String? = null,
    val created_at: String? = null
)

@Serializable
data class Like(
    val id: String = "",
    val post_id: String = "",
    val user_id: String = ""
)

@Serializable
data class Comment(
    val id: String = "",
    val post_id: String = "",
    val user_id: String = "",
    val content: String = "",
    val created_at: String? = null
)

@Serializable
data class Story(
    val id: String = "",
    val user_id: String = "",
    val image_url: String = "",
    val created_at: String? = null
)
