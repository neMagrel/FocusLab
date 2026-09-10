package com.example.focuslab.focus.model

fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
