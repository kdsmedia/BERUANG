package com.altomedia.beruang.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** ISO-8601 timestamp helper used when the client sets created_at on inserts. */
private val isoFmt by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
}

fun isoNow(): String = isoFmt.format(Date())

fun isoOf(epochMillis: Long): String = isoFmt.format(Date(epochMillis))
