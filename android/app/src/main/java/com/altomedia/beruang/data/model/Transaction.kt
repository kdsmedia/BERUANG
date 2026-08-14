package com.altomedia.beruang.data.model

import kotlinx.serialization.Serializable

/** A points transfer between two users. */
@Serializable
data class Transaction(
    val id: String = "",
    val from_id: String = "",
    val to_id: String = "",
    val amount: Long = 0,
    val created_at: String? = null
)
