package com.example.focuslab.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.focuslab.R

@Suppress("UNUSED_PARAMETER")
@Composable
fun FocusScreen(
    uiState: FocusUiState,
    onCategorySelected: (String) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onStartFocus: () -> Unit,
    onManageCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        // STUDENT ZONE START
        FocusHeader(appName = stringResource(R.string.app_name))
        CategorySelector(
            categories = uiState.categories,
            selectedId = null,
            onSelected = {}
        )
        DurationSelector(
            durations = uiState.supportedDurations,
            selectedDuration = null,
            onSelected = {}
        )
        TimerCard(sessionUiState = uiState.session)
        ProgressCard(
            xp = uiState.xp,
            level = uiState.level,
            completedSessions = uiState.completedSessions
        )

        // TODO(STUDENT): Wire category and duration selection callbacks.
        // TODO(STUDENT): Wire FocusPrimaryButton to onStartFocus.
        // STUDENT ZONE END
    }
}
