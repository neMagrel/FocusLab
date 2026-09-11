package com.example.focuslab.focus

import androidx.lifecycle.viewModelScope
import com.example.focuslab.focus.data.FocusRepository
import com.example.focuslab.focus.data.FocusRepositorySnapshot
import com.example.focuslab.focus.model.ActiveFocusSession
import com.example.focuslab.focus.model.DefaultFocusCategories
import com.example.focuslab.focus.model.FocusCategory
import com.example.focuslab.focus.model.FocusProgress
import com.example.focuslab.focus.model.TimeProvider
import com.example.focuslab.focus.model.isSupportedDuration
import com.example.focuslab.focus.model.rewardForDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModels = mutableListOf<FocusViewModel>()

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
    }

    @Test
    fun `initial repository snapshot projects durable and derived idle state`() =
        runViewModelTest {
            val clock = FakeTimeProvider(nowEpochMillis = 5_000L)
            val repository = FakeFocusRepository(
                initialSnapshot = snapshot(
                    selectedCategoryId = "reading",
                    selectedDurationMinutes = 15,
                    progress = FocusProgress(xp = 140, completedSessions = 6)
                ),
                timeProvider = clock
            )

            val viewModel = createViewModel(repository, clock)
            runCurrent()
            val state = viewModel.uiState.value

            assertEquals(DefaultFocusCategories, state.categories)
            assertEquals("reading", state.selectedCategoryId)
            assertEquals(listOf(1, 5, 15, 25), state.supportedDurations)
            assertEquals(15, state.selectedDurationMinutes)
            assertEquals(140, state.xp)
            assertEquals(6, state.completedSessions)
            assertEquals(2, state.level)
            assertSame(FocusSessionUiState.Idle, state.session)
            assertTrue(state.selectionEnabled)
            assertTrue(state.canStart)
        }

    @Test
    fun `select category while idle updates through repository`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(snapshot(), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onCategorySelected("code")
            runCurrent()

            assertEquals(listOf("code"), repository.categorySelections)
            assertEquals("code", viewModel.uiState.value.selectedCategoryId)
        }

    @Test
    fun `select duration while idle updates through repository`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(snapshot(), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onDurationSelected(5)
            runCurrent()

            assertEquals(listOf(5), repository.durationSelections)
            assertEquals(5, viewModel.uiState.value.selectedDurationMinutes)
        }

    @Test
    fun `invalid category and unsupported duration do not corrupt state`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val initial = snapshot()
            val repository = FakeFocusRepository(initial, clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onCategorySelected("missing")
            viewModel.onDurationSelected(7)
            runCurrent()

            assertTrue(repository.categorySelections.isEmpty())
            assertTrue(repository.durationSelections.isEmpty())
            assertEquals(initial, repository.currentSnapshot())
        }

    @Test
    fun `category mutation events update UiState through repository`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(snapshot(selectedCategoryId = "code"), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onAddCategory("  Музыка  ", "🎵")
            runCurrent()
            val added = viewModel.uiState.value.categories.last()
            assertTrue(added.id.isNotBlank())
            assertEquals("Музыка", added.title)
            assertEquals("🎵", added.emoji)

            viewModel.onEditCategory(added.id, "  Пианино  ", "🎹")
            runCurrent()
            val edited = viewModel.uiState.value.categories.last()
            assertEquals(added.id, edited.id)
            assertEquals("Пианино", edited.title)
            assertEquals("🎹", edited.emoji)

            viewModel.onDeleteCategory("code")
            runCurrent()
            assertEquals("study", viewModel.uiState.value.selectedCategoryId)
            assertFalse(viewModel.uiState.value.categories.any { it.id == "code" })
        }

    @Test
    fun `missing category mutation events and mutations while running are safe no-ops`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(
                snapshot(activeSession = activeSession()),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onEditCategory("missing", "Музыка", "🎵")
            viewModel.onDeleteCategory("missing")
            viewModel.onAddCategory("Музыка", "🎵")
            runCurrent()

            assertTrue(repository.categoryMutations.isEmpty())
            assertEquals(DefaultFocusCategories, viewModel.uiState.value.categories)
        }

    @Test
    fun `selection events while running are ignored`() =
        runViewModelTest {
            val clock = FakeTimeProvider(1_000L)
            val session = activeSession(endsAtEpochMillis = 61_000L)
            val repository = FakeFocusRepository(snapshot(activeSession = session), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onCategorySelected("code")
            viewModel.onDurationSelected(5)
            runCurrent()

            assertTrue(repository.categorySelections.isEmpty())
            assertTrue(repository.durationSelections.isEmpty())
            assertEquals(session, repository.currentSnapshot().activeSession)
            assertFalse(viewModel.uiState.value.selectionEnabled)
        }

    @Test
    fun `start while idle creates and observes active session`() =
        runViewModelTest {
            val clock = FakeTimeProvider(50_000L)
            val repository = FakeFocusRepository(
                snapshot(selectedCategoryId = "code", selectedDurationMinutes = 1),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onStartFocus()
            runCurrent()

            val running = viewModel.uiState.value.session as FocusSessionUiState.Running
            assertEquals(1, repository.startCalls)
            assertEquals("code", running.session.categoryId)
            assertEquals(50_000L, running.session.startedAtEpochMillis)
            assertEquals(110_000L, running.session.endsAtEpochMillis)
            assertEquals(60L, running.remainingSeconds)
        }

    @Test
    fun `repeated start while running does not replace session`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(snapshot(selectedDurationMinutes = 1), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onStartFocus()
            runCurrent()
            val firstSession = repository.currentSnapshot().activeSession
            viewModel.onStartFocus()
            runCurrent()

            assertEquals(1, repository.startCalls)
            assertEquals(firstSession, repository.currentSnapshot().activeSession)
        }

    @Test
    fun `cancel running session clears it without changing progress`() =
        runViewModelTest {
            val clock = FakeTimeProvider(10_000L)
            val session = activeSession(endsAtEpochMillis = 70_000L)
            val progress = FocusProgress(xp = 70, completedSessions = 4)
            val repository = FakeFocusRepository(
                snapshot(
                    selectedCategoryId = "reading",
                    selectedDurationMinutes = 1,
                    progress = progress,
                    activeSession = session
                ),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onCancelFocus()
            runCurrent()

            assertEquals(listOf(session.id), repository.cancellationIds)
            assertNull(repository.currentSnapshot().activeSession)
            assertEquals(progress, repository.currentSnapshot().progress)
            assertSame(FocusSessionUiState.Idle, viewModel.uiState.value.session)
            assertTrue(viewModel.uiState.value.canStart)
        }

    @Test
    fun `cancel while idle is a no-op`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(snapshot(), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            viewModel.onCancelFocus()
            runCurrent()

            assertTrue(repository.cancellationIds.isEmpty())
            assertSame(FocusSessionUiState.Idle, viewModel.uiState.value.session)
        }

    @Test
    fun `reconcile active session before end recovers running state`() =
        runViewModelTest {
            val clock = FakeTimeProvider(20_000L)
            val session = activeSession(
                categoryId = "reading",
                durationMinutes = 5,
                startedAtEpochMillis = 0L,
                endsAtEpochMillis = 300_000L
            )
            val repository = FakeFocusRepository(snapshot(activeSession = session), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            val running = viewModel.uiState.value.session as FocusSessionUiState.Running
            assertEquals(session, running.session)
            assertEquals("reading", running.category.id)
            assertEquals(280_000L, running.remainingMillis)
            assertEquals(280L, running.remainingSeconds)
            assertEquals(0, repository.completionCalls)
        }

    @Test
    fun `remaining seconds rounds upward and never becomes negative`() =
        runViewModelTest {
            val clock = FakeTimeProvider(8_499L)
            val repository = FakeFocusRepository(
                snapshot(activeSession = activeSession(endsAtEpochMillis = 10_000L)),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()
            assertEquals(2L, viewModel.uiState.value.remainingSeconds)

            clock.nowEpochMillis = 9_000L
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(1L, viewModel.uiState.value.remainingSeconds)

            clock.nowEpochMillis = 10_001L
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(0L, viewModel.uiState.value.remainingMillis)
            assertEquals(0L, viewModel.uiState.value.remainingSeconds)
        }

    @Test
    fun `ticker advances derived remaining without persistence writes`() =
        runViewModelTest {
            val clock = FakeTimeProvider(0L)
            val repository = FakeFocusRepository(
                snapshot(activeSession = activeSession(endsAtEpochMillis = 5_000L)),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()
            val writesBeforeTick = repository.durableWrites

            clock.nowEpochMillis = 1_200L
            advanceTimeBy(1_000L)
            runCurrent()

            assertEquals(3_800L, viewModel.uiState.value.remainingMillis)
            assertEquals(4L, viewModel.uiState.value.remainingSeconds)
            assertEquals(writesBeforeTick, repository.durableWrites)
        }

    @Test
    fun `reconcile expired session completes through repository`() =
        runViewModelTest {
            val clock = FakeTimeProvider(60_001L)
            val session = activeSession(endsAtEpochMillis = 60_000L)
            val repository = FakeFocusRepository(snapshot(activeSession = session), clock)
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            val state = viewModel.uiState.value
            val completed = state.session as FocusSessionUiState.JustCompleted
            assertEquals(listOf(session.id), repository.completionIds)
            assertNull(repository.currentSnapshot().activeSession)
            assertEquals(FocusProgress(xp = 10, completedSessions = 1), repository.currentSnapshot().progress)
            assertEquals(10, completed.feedback.rewardXp)
            assertEquals(10, state.xp)
            assertEquals(1, state.completedSessions)
        }

    @Test
    fun `duplicate completion paths reward once and only winner shows feedback`() =
        runViewModelTest {
            val clock = FakeTimeProvider(60_001L)
            val repository = FakeFocusRepository(
                snapshot(activeSession = activeSession(endsAtEpochMillis = 60_000L)),
                clock
            )
            val first = createViewModel(repository, clock)
            val second = createViewModel(repository, clock)
            runCurrent()

            assertEquals(2, repository.completionCalls)
            assertEquals(FocusProgress(xp = 10, completedSessions = 1), repository.currentSnapshot().progress)
            assertEquals(
                1,
                listOf(first, second).count {
                    it.uiState.value.session is FocusSessionUiState.JustCompleted
                }
            )
        }

    @Test
    fun `new ViewModel recovers same persisted running session`() =
        runViewModelTest {
            val clock = FakeTimeProvider(10_000L)
            val session = activeSession(endsAtEpochMillis = 70_000L)
            val repository = FakeFocusRepository(snapshot(activeSession = session), clock)
            val first = createViewModel(repository, clock)
            val second = createViewModel(repository, clock)
            runCurrent()

            assertEquals(session, (first.uiState.value.session as FocusSessionUiState.Running).session)
            assertEquals(session, (second.uiState.value.session as FocusSessionUiState.Running).session)
            assertEquals(0, repository.startCalls)
            assertEquals(0, repository.completionCalls)
        }

    @Test
    fun `new ViewModel does not replay old completed reward`() =
        runViewModelTest {
            val clock = FakeTimeProvider(100_000L)
            val repository = FakeFocusRepository(
                snapshot(progress = FocusProgress(xp = 40, completedSessions = 2)),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()

            assertEquals(0, repository.completionCalls)
            assertEquals(40, viewModel.uiState.value.xp)
            assertEquals(2, viewModel.uiState.value.completedSessions)
            assertSame(FocusSessionUiState.Idle, viewModel.uiState.value.session)
        }

    @Test
    fun `completion feedback is transient`() =
        runViewModelTest {
            val clock = FakeTimeProvider(60_001L)
            val repository = FakeFocusRepository(
                snapshot(activeSession = activeSession(endsAtEpochMillis = 60_000L)),
                clock
            )
            val viewModel = createViewModel(repository, clock)
            runCurrent()
            assertTrue(viewModel.uiState.value.session is FocusSessionUiState.JustCompleted)

            advanceTimeBy(2_999L)
            runCurrent()
            assertTrue(viewModel.uiState.value.session is FocusSessionUiState.JustCompleted)

            advanceTimeBy(1L)
            runCurrent()
            assertSame(FocusSessionUiState.Idle, viewModel.uiState.value.session)
            assertEquals(FocusProgress(xp = 10, completedSessions = 1), repository.currentSnapshot().progress)
        }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
        runTest(mainDispatcherRule.dispatcher) {
            try {
                block()
            } finally {
                viewModels.forEach { it.viewModelScope.cancel() }
                viewModels.clear()
                runCurrent()
            }
        }

    private fun createViewModel(
        repository: FocusRepository,
        timeProvider: TimeProvider
    ): FocusViewModel = FocusViewModel(
        repository = repository,
        timeProvider = timeProvider,
        tickIntervalMillis = 1_000L,
        completionFeedbackDurationMillis = 3_000L
    ).also(viewModels::add)

    private fun snapshot(
        selectedCategoryId: String = "study",
        selectedDurationMinutes: Int = 25,
        progress: FocusProgress = FocusProgress(xp = 0, completedSessions = 0),
        activeSession: ActiveFocusSession? = null
    ) = FocusRepositorySnapshot(
        categories = DefaultFocusCategories,
        selectedCategoryId = selectedCategoryId,
        selectedDurationMinutes = selectedDurationMinutes,
        progress = progress,
        activeSession = activeSession
    )

    private fun activeSession(
        id: String = "session-1",
        categoryId: String = "study",
        durationMinutes: Int = 1,
        startedAtEpochMillis: Long = 0L,
        endsAtEpochMillis: Long = 60_000L
    ) = ActiveFocusSession(
        id = id,
        categoryId = categoryId,
        durationMinutes = durationMinutes,
        startedAtEpochMillis = startedAtEpochMillis,
        endsAtEpochMillis = endsAtEpochMillis
    )
}

private class FakeTimeProvider(
    var nowEpochMillis: Long
) : TimeProvider {
    override fun nowEpochMillis(): Long = nowEpochMillis
}

private class FakeFocusRepository(
    initialSnapshot: FocusRepositorySnapshot,
    private val timeProvider: TimeProvider
) : FocusRepository {
    private val mutableSnapshots = MutableStateFlow(initialSnapshot)
    override val snapshots: Flow<FocusRepositorySnapshot> = mutableSnapshots

    val categorySelections = mutableListOf<String>()
    val durationSelections = mutableListOf<Int>()
    val cancellationIds = mutableListOf<String>()
    val completionIds = mutableListOf<String>()
    val categoryMutations = mutableListOf<String>()
    var startCalls = 0
    var completionCalls = 0
    var durableWrites = 0
    private var nextSessionNumber = 1

    override suspend fun currentSnapshot(): FocusRepositorySnapshot = mutableSnapshots.value

    override suspend fun addCategory(category: FocusCategory): Boolean {
        categoryMutations += "add"
        val current = mutableSnapshots.value
        mutableSnapshots.value = current.copy(categories = current.categories + category)
        durableWrites += 1
        return true
    }

    override suspend fun updateCategory(category: FocusCategory): Boolean {
        categoryMutations += "edit"
        val current = mutableSnapshots.value
        val index = current.categories.indexOfFirst { it.id == category.id }
        if (index < 0) return false
        val updated = current.categories.toMutableList().apply { this[index] = category }
        mutableSnapshots.value = current.copy(categories = updated)
        durableWrites += 1
        return true
    }

    override suspend fun deleteCategory(categoryId: String): Boolean {
        categoryMutations += "delete"
        val current = mutableSnapshots.value
        val remaining = current.categories.filterNot { it.id == categoryId }
        if (remaining.size != current.categories.size - 1 || remaining.isEmpty()) return false
        mutableSnapshots.value = current.copy(
            categories = remaining,
            selectedCategoryId = if (current.selectedCategoryId == categoryId) {
                remaining.first().id
            } else {
                current.selectedCategoryId
            }
        )
        durableWrites += 1
        return true
    }

    override suspend fun selectCategory(categoryId: String): Boolean {
        categorySelections += categoryId
        val current = mutableSnapshots.value
        if (current.categories.none { it.id == categoryId }) return false
        mutableSnapshots.value = current.copy(selectedCategoryId = categoryId)
        durableWrites += 1
        return true
    }

    override suspend fun selectDuration(durationMinutes: Int): Boolean {
        durationSelections += durationMinutes
        if (!isSupportedDuration(durationMinutes)) return false
        mutableSnapshots.value = mutableSnapshots.value.copy(
            selectedDurationMinutes = durationMinutes
        )
        durableWrites += 1
        return true
    }

    override suspend fun updateProgress(progress: FocusProgress): Boolean = false

    override suspend fun startFocusSession(): Boolean {
        startCalls += 1
        val current = mutableSnapshots.value
        if (current.activeSession != null) return false
        val startedAt = timeProvider.nowEpochMillis()
        mutableSnapshots.value = current.copy(
            activeSession = ActiveFocusSession(
                id = "started-${nextSessionNumber++}",
                categoryId = current.selectedCategoryId,
                durationMinutes = current.selectedDurationMinutes,
                startedAtEpochMillis = startedAt,
                endsAtEpochMillis = startedAt + current.selectedDurationMinutes * 60_000L
            )
        )
        durableWrites += 1
        return true
    }

    override suspend fun cancelSessionIfActive(expectedSessionId: String): Boolean {
        cancellationIds += expectedSessionId
        val current = mutableSnapshots.value
        val session = current.activeSession
        if (session == null || session.id != expectedSessionId) return false
        mutableSnapshots.value = current.copy(activeSession = null)
        durableWrites += 1
        return true
    }

    override suspend fun completeSessionIfActive(expectedSessionId: String): Boolean {
        completionCalls += 1
        completionIds += expectedSessionId
        val current = mutableSnapshots.value
        val session = current.activeSession
        if (session == null || session.id != expectedSessionId) return false
        mutableSnapshots.value = current.copy(
            progress = current.progress.copy(
                xp = current.progress.xp + rewardForDuration(session.durationMinutes),
                completedSessions = current.progress.completedSessions + 1
            ),
            activeSession = null
        )
        durableWrites += 1
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
