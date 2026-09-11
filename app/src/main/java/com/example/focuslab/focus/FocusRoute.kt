package com.example.focuslab.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focuslab.focus.data.DataStoreFocusRepository
import com.example.focuslab.focus.data.FocusRepository
import com.example.focuslab.focus.data.focusDataStore
import com.example.focuslab.focus.model.SystemTimeProvider
import com.example.focuslab.focus.model.TimeProvider

data class FocusUiActions(
    val onCategorySelected: (String) -> Unit,
    val onDurationSelected: (Int) -> Unit,
    val onStartFocus: () -> Unit,
    val onCancelFocus: () -> Unit,
    val onManageCategories: () -> Unit
)

@Composable
fun FocusRoute() {
    FocusRoute { uiState, actions ->
        FocusScreen(
            uiState = uiState,
            onCategorySelected = actions.onCategorySelected,
            onDurationSelected = actions.onDurationSelected,
            onStartFocus = actions.onStartFocus,
            onCancelFocus = actions.onCancelFocus,
            onManageCategories = actions.onManageCategories
        )
    }
}

@Composable
internal fun FocusRoute(
    content: @Composable (FocusUiState, FocusUiActions) -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val factory = remember(applicationContext) {
        FocusViewModelFactory(
            repository = DataStoreFocusRepository(applicationContext.focusDataStore),
            timeProvider = SystemTimeProvider
        )
    }
    val viewModel: FocusViewModel = viewModel(factory = factory)
    FocusRoute(viewModel = viewModel, content = content)
}

@Composable
internal fun FocusRoute(
    viewModel: FocusViewModel,
    content: @Composable (FocusUiState, FocusUiActions) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryEditorOpen by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val actions = remember(viewModel) {
        FocusUiActions(
            onCategorySelected = viewModel::onCategorySelected,
            onDurationSelected = viewModel::onDurationSelected,
            onStartFocus = viewModel::onStartFocus,
            onCancelFocus = viewModel::onCancelFocus,
            onManageCategories = { categoryEditorOpen = true }
        )
    }
    content(uiState, actions)
    if (categoryEditorOpen) {
        CategoryEditor(
            categories = uiState.categories,
            enabled = uiState.selectionEnabled,
            onAddCategory = viewModel::onAddCategory,
            onEditCategory = viewModel::onEditCategory,
            onDeleteCategory = viewModel::onDeleteCategory,
            onDismiss = { categoryEditorOpen = false }
        )
    }
}

private class FocusViewModelFactory(
    private val repository: FocusRepository,
    private val timeProvider: TimeProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FocusViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return FocusViewModel(repository, timeProvider) as T
    }
}
