package com.example.focuslab.focus.data

import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.FocusProgress
import kotlinx.coroutines.flow.Flow

data class FocusRepositorySnapshot(
    val categories: List<FocusCategory>,
    val selectedCategoryId: String,
    val selectedDurationMinutes: Int,
    val progress: FocusProgress,
    val activeSession: ActiveFocusSession? = null
)

interface FocusRepository {
    val snapshots: Flow<FocusRepositorySnapshot>

    suspend fun currentSnapshot(): FocusRepositorySnapshot

    suspend fun addCategory(category: FocusCategory): Boolean

    suspend fun updateCategory(category: FocusCategory): Boolean

    suspend fun deleteCategory(categoryId: String): Boolean

    suspend fun selectCategory(categoryId: String): Boolean

    suspend fun selectDuration(durationMinutes: Int): Boolean

    suspend fun updateProgress(progress: FocusProgress): Boolean

    suspend fun startFocusSession(): Boolean

    suspend fun cancelSessionIfActive(expectedSessionId: String): Boolean

    suspend fun completeSessionIfActive(expectedSessionId: String): Boolean
}
