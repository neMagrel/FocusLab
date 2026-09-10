package com.example.focuslab.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val currentCategory = uiState.categories.firstOrNull {
        it.id == uiState.selectedCategoryId
    } ?: uiState.categories.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.project_shell_status),
            style = MaterialTheme.typography.bodyLarge
        )
        currentCategory?.let { category ->
            Text(
                text = "${category.emoji} ${category.title}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // STUDENT ZONE START
        // TODO(STUDENT): Assemble the prepared Focus Lab components here.
        // TODO(STUDENT): Add ProgressCard and show the progress values.
        // TODO(STUDENT): Wire category and duration selection callbacks.
        // TODO(STUDENT): Wire FocusPrimaryButton to onStartFocus.
        // STUDENT ZONE END
    }
}
