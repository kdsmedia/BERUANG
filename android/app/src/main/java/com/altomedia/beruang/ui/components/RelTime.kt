package com.altomedia.beruang.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

private val isoParsers = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
).map { SimpleDateFormat(it, Locale.US) }.apply { forEach { it.isLenient = true } }

private fun parseIso(s: String?): Date? {
    if (s.isNullOrBlank()) return null
    for (p in isoParsers) runCatching { return p.parse(s) }
    // Fallback: try fixing trailing 'Z' (UTC) to '+00:00'
    val fixed = s.replace("Z", "+00:00")
    for (p in isoParsers) runCatching { return p.parse(fixed) }
    return null
}

fun relTime(ts: String?): String {
    val date = parseIso(ts) ?: return ""
    val diff = System.currentTimeMillis() - date.time
    val sec = TimeUnit.MILLISECONDS.toSeconds(diff)
    if (sec < 60) return "just now"
    val min = TimeUnit.MILLISECONDS.toMinutes(diff)
    if (min < 60) return "${min}m"
    val h = TimeUnit.MILLISECONDS.toHours(diff)
    if (h < 24) return "${h}h"
    val d = TimeUnit.MILLISECONDS.toDays(diff)
    if (d < 7) return "${d}d"
    return dateFmt.format(date)
}
