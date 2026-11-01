package com.martin_thoma.drift.feature.alarm

import com.martin_thoma.drift.core.domain.TimeSource
import com.martin_thoma.drift.core.model.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {
    private val repository = FakeAlarmRepository()
    private val now = ZonedDateTime.parse("2026-11-01T23:18:00+01:00[Europe/Berlin]")
    private val viewModel by lazy { AlarmListViewModel(repository, TimeSource { now }) }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the state shows the alarms and the one that rings next`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            assertTrue(
                viewModel.uiState.value.alarms
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.loading)

            viewModel.add(LocalTime.of(6, 30))
            viewModel.add(LocalTime.of(23, 45))

            val state = viewModel.uiState.value
            assertEquals(listOf(LocalTime.of(6, 30), LocalTime.of(23, 45)), state.alarms.map { it.time })
            // 23:45 is still ahead tonight, so it rings before 6:30.
            assertEquals(LocalTime.of(23, 45), state.upcoming?.alarm?.time)
        }

    @Test
    fun `turning the only alarm off leaves nothing upcoming`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            viewModel.add(LocalTime.of(6, 30))
            val alarm =
                viewModel.uiState.value.alarms
                    .single()

            viewModel.setEnabled(alarm, false)

            assertFalse(
                viewModel.uiState.value.alarms
                    .single()
                    .enabled,
            )
            assertNull(viewModel.uiState.value.upcoming)
        }

    @Test
    fun `undo puts a deleted alarm back with its id and settings`() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
            repository.save(Alarm(time = LocalTime.of(7, 0), label = "Work", snoozeMinutes = 15))
            val alarm =
                viewModel.uiState.value.alarms
                    .single()

            viewModel.delete(alarm)
            assertTrue(
                viewModel.uiState.value.alarms
                    .isEmpty(),
            )
            viewModel.restore(alarm)

            assertEquals(listOf(alarm), viewModel.uiState.value.alarms)
        }
}
