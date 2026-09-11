package com.example.focuslab.focus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.ui.theme.FocusLabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReusableComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val categories = listOf(
        FocusCategory("study", "📚", "Учёба"),
        FocusCategory("long", "🎹", "Музыкальная композиция")
    )

    @Test
    fun categorySelectorRendersSelectionAndEmitsEnabledClick() {
        var selectedId: String? = null
        composeRule.setContent {
            FocusLabTheme {
                CategorySelector(
                    categories = categories,
                    selectedId = "study",
                    onSelected = { selectedId = it }
                )
            }
        }

        composeRule.onNodeWithText("Учёба", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("category_selector_study").assertIsSelected()
        composeRule.onNodeWithTag("category_selector_long").performClick()

        composeRule.runOnIdle { assertEquals("long", selectedId) }
    }

    @Test
    fun disabledCategoryAndDurationSelectorsDoNotEmitClicks() {
        var clicks = 0
        composeRule.setContent {
            FocusLabTheme {
                androidx.compose.foundation.layout.Column {
                    CategorySelector(
                        categories = categories,
                        selectedId = "study",
                        onSelected = { clicks++ },
                        enabled = false
                    )
                    DurationSelector(
                        durations = listOf(1, 5, 15, 25),
                        selectedDuration = 25,
                        onSelected = { clicks++ },
                        enabled = false
                    )
                }
            }
        }

        composeRule.onNodeWithTag("category_selector_long").assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag("duration_selector_5").assertIsNotEnabled().performClick()
        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun durationSelectorRendersValuesSelectionAndEmitsClick() {
        var selectedDuration: Int? = null
        composeRule.setContent {
            FocusLabTheme {
                DurationSelector(
                    durations = listOf(1, 5, 15, 25),
                    selectedDuration = 15,
                    onSelected = { selectedDuration = it }
                )
            }
        }

        listOf(1, 5, 15, 25).forEach { duration ->
            composeRule.onNodeWithText("$duration мин").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("duration_selector_15").assertIsSelected()
        composeRule.onNodeWithTag("duration_selector_5").performClick()
        composeRule.runOnIdle { assertEquals(5, selectedDuration) }
    }

    @Test
    fun primaryButtonUsesProvidedLabelAndEnabledState() {
        var clicks = 0
        composeRule.setContent {
            FocusLabTheme {
                androidx.compose.foundation.layout.Column {
                    FocusPrimaryButton(
                        label = "Начать фокус",
                        enabled = true,
                        onClick = { clicks++ }
                    )
                    FocusPrimaryButton(
                        label = "Фокус идёт",
                        enabled = false,
                        onClick = { clicks++ }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Начать фокус").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Фокус идёт").assertIsNotEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun timerCardRendersIdleRunningAndCompletedStates() {
        val session = ActiveFocusSession(
            id = "session",
            categoryId = "study",
            durationMinutes = 25,
            startedAtEpochMillis = 0L,
            endsAtEpochMillis = 1_500_000L
        )
        val running = FocusSessionUiState.Running(
            session = session,
            category = categories.first(),
            remainingMillis = 754_000L,
            remainingSeconds = 754L
        )
        val completed = FocusSessionUiState.JustCompleted(
            FocusCompletionFeedback(
                sessionId = session.id,
                category = categories.first(),
                durationMinutes = 25,
                rewardXp = 40
            )
        )

        composeRule.setContent {
            FocusLabTheme {
                androidx.compose.foundation.layout.Column {
                    TimerCard(FocusSessionUiState.Idle)
                    TimerCard(running)
                    TimerCard(completed)
                }
            }
        }

        composeRule.onNodeWithText("Готовы начать?").assertIsDisplayed()
        composeRule.onNodeWithText("12:34").assertIsDisplayed()
        composeRule.onNodeWithText("Фокус завершён").assertIsDisplayed()
        composeRule.onNodeWithText("📚 Учёба · +40 XP").assertIsDisplayed()
    }

    @Test
    fun progressCardRendersProvidedValues() {
        composeRule.setContent {
            FocusLabTheme {
                ProgressCard(
                    xp = 70,
                    level = 1,
                    completedSessions = 4
                )
            }
        }

        composeRule.onNodeWithText("XP: 70").assertIsDisplayed()
        composeRule.onNodeWithText("Уровень: 1").assertIsDisplayed()
        composeRule.onNodeWithText("Завершено сессий: 4").assertIsDisplayed()
    }

    @Test
    fun focusScreenForwardsSelectionAndStartAndDisplaysAuthoritativeValues() {
        var selectedCategoryId: String? = null
        var selectedDuration: Int? = null
        var startClicks = 0
        composeRule.setContent {
            FocusLabTheme {
                FocusScreen(
                    uiState = FocusUiState(
                        categories = categories,
                        selectedCategoryId = "study",
                        selectedDurationMinutes = 1,
                        session = FocusSessionUiState.Idle
                    ),
                    onCategorySelected = { selectedCategoryId = it },
                    onDurationSelected = { selectedDuration = it },
                    onStartFocus = { startClicks++ },
                    onManageCategories = {}
                )
            }
        }

        composeRule.onNodeWithTag("category_selector_study").assertIsSelected()
        composeRule.onNodeWithTag("duration_selector_1").assertIsSelected()
        composeRule.onNodeWithTag("category_selector_long").performClick()
        composeRule.onNodeWithTag("duration_selector_15").performClick()
        composeRule.onNodeWithTag("focus_primary_button").assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals("long", selectedCategoryId)
            assertEquals(15, selectedDuration)
            assertEquals(1, startClicks)
        }
    }
}
