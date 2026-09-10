package com.example.focuslab.focus

import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.SUPPORTED_DURATIONS
import com.example.focuslab.focus.model.isSupportedDuration
import com.example.focuslab.focus.model.levelForXp

sealed interface FocusSessionUiState {
    data object Loading : FocusSessionUiState

    data object Idle : FocusSessionUiState

    data class Running(
        val session: ActiveFocusSession,
        val category: FocusCategory,
        val remainingMillis: Long,
        val remainingSeconds: Long
    ) : FocusSessionUiState

    data class JustCompleted(
        val feedback: FocusCompletionFeedback
    ) : FocusSessionUiState
}

data class FocusCompletionFeedback(
    val sessionId: String,
    val category: FocusCategory,
    val durationMinutes: Int,
    val rewardXp: Int
)

data class FocusUiState(
    val categories: List<FocusCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val supportedDurations: List<Int> = SUPPORTED_DURATIONS,
    val selectedDurationMinutes: Int? = null,
    val xp: Int = 0,
    val completedSessions: Int = 0,
    val session: FocusSessionUiState = FocusSessionUiState.Loading
) {
    val level: Int
        get() = levelForXp(xp)

    val isReady: Boolean
        get() = session !is FocusSessionUiState.Loading

    val isRunning: Boolean
        get() = session is FocusSessionUiState.Running

    val selectionEnabled: Boolean
        get() = isReady && !isRunning

    val canStart: Boolean
        get() = selectionEnabled &&
            selectedCategoryId != null &&
            categories.any { it.id == selectedCategoryId } &&
            selectedDurationMinutes?.let(::isSupportedDuration) == true

    val currentSessionCategory: FocusCategory?
        get() = when (val current = session) {
            is FocusSessionUiState.Running -> current.category
            is FocusSessionUiState.JustCompleted -> current.feedback.category
            FocusSessionUiState.Idle,
            FocusSessionUiState.Loading -> null
        }

    val remainingMillis: Long
        get() = (session as? FocusSessionUiState.Running)?.remainingMillis ?: 0L

    val remainingSeconds: Long
        get() = (session as? FocusSessionUiState.Running)?.remainingSeconds ?: 0L
}
