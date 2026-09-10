package com.example.focuslab.focus.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.focuslab.focus.model.DefaultFocusCategories
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.FocusProgress
import com.example.focuslab.focus.model.isSupportedDuration
import com.example.focuslab.focus.model.normalizeCategoryTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal const val DEFAULT_DURATION_MINUTES = 25

internal object FocusPreferenceKeys {
    val categories = stringPreferencesKey("categories")
    val selectedCategoryId = stringPreferencesKey("selected_category_id")
    val selectedDurationMinutes = intPreferencesKey("selected_duration_minutes")
    val xp = intPreferencesKey("xp")
    val completedSessions = intPreferencesKey("completed_sessions")
    val categoriesInitialized = booleanPreferencesKey("categories_initialized")
}

class DataStoreFocusRepository(
    private val dataStore: DataStore<Preferences>
) : FocusRepository {
    override val snapshots: Flow<FocusRepositorySnapshot> = flow {
        currentSnapshot()
        emitAll(
            dataStore.data
                .map(::normalizedSnapshot)
                .distinctUntilChanged()
        )
    }

    override suspend fun currentSnapshot(): FocusRepositorySnapshot {
        lateinit var snapshot: FocusRepositorySnapshot
        dataStore.edit { preferences ->
            snapshot = normalizeAndRepair(preferences)
        }
        return snapshot
    }

    override suspend fun addCategory(category: FocusCategory): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            if (category.isValidFor(current.categories)) {
                preferences[FocusPreferenceKeys.categories] =
                    FocusCodec.encode(current.categories + category)
                changed = true
            }
        }
        return changed
    }

    override suspend fun updateCategory(category: FocusCategory): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            val index = current.categories.indexOfFirst { it.id == category.id }
            val otherCategories = current.categories.filterNot { it.id == category.id }
            if (index >= 0 && category.isValidFor(otherCategories)) {
                val updated = current.categories.toMutableList().apply {
                    this[index] = category
                }
                preferences[FocusPreferenceKeys.categories] = FocusCodec.encode(updated)
                changed = true
            }
        }
        return changed
    }

    override suspend fun deleteCategory(categoryId: String): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            val remaining = current.categories.filterNot { it.id == categoryId }
            if (remaining.size == current.categories.size - 1 && remaining.isNotEmpty()) {
                preferences[FocusPreferenceKeys.categories] = FocusCodec.encode(remaining)
                if (current.selectedCategoryId == categoryId) {
                    preferences[FocusPreferenceKeys.selectedCategoryId] = remaining.first().id
                }
                changed = true
            }
        }
        return changed
    }

    override suspend fun selectCategory(categoryId: String): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            if (current.categories.any { it.id == categoryId }) {
                preferences[FocusPreferenceKeys.selectedCategoryId] = categoryId
                changed = true
            }
        }
        return changed
    }

    override suspend fun selectDuration(durationMinutes: Int): Boolean {
        if (!isSupportedDuration(durationMinutes)) return false

        dataStore.edit { preferences ->
            normalizeAndRepair(preferences)
            preferences[FocusPreferenceKeys.selectedDurationMinutes] = durationMinutes
        }
        return true
    }

    override suspend fun updateProgress(progress: FocusProgress): Boolean {
        if (progress.xp < 0 || progress.completedSessions < 0) return false

        dataStore.edit { preferences ->
            normalizeAndRepair(preferences)
            preferences[FocusPreferenceKeys.xp] = progress.xp
            preferences[FocusPreferenceKeys.completedSessions] = progress.completedSessions
        }
        return true
    }

    private fun normalizeAndRepair(
        preferences: MutablePreferences
    ): FocusRepositorySnapshot {
        val snapshot = normalizedSnapshot(preferences)
        preferences[FocusPreferenceKeys.categories] = FocusCodec.encode(snapshot.categories)
        preferences[FocusPreferenceKeys.selectedCategoryId] = snapshot.selectedCategoryId
        preferences[FocusPreferenceKeys.selectedDurationMinutes] =
            snapshot.selectedDurationMinutes
        preferences[FocusPreferenceKeys.xp] = snapshot.progress.xp
        preferences[FocusPreferenceKeys.completedSessions] =
            snapshot.progress.completedSessions
        preferences[FocusPreferenceKeys.categoriesInitialized] = true
        return snapshot
    }

    private fun normalizedSnapshot(
        preferences: Preferences
    ): FocusRepositorySnapshot {
        if (preferences[FocusPreferenceKeys.categoriesInitialized] != true) {
            return defaultSnapshot()
        }

        val categories = preferences[FocusPreferenceKeys.categories]
            ?.let(FocusCodec::decode)
            ?.takeIf(::isValidCategoryList)
            ?: DefaultFocusCategories
        val selectedCategoryId = preferences[FocusPreferenceKeys.selectedCategoryId]
            ?.takeIf { selectedId -> categories.any { it.id == selectedId } }
            ?: categories.first().id
        val selectedDurationMinutes = preferences[FocusPreferenceKeys.selectedDurationMinutes]
            ?.takeIf(::isSupportedDuration)
            ?: DEFAULT_DURATION_MINUTES

        return FocusRepositorySnapshot(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            selectedDurationMinutes = selectedDurationMinutes,
            progress = FocusProgress(
                xp = (preferences[FocusPreferenceKeys.xp] ?: 0).coerceAtLeast(0),
                completedSessions = (preferences[FocusPreferenceKeys.completedSessions] ?: 0)
                    .coerceAtLeast(0)
            )
        )
    }

    private fun defaultSnapshot(): FocusRepositorySnapshot = FocusRepositorySnapshot(
        categories = DefaultFocusCategories,
        selectedCategoryId = DefaultFocusCategories.first().id,
        selectedDurationMinutes = DEFAULT_DURATION_MINUTES,
        progress = FocusProgress(xp = 0, completedSessions = 0)
    )

    private fun FocusCategory.isValidFor(existing: List<FocusCategory>): Boolean {
        val normalizedTitle = normalizeCategoryTitle(title)
        return id.isNotBlank() &&
            emoji.isNotBlank() &&
            normalizedTitle.isNotEmpty() &&
            existing.none { it.id == id } &&
            existing.none { normalizeCategoryTitle(it.title) == normalizedTitle }
    }

    private fun isValidCategoryList(categories: List<FocusCategory>): Boolean {
        if (categories.isEmpty()) return false
        val ids = mutableSetOf<String>()
        val titles = mutableSetOf<String>()
        return categories.all { category ->
            val normalizedTitle = normalizeCategoryTitle(category.title)
            category.id.isNotBlank() &&
                category.emoji.isNotBlank() &&
                normalizedTitle.isNotEmpty() &&
                ids.add(category.id) &&
                titles.add(normalizedTitle)
        }
    }
}
