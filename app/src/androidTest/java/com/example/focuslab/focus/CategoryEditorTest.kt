package com.example.focuslab.focus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.ui.theme.FocusLabTheme
import org.junit.Rule
import org.junit.Test

class CategoryEditorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addFlowSavesAndDisplaysCategory() {
        var categories by mutableStateOf(
            listOf(FocusCategory(id = "study", emoji = "📚", title = "Учёба"))
        )

        composeRule.setContent {
            FocusLabTheme {
                CategoryEditor(
                    categories = categories,
                    enabled = true,
                    onAddCategory = { title, emoji ->
                        categories = categories + FocusCategory("music", emoji, title)
                    },
                    onEditCategory = { _, _, _ -> },
                    onDeleteCategory = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("category_editor_add").performClick()
        composeRule.onNodeWithTag("category_editor_title").performTextInput("Музыка")
        composeRule.onNodeWithTag("category_editor_save").performClick()

        composeRule.onNodeWithText("📚 Музыка").assertIsDisplayed()
    }
}
