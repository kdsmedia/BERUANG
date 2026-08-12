package com.altomedia.beruang.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/** A points transfer between two users. */
data class Transaction(
    @DocumentId val id: String = "",
    val from_id: String = "",
    val to_id: String = "",
    val amount: Long = 0,
    @ServerTimestamp val created_at: Timestamp? = null
)
