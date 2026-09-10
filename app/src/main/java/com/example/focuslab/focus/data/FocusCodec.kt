package com.example.focuslab.focus.data

import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.FocusCategory
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

internal object FocusCodec {
    private const val VERSION = "v1"
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun encode(categories: List<FocusCategory>): String = buildString {
        append(VERSION)
        categories.forEach { category ->
            append('\n')
            append(encodeField(category.id))
            append('.')
            append(encodeField(category.emoji))
            append('.')
            append(encodeField(category.title))
        }
    }

    fun decode(value: String): List<FocusCategory>? = runCatching {
        val lines = value.lines()
        require(lines.firstOrNull() == VERSION)

        lines.drop(1).map { line ->
            val fields = line.split('.')
            require(fields.size == 3)
            FocusCategory(
                id = decodeField(fields[0]),
                emoji = decodeField(fields[1]),
                title = decodeField(fields[2])
            )
        }
    }.getOrNull()

    fun encodeActiveSession(session: ActiveFocusSession): String = listOf(
        VERSION,
        encodeField(session.id),
        encodeField(session.categoryId),
        session.durationMinutes.toString(),
        session.startedAtEpochMillis.toString(),
        session.endsAtEpochMillis.toString()
    ).joinToString(".")

    fun decodeActiveSession(value: String): ActiveFocusSession? = runCatching {
        val fields = value.split('.')
        require(fields.size == 6)
        require(fields[0] == VERSION)
        ActiveFocusSession(
            id = decodeField(fields[1]),
            categoryId = decodeField(fields[2]),
            durationMinutes = fields[3].toInt(),
            startedAtEpochMillis = fields[4].toLong(),
            endsAtEpochMillis = fields[5].toLong()
        )
    }.getOrNull()

    private fun encodeField(value: String): String =
        base64.encode(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String =
        String(base64.decode(value), StandardCharsets.UTF_8)
}
