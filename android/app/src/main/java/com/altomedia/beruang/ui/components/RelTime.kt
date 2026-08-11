package com.altomedia.beruang.ui.components

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

fun relTime(ts: Timestamp?): String {
    if (ts == null) return ""
    val date: Date = ts.toDate()
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
