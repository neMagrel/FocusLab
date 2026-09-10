package com.example.focuslab.focus.data

import com.example.focuslab.focus.model.FocusCategory
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object FocusCodec {
    private const val VERSION = "v1"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

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

    private fun encodeField(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeField(value: String): String =
        String(decoder.decode(value), StandardCharsets.UTF_8)
}
