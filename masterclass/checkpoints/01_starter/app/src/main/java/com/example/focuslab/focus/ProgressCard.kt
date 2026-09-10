package com.example.focuslab.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Suppress("UNUSED_PARAMETER")
@Composable
fun ProgressCard(
    xp: Int,
    level: Int,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("progress_card_stub"),
        contentAlignment = Alignment.Center
    ) {
        // TODO(STUDENT): Replace this placeholder with your progress card.
        Text("Карточка прогресса")
    }
}
