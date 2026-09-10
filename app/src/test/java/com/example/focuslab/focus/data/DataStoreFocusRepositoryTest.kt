package com.example.focuslab.focus.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import com.example.focuslab.focus.model.DefaultFocusCategories
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.FocusProgress
import com.example.focuslab.focus.model.SUPPORTED_DURATIONS
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class DataStoreFocusRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `clean store seeds a valid initial snapshot in one initialization`() = runBlocking {
        val handle = createStore()
        val repository = DataStoreFocusRepository(handle.dataStore)

        val snapshot = repository.snapshots.first()
        val preferences = handle.dataStore.data.first()

        assertEquals(DefaultFocusCategories, snapshot.categories)
        assertEquals(DefaultFocusCategories.first().id, snapshot.selectedCategoryId)
        assertEquals(DEFAULT_DURATION_MINUTES, snapshot.selectedDurationMinutes)
        assertTrue(snapshot.selectedDurationMinutes in SUPPORTED_DURATIONS)
        assertEquals(FocusProgress(xp = 0, completedSessions = 0), snapshot.progress)
        assertEquals(true, preferences[FocusPreferenceKeys.categoriesInitialized])
        close(handle)
    }

    @Test
    fun `defaults seed once and edited categories survive repository recreation`() = runBlocking {
        val file = newStoreFile()
        val firstHandle = createStore(file)
        val firstRepository = DataStoreFocusRepository(firstHandle.dataStore)
        firstRepository.currentSnapshot()
        assertTrue(
            firstRepository.updateCategory(
                DefaultFocusCategories.first().copy(emoji = "🎓", title = "Моя учёба")
            )
        )
        close(firstHandle)

        val secondHandle = createStore(file)
        val reopened = DataStoreFocusRepository(secondHandle.dataStore).currentSnapshot()

        assertEquals("study", reopened.categories.first().id)
        assertEquals("🎓", reopened.categories.first().emoji)
        assertEquals("Моя учёба", reopened.categories.first().title)
        assertEquals(4, reopened.categories.size)
        close(secondHandle)
    }

    @Test
    fun `valid selection and every supported duration persist after reopen`() = runBlocking {
        SUPPORTED_DURATIONS.forEach { duration ->
            val file = newStoreFile()
            val firstHandle = createStore(file)
            val firstRepository = DataStoreFocusRepository(firstHandle.dataStore)
            firstRepository.currentSnapshot()
            assertTrue(firstRepository.selectCategory("reading"))
            assertTrue(firstRepository.selectDuration(duration))
            close(firstHandle)

            val secondHandle = createStore(file)
            val reopened = DataStoreFocusRepository(secondHandle.dataStore).currentSnapshot()
            assertEquals("reading", reopened.selectedCategoryId)
            assertEquals(duration, reopened.selectedDurationMinutes)
            close(secondHandle)
        }
    }

    @Test
    fun `invalid selection and duration mutations are safe no-ops`() = runBlocking {
        val handle = createStore()
        val repository = DataStoreFocusRepository(handle.dataStore)
        val initial = repository.currentSnapshot()

        assertFalse(repository.selectCategory("missing"))
        assertFalse(repository.selectDuration(10))
        assertEquals(initial, repository.currentSnapshot())
        close(handle)
    }

    @Test
    fun `category add edit and selected delete preserve persistence and order`() = runBlocking {
        val file = newStoreFile()
        val firstHandle = createStore(file)
        val firstRepository = DataStoreFocusRepository(firstHandle.dataStore)
        firstRepository.currentSnapshot()
        val custom = FocusCategory(id = "music", emoji = "🎵", title = "Музыка")

        assertTrue(firstRepository.addCategory(custom))
        assertTrue(firstRepository.updateCategory(custom.copy(emoji = "🎹", title = "Пианино")))
        close(firstHandle)

        val secondHandle = createStore(file)
        val secondRepository = DataStoreFocusRepository(secondHandle.dataStore)
        val afterEditReopen = secondRepository.currentSnapshot()
        assertEquals(
            DefaultFocusCategories.map(FocusCategory::id) + "music",
            afterEditReopen.categories.map(FocusCategory::id)
        )
        assertEquals("🎹", afterEditReopen.categories.last().emoji)
        assertEquals("Пианино", afterEditReopen.categories.last().title)
        assertTrue(secondRepository.selectCategory(custom.id))
        assertTrue(secondRepository.deleteCategory(custom.id))
        close(secondHandle)

        val thirdHandle = createStore(file)
        val afterDeleteReopen = DataStoreFocusRepository(thirdHandle.dataStore).currentSnapshot()
        assertEquals(DefaultFocusCategories, afterDeleteReopen.categories)
        assertEquals(DefaultFocusCategories.first().id, afterDeleteReopen.selectedCategoryId)
        close(thirdHandle)
    }

    @Test
    fun `category edit keeps stable id and list position`() = runBlocking {
        val handle = createStore()
        val repository = DataStoreFocusRepository(handle.dataStore)
        repository.currentSnapshot()
        val originalOrder = DefaultFocusCategories.map(FocusCategory::id)

        assertTrue(
            repository.updateCategory(
                FocusCategory(id = "code", emoji = "⌨️", title = "Разработка")
            )
        )
        val snapshot = repository.currentSnapshot()

        assertEquals(originalOrder, snapshot.categories.map(FocusCategory::id))
        assertEquals("⌨️", snapshot.categories[1].emoji)
        assertEquals("Разработка", snapshot.categories[1].title)
        close(handle)
    }

    @Test
    fun `missing mutations duplicate values and deleting last category are protected`() = runBlocking {
        val handle = createStore()
        val repository = DataStoreFocusRepository(handle.dataStore)
        repository.currentSnapshot()

        assertFalse(repository.updateCategory(FocusCategory("missing", "🎵", "Музыка")))
        assertFalse(repository.deleteCategory("missing"))
        assertFalse(repository.addCategory(FocusCategory("code", "🎵", "Музыка")))
        assertFalse(repository.addCategory(FocusCategory("music", "🎵", "  КОД ")))

        DefaultFocusCategories.dropLast(1).forEach { category ->
            assertTrue(repository.deleteCategory(category.id))
        }
        val onlyCategory = repository.currentSnapshot().categories.single()
        assertFalse(repository.deleteCategory(onlyCategory.id))
        assertEquals(listOf(onlyCategory), repository.currentSnapshot().categories)
        close(handle)
    }

    @Test
    fun `progress persists and negative public update is rejected`() = runBlocking {
        val file = newStoreFile()
        val firstHandle = createStore(file)
        val repository = DataStoreFocusRepository(firstHandle.dataStore)
        repository.currentSnapshot()

        assertTrue(repository.updateProgress(FocusProgress(xp = 230, completedSessions = 7)))
        assertFalse(repository.updateProgress(FocusProgress(xp = -1, completedSessions = 8)))
        close(firstHandle)

        val secondHandle = createStore(file)
        val reopened = DataStoreFocusRepository(secondHandle.dataStore).currentSnapshot()
        assertEquals(FocusProgress(xp = 230, completedSessions = 7), reopened.progress)
        close(secondHandle)
    }

    @Test
    fun `malformed categories recover to defaults and repair selection`() = runBlocking {
        val handle = createStoreWithPreferences {
            it[FocusPreferenceKeys.categoriesInitialized] = true
            it[FocusPreferenceKeys.categories] = "malformed"
            it[FocusPreferenceKeys.selectedCategoryId] = "missing"
        }
        val snapshot = DataStoreFocusRepository(handle.dataStore).currentSnapshot()
        val repaired = handle.dataStore.data.first()

        assertEquals(DefaultFocusCategories, snapshot.categories)
        assertEquals(DefaultFocusCategories.first().id, snapshot.selectedCategoryId)
        assertEquals(FocusCodec.encode(DefaultFocusCategories), repaired[FocusPreferenceKeys.categories])
        assertEquals(DefaultFocusCategories.first().id, repaired[FocusPreferenceKeys.selectedCategoryId])
        close(handle)
    }

    @Test
    fun `empty categories recover to defaults`() = runBlocking {
        val handle = createStoreWithPreferences {
            it[FocusPreferenceKeys.categoriesInitialized] = true
            it[FocusPreferenceKeys.categories] = FocusCodec.encode(emptyList())
        }

        val snapshot = DataStoreFocusRepository(handle.dataStore).currentSnapshot()

        assertEquals(DefaultFocusCategories, snapshot.categories)
        assertEquals(DefaultFocusCategories.first().id, snapshot.selectedCategoryId)
        close(handle)
    }

    @Test
    fun `missing initialized categories value recovers to defaults`() = runBlocking {
        val handle = createStoreWithPreferences {
            it[FocusPreferenceKeys.categoriesInitialized] = true
            it[FocusPreferenceKeys.selectedCategoryId] = "missing"
        }

        val snapshot = DataStoreFocusRepository(handle.dataStore).currentSnapshot()

        assertEquals(DefaultFocusCategories, snapshot.categories)
        assertEquals(DefaultFocusCategories.first().id, snapshot.selectedCategoryId)
        close(handle)
    }

    @Test
    fun `invalid durable scalar values are repaired`() = runBlocking {
        val customCategories = listOf(
            FocusCategory(id = "a", emoji = "🅰️", title = "А"),
            FocusCategory(id = "b", emoji = "🅱️", title = "Б")
        )
        val handle = createStoreWithPreferences {
            it[FocusPreferenceKeys.categoriesInitialized] = true
            it[FocusPreferenceKeys.categories] = FocusCodec.encode(customCategories)
            it[FocusPreferenceKeys.selectedCategoryId] = "missing"
            it[FocusPreferenceKeys.selectedDurationMinutes] = 10
            it[FocusPreferenceKeys.xp] = -100
            it[FocusPreferenceKeys.completedSessions] = -2
        }

        val snapshot = DataStoreFocusRepository(handle.dataStore).currentSnapshot()
        val repaired = handle.dataStore.data.first()

        assertEquals(customCategories.first().id, snapshot.selectedCategoryId)
        assertEquals(DEFAULT_DURATION_MINUTES, snapshot.selectedDurationMinutes)
        assertEquals(FocusProgress(0, 0), snapshot.progress)
        assertEquals(customCategories.first().id, repaired[FocusPreferenceKeys.selectedCategoryId])
        assertEquals(DEFAULT_DURATION_MINUTES, repaired[FocusPreferenceKeys.selectedDurationMinutes])
        assertEquals(0, repaired[FocusPreferenceKeys.xp])
        assertEquals(0, repaired[FocusPreferenceKeys.completedSessions])
        close(handle)
    }

    private suspend fun createStoreWithPreferences(
        block: (MutablePreferences) -> Unit
    ): StoreHandle {
        val handle = createStore()
        handle.dataStore.edit(block)
        return handle
    }

    private fun createStore(file: File = newStoreFile()): StoreHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { file.toOkioPath() }
            ),
            scope = scope,
        )
        return StoreHandle(dataStore = dataStore, scope = scope)
    }

    private fun newStoreFile(): File =
        File(temporaryFolder.newFolder(), "focus.preferences_pb")

    private suspend fun close(handle: StoreHandle) {
        handle.scope.coroutineContext.job.cancelAndJoin()
    }

    private data class StoreHandle(
        val dataStore: DataStore<Preferences>,
        val scope: CoroutineScope
    )
}
