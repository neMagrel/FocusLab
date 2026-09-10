package com.example.focuslab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focuslab.R
import com.example.focuslab.focus.ConfigureCategoriesButton
import com.example.focuslab.focus.FocusRoute
import com.example.focuslab.focus.FocusUiActions
import com.example.focuslab.focus.FocusUiState
import com.example.focuslab.ui.theme.FocusLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusLabTheme {
                FocusRoute { uiState, actions ->
                    FocusLabShell(uiState = uiState, actions = actions)
                }
            }
        }
    }
}

@Composable
private fun FocusLabShell(
    uiState: FocusUiState,
    actions: FocusUiActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
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
        uiState.categories.forEach { category ->
            Text(text = "${category.emoji} ${category.title}")
        }
        ConfigureCategoriesButton(
            onClick = actions.onManageCategories,
            enabled = uiState.selectionEnabled,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusLabShellPreview() {
    FocusLabTheme {
        FocusLabShell(
            uiState = FocusUiState(),
            actions = FocusUiActions({}, {}, {}, {})
        )
    }
}
