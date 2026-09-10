package com.example.focuslab.focus.model

data class ActiveFocusSession(
    val id: String,
    val categoryId: String,
    val durationMinutes: Int,
    val startedAtEpochMillis: Long,
    val endsAtEpochMillis: Long
)
