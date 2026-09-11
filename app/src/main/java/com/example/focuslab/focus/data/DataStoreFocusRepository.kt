package com.example.focuslab.focus.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.CURATED_CATEGORY_EMOJIS
import com.example.focuslab.focus.model.DefaultFocusCategories
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.FocusProgress
import com.example.focuslab.focus.model.SystemTimeProvider
import com.example.focuslab.focus.model.TimeProvider
import com.example.focuslab.focus.model.isSupportedDuration
import com.example.focuslab.focus.model.normalizeCategoryTitle
import com.example.focuslab.focus.model.rewardForDuration
import com.example.focuslab.focus.model.validateCategoryDraft
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal const val DEFAULT_DURATION_MINUTES = 25
private const val MILLIS_PER_MINUTE = 60_000L

internal object FocusPreferenceKeys {
    val categories = stringPreferencesKey("categories")
    val selectedCategoryId = stringPreferencesKey("selected_category_id")
    val selectedDurationMinutes = intPreferencesKey("selected_duration_minutes")
    val xp = intPreferencesKey("xp")
    val completedSessions = intPreferencesKey("completed_sessions")
    val categoriesInitialized = booleanPreferencesKey("categories_initialized")
    val activeSession = stringPreferencesKey("active_session")
}

class DataStoreFocusRepository(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: TimeProvider = SystemTimeProvider
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
            val sanitized = category.copy(title = category.title.trim())
            if (sanitized.isValidFor(current.categories)) {
                preferences[FocusPreferenceKeys.categories] =
                    FocusCodec.encode(current.categories + sanitized)
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
            val sanitized = category.copy(title = category.title.trim())
            if (index >= 0 && sanitized.isValidFor(otherCategories)) {
                val updated = current.categories.toMutableList().apply {
                    this[index] = sanitized
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

    override suspend fun startFocusSession(): Boolean {
        var started = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            val categoryExists = current.categories.any { it.id == current.selectedCategoryId }
            if (
                current.activeSession == null &&
                categoryExists &&
                isSupportedDuration(current.selectedDurationMinutes)
            ) {
                val startedAtEpochMillis = timeProvider.nowEpochMillis()
                val session = ActiveFocusSession(
                    id = UUID.randomUUID().toString(),
                    categoryId = current.selectedCategoryId,
                    durationMinutes = current.selectedDurationMinutes,
                    startedAtEpochMillis = startedAtEpochMillis,
                    endsAtEpochMillis = startedAtEpochMillis +
                        current.selectedDurationMinutes * MILLIS_PER_MINUTE
                )
                preferences[FocusPreferenceKeys.activeSession] =
                    FocusCodec.encodeActiveSession(session)
                started = true
            }
        }
        return started
    }

    override suspend fun cancelSessionIfActive(expectedSessionId: String): Boolean {
        var cancelled = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            if (current.activeSession?.id == expectedSessionId) {
                preferences.remove(FocusPreferenceKeys.activeSession)
                cancelled = true
            }
        }
        return cancelled
    }

    override suspend fun completeSessionIfActive(expectedSessionId: String): Boolean {
        var completed = false
        dataStore.edit { preferences ->
            val current = normalizeAndRepair(preferences)
            val session = current.activeSession
            if (session != null && session.id == expectedSessionId) {
                preferences[FocusPreferenceKeys.xp] =
                    current.progress.xp + rewardForDuration(session.durationMinutes)
                preferences[FocusPreferenceKeys.completedSessions] =
                    current.progress.completedSessions + 1
                preferences.remove(FocusPreferenceKeys.activeSession)
                completed = true
            }
        }
        return completed
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
        snapshot.activeSession?.let { session ->
            preferences[FocusPreferenceKeys.activeSession] =
                FocusCodec.encodeActiveSession(session)
        } ?: preferences.remove(FocusPreferenceKeys.activeSession)
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
        val activeSession = preferences[FocusPreferenceKeys.activeSession]
            ?.let(FocusCodec::decodeActiveSession)
            ?.takeIf { it.isValidFor(categories) }

        return FocusRepositorySnapshot(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            selectedDurationMinutes = selectedDurationMinutes,
            progress = FocusProgress(
                xp = (preferences[FocusPreferenceKeys.xp] ?: 0).coerceAtLeast(0),
                completedSessions = (preferences[FocusPreferenceKeys.completedSessions] ?: 0)
                    .coerceAtLeast(0)
            ),
            activeSession = activeSession
        )
    }

    private fun defaultSnapshot(): FocusRepositorySnapshot = FocusRepositorySnapshot(
        categories = DefaultFocusCategories,
        selectedCategoryId = DefaultFocusCategories.first().id,
        selectedDurationMinutes = DEFAULT_DURATION_MINUTES,
        progress = FocusProgress(xp = 0, completedSessions = 0),
        activeSession = null
    )

    private fun ActiveFocusSession.isValidFor(categories: List<FocusCategory>): Boolean =
        id.isNotBlank() &&
            categories.any { it.id == categoryId } &&
            isSupportedDuration(durationMinutes) &&
            startedAtEpochMillis >= 0L &&
            endsAtEpochMillis == startedAtEpochMillis + durationMinutes * MILLIS_PER_MINUTE

    private fun FocusCategory.isValidFor(existing: List<FocusCategory>): Boolean {
        return id.isNotBlank() &&
            existing.none { it.id == id } &&
            emoji in CURATED_CATEGORY_EMOJIS &&
            validateCategoryDraft(
                title = title,
                emoji = emoji,
                existingCategories = existing
            ) == null
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
