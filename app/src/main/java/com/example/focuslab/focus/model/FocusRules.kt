package com.example.focuslab.focus.model

import java.util.Locale

val SUPPORTED_DURATIONS: List<Int> = listOf(1, 5, 15, 25)

fun isSupportedDuration(durationMinutes: Int): Boolean =
    durationMinutes in SUPPORTED_DURATIONS

fun rewardForDuration(durationMinutes: Int): Int = when (durationMinutes) {
    1 -> 10
    5 -> 20
    15 -> 30
    25 -> 40
    else -> throw IllegalArgumentException("Unsupported duration: $durationMinutes minutes")
}

fun levelForXp(xp: Int): Int {
    require(xp >= 0) { "XP must not be negative" }
    return xp / 100 + 1
}

fun normalizeCategoryTitle(title: String): String =
    title.trim()
        .replace(Regex("\\s+"), " ")
        .lowercase(Locale.ROOT)

fun remainingMillis(
    endsAtEpochMillis: Long,
    nowEpochMillis: Long
): Long = maxOf(0L, endsAtEpochMillis - nowEpochMillis)
