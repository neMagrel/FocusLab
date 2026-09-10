package com.example.focuslab.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.DefaultFocusCategories
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.SUPPORTED_DURATIONS
import com.example.focuslab.ui.theme.FocusLabTheme

private val PreviewCategories = DefaultFocusCategories + listOf(
    FocusCategory("music", "🎵", "Музыкальная композиция"),
    FocusCategory("science", "🔬", "Научный проект"),
    FocusCategory("sport", "🏃", "Тренировка")
)

@Preview(name = "Components · 360 dp · Idle", widthDp = 360, showBackground = true)
@Composable
private fun NarrowIdleComponentsPreview() {
    FocusLabTheme(dynamicColor = false) {
        PreviewColumn {
            FocusHeader(appName = "Focus Lab")
            CategorySelector(
                categories = PreviewCategories,
                selectedId = "study",
                onSelected = {}
            )
            DurationSelector(
                durations = SUPPORTED_DURATIONS,
                selectedDuration = 25,
                onSelected = {}
            )
            ConfigureCategoriesButton(onClick = {})
            TimerCard(sessionUiState = FocusSessionUiState.Idle)
            FocusPrimaryButton(label = "Начать фокус", enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Components · 412 dp · Running disabled", widthDp = 412, showBackground = true)
@Composable
private fun RunningComponentsPreview() {
    val session = ActiveFocusSession(
        id = "preview",
        categoryId = "reading",
        durationMinutes = 25,
        startedAtEpochMillis = 0L,
        endsAtEpochMillis = 1_500_000L
    )
    FocusLabTheme(dynamicColor = false) {
        PreviewColumn {
            CategorySelector(
                categories = DefaultFocusCategories,
                selectedId = "reading",
                onSelected = {},
                enabled = false
            )
            DurationSelector(
                durations = SUPPORTED_DURATIONS,
                selectedDuration = 25,
                onSelected = {},
                enabled = false
            )
            TimerCard(
                sessionUiState = FocusSessionUiState.Running(
                    session = session,
                    category = DefaultFocusCategories[2],
                    remainingMillis = 754_000L,
                    remainingSeconds = 754L
                )
            )
            FocusPrimaryButton(label = "Фокус идёт", enabled = false, onClick = {})
        }
    }
}

@Preview(name = "Timer · Completed", widthDp = 360, showBackground = true)
@Composable
private fun CompletedTimerPreview() {
    FocusLabTheme(dynamicColor = false) {
        TimerCard(
            sessionUiState = FocusSessionUiState.JustCompleted(
                FocusCompletionFeedback(
                    sessionId = "preview",
                    category = DefaultFocusCategories.first(),
                    durationMinutes = 25,
                    rewardXp = 40
                )
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PreviewColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
