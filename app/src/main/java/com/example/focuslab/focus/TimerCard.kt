package com.example.focuslab.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun TimerCard(
    sessionUiState: FocusSessionUiState,
    modifier: Modifier = Modifier
) {
    val containerColor = when (sessionUiState) {
        is FocusSessionUiState.JustCompleted -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timer_card"),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        when (sessionUiState) {
            FocusSessionUiState.Loading -> LoadingTimerContent()
            FocusSessionUiState.Idle -> IdleTimerContent()
            is FocusSessionUiState.Running -> RunningTimerContent(sessionUiState)
            is FocusSessionUiState.JustCompleted -> CompletedTimerContent(sessionUiState)
        }
    }
}

@Composable
private fun LoadingTimerContent() {
    Row(
        modifier = Modifier.padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
        Text("Подготавливаем фокус…", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun IdleTimerContent() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Готовы начать?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Выберите категорию и время для фокуса.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunningTimerContent(state: FocusSessionUiState.Running) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${state.category.emoji} ${state.category.title}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatRemainingTime(state.remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("timer_countdown")
        )
        Text(
            text = "Фокус продолжается",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompletedTimerContent(state: FocusSessionUiState.JustCompleted) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Фокус завершён",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${state.feedback.category.emoji} ${state.feedback.category.title} · +${state.feedback.rewardXp} XP",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatRemainingTime(remainingSeconds: Long): String {
    val safeSeconds = remainingSeconds.coerceAtLeast(0L)
    val minutes = safeSeconds / 60L
    val seconds = safeSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}
