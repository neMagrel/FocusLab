package com.example.focuslab.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focuslab.focus.data.FocusRepository
import com.example.focuslab.focus.data.FocusRepositorySnapshot
import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.SystemTimeProvider
import com.example.focuslab.focus.model.TimeProvider
import com.example.focuslab.focus.model.remainingMillis
import com.example.focuslab.focus.model.rewardForDuration
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val DEFAULT_TICK_INTERVAL_MILLIS = 1_000L
private const val DEFAULT_COMPLETION_FEEDBACK_MILLIS = 3_000L

class FocusViewModel(
    private val repository: FocusRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val tickIntervalMillis: Long = DEFAULT_TICK_INTERVAL_MILLIS,
    private val completionFeedbackDurationMillis: Long = DEFAULT_COMPLETION_FEEDBACK_MILLIS
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = mutableUiState.asStateFlow()

    private var latestSnapshot: FocusRepositorySnapshot? = null
    private var tickerJob: Job? = null
    private var tickerSessionId: String? = null
    private var completionInFlightSessionId: String? = null
    private var completionFeedback: FocusCompletionFeedback? = null
    private var feedbackJob: Job? = null

    init {
        require(tickIntervalMillis > 0L) { "Tick interval must be positive" }
        require(completionFeedbackDurationMillis >= 0L) {
            "Completion feedback duration must not be negative"
        }

        viewModelScope.launch {
            repository.snapshots.collect(::onRepositorySnapshot)
        }
    }

    fun onCategorySelected(categoryId: String) {
        val snapshot = latestSnapshot ?: return
        if (snapshot.activeSession != null || snapshot.categories.none { it.id == categoryId }) {
            return
        }

        viewModelScope.launch {
            repository.selectCategory(categoryId)
        }
    }

    fun onDurationSelected(durationMinutes: Int) {
        val snapshot = latestSnapshot ?: return
        if (snapshot.activeSession != null || durationMinutes !in uiState.value.supportedDurations) {
            return
        }

        viewModelScope.launch {
            repository.selectDuration(durationMinutes)
        }
    }

    fun onStartFocus() {
        if (!uiState.value.canStart || latestSnapshot?.activeSession != null) return

        clearCompletionFeedback()
        latestSnapshot?.let(::publishSnapshot)
        viewModelScope.launch {
            repository.startFocusSession()
        }
    }

    fun onAddCategory(title: String, emoji: String) {
        val snapshot = latestSnapshot ?: return
        if (snapshot.activeSession != null) return

        val category = FocusCategory(
            id = UUID.randomUUID().toString(),
            emoji = emoji,
            title = title.trim()
        )
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun onEditCategory(categoryId: String, title: String, emoji: String) {
        val snapshot = latestSnapshot ?: return
        if (snapshot.activeSession != null) return
        val existing = snapshot.categories.firstOrNull { it.id == categoryId } ?: return

        viewModelScope.launch {
            repository.updateCategory(
                existing.copy(emoji = emoji, title = title.trim())
            )
        }
    }

    fun onDeleteCategory(categoryId: String) {
        val snapshot = latestSnapshot ?: return
        if (snapshot.activeSession != null) return

        viewModelScope.launch {
            repository.deleteCategory(categoryId)
        }
    }

    private fun onRepositorySnapshot(snapshot: FocusRepositorySnapshot) {
        latestSnapshot = snapshot
        val session = snapshot.activeSession
        if (session == null) {
            stopTicker()
            publishSnapshot(snapshot)
            return
        }

        clearCompletionFeedback()
        val nowEpochMillis = timeProvider.nowEpochMillis()
        if (session.endsAtEpochMillis <= nowEpochMillis) {
            stopTicker()
            publishSnapshot(snapshot, nowEpochMillis)
            requestCompletion(session)
        } else {
            publishSnapshot(snapshot, nowEpochMillis)
            ensureTicker(session)
        }
    }

    private fun ensureTicker(session: ActiveFocusSession) {
        if (tickerSessionId == session.id && tickerJob?.isActive == true) return

        stopTicker()
        tickerSessionId = session.id
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(tickIntervalMillis)
                val snapshot = latestSnapshot ?: continue
                val currentSession = snapshot.activeSession
                if (currentSession?.id != session.id) break

                val nowEpochMillis = timeProvider.nowEpochMillis()
                publishSnapshot(snapshot, nowEpochMillis)
                if (currentSession.endsAtEpochMillis <= nowEpochMillis) {
                    requestCompletion(currentSession)
                    break
                }
            }
        }
    }

    private fun requestCompletion(session: ActiveFocusSession) {
        if (completionInFlightSessionId == session.id) return
        completionInFlightSessionId = session.id

        viewModelScope.launch {
            try {
                if (repository.completeSessionIfActive(session.id)) {
                    showCompletionFeedback(session)
                }
            } finally {
                if (completionInFlightSessionId == session.id) {
                    completionInFlightSessionId = null
                }
            }
        }
    }

    private fun showCompletionFeedback(session: ActiveFocusSession) {
        val snapshot = latestSnapshot ?: return
        val category = snapshot.categories.firstOrNull { it.id == session.categoryId } ?: return
        val feedback = FocusCompletionFeedback(
            sessionId = session.id,
            category = category,
            durationMinutes = session.durationMinutes,
            rewardXp = rewardForDuration(session.durationMinutes)
        )
        completionFeedback = feedback
        publishSnapshot(snapshot)

        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            delay(completionFeedbackDurationMillis)
            if (completionFeedback?.sessionId == feedback.sessionId) {
                completionFeedback = null
                latestSnapshot?.let(::publishSnapshot)
            }
        }
    }

    private fun clearCompletionFeedback() {
        completionFeedback = null
        feedbackJob?.cancel()
        feedbackJob = null
    }

    private fun publishSnapshot(
        snapshot: FocusRepositorySnapshot,
        nowEpochMillis: Long = timeProvider.nowEpochMillis()
    ) {
        val sessionUiState = snapshot.activeSession?.let { session ->
            val remaining = remainingMillis(session.endsAtEpochMillis, nowEpochMillis)
            if (remaining == 0L) {
                FocusSessionUiState.Loading
            } else {
                val category = snapshot.categories.first { it.id == session.categoryId }
                FocusSessionUiState.Running(
                    session = session,
                    category = category,
                    remainingMillis = remaining,
                    remainingSeconds = remaining.toWholeSecondsRoundedUp()
                )
            }
        } ?: completionFeedback?.let(FocusSessionUiState::JustCompleted)
            ?: FocusSessionUiState.Idle

        mutableUiState.value = FocusUiState(
            categories = snapshot.categories,
            selectedCategoryId = snapshot.selectedCategoryId,
            selectedDurationMinutes = snapshot.selectedDurationMinutes,
            xp = snapshot.progress.xp,
            completedSessions = snapshot.progress.completedSessions,
            session = sessionUiState
        )

    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        tickerSessionId = null
    }
}

private fun Long.toWholeSecondsRoundedUp(): Long =
    if (this == 0L) 0L else ((this - 1L) / 1_000L) + 1L
