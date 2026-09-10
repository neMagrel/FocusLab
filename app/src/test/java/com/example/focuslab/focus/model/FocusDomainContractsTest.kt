package com.example.focuslab.focus.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusDomainContractsTest {
    @Test
    fun `supported durations contain exactly the v1 values`() {
        assertEquals(listOf(1, 5, 15, 25), SUPPORTED_DURATIONS)
        assertTrue(isSupportedDuration(1))
        assertTrue(isSupportedDuration(25))
        assertFalse(isSupportedDuration(10))
    }

    @Test
    fun `reward follows the v1 lookup table`() {
        assertEquals(10, rewardForDuration(1))
        assertEquals(20, rewardForDuration(5))
        assertEquals(30, rewardForDuration(15))
        assertEquals(40, rewardForDuration(25))
    }

    @Test
    fun `reward rejects an unsupported duration`() {
        assertThrows(IllegalArgumentException::class.java) {
            rewardForDuration(10)
        }
    }

    @Test
    fun `level is derived at each one hundred xp boundary`() {
        assertEquals(1, levelForXp(0))
        assertEquals(1, levelForXp(99))
        assertEquals(2, levelForXp(100))
        assertEquals(2, levelForXp(199))
        assertEquals(3, levelForXp(200))
    }

    @Test
    fun `level rejects negative xp`() {
        assertThrows(IllegalArgumentException::class.java) {
            levelForXp(-1)
        }
    }

    @Test
    fun `remaining time is derived from absolute timestamps and clamped to zero`() {
        assertEquals(2_500L, remainingMillis(endsAtEpochMillis = 10_000L, nowEpochMillis = 7_500L))
        assertEquals(0L, remainingMillis(endsAtEpochMillis = 10_000L, nowEpochMillis = 10_000L))
        assertEquals(0L, remainingMillis(endsAtEpochMillis = 10_000L, nowEpochMillis = 12_000L))
    }

    @Test
    fun `normalization trims collapses whitespace and lowercases deterministically`() {
        assertEquals("учёба", normalizeCategoryTitle("  Учёба  "))
        assertEquals("учёба", normalizeCategoryTitle("учёба"))
        assertEquals("мой проект", normalizeCategoryTitle("Мой   проект"))
        assertEquals("мой проект", normalizeCategoryTitle("мой\tпроект"))
        assertFalse(normalizeCategoryTitle("Код") == normalizeCategoryTitle("Коды"))
    }

    @Test
    fun `category draft validation enforces title emoji and normalized uniqueness`() {
        val categories = listOf(
            FocusCategory(id = "study", emoji = "📚", title = "Учёба"),
            FocusCategory(id = "code", emoji = "💻", title = "Мой проект")
        )

        assertEquals(
            CategoryDraftError.BlankTitle,
            validateCategoryDraft("   ", "📚", categories)
        )
        assertNull(validateCategoryDraft("А", "📚", categories))
        assertNull(validateCategoryDraft("А".repeat(24), "📚", categories))
        assertEquals(
            CategoryDraftError.TitleTooLong,
            validateCategoryDraft("А".repeat(25), "📚", categories)
        )
        assertEquals(
            CategoryDraftError.DuplicateTitle,
            validateCategoryDraft("  мой   ПРОЕКТ ", "🎨", categories)
        )
        assertNull(
            validateCategoryDraft("мой проект", "🎨", categories, editedCategoryId = "code")
        )
        assertEquals(
            CategoryDraftError.DuplicateTitle,
            validateCategoryDraft("учёба", "🎨", categories, editedCategoryId = "code")
        )
        assertEquals(
            CategoryDraftError.InvalidEmoji,
            validateCategoryDraft("Музыка", "not-an-emoji", categories)
        )
    }

    @Test
    fun `default categories have canonical values and unique stable ids`() {
        assertEquals(
            listOf(
                FocusCategory(id = "study", emoji = "📚", title = "Учёба"),
                FocusCategory(id = "code", emoji = "💻", title = "Код"),
                FocusCategory(id = "reading", emoji = "📖", title = "Чтение"),
                FocusCategory(id = "creativity", emoji = "🎨", title = "Творчество")
            ),
            DefaultFocusCategories
        )
        assertEquals(
            DefaultFocusCategories.size,
            DefaultFocusCategories.map(FocusCategory::id).toSet().size
        )
    }
}
