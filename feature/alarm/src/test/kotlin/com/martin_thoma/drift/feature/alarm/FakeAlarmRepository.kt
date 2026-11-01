package com.martin_thoma.drift.feature.alarm

import com.martin_thoma.drift.core.data.AlarmRepository
import com.martin_thoma.drift.core.model.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/** Alarms in memory, sorted by time of day like the real repository. */
class FakeAlarmRepository : AlarmRepository {
    private val stored = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override val alarms: StateFlow<List<Alarm>> = stored

    override suspend fun getAll() = stored.value

    override suspend fun get(id: Long) = stored.value.find { it.id == id }

    override suspend fun save(alarm: Alarm): Long {
        val saved = if (alarm.id == 0L) alarm.copy(id = nextId++) else alarm
        stored.value = (stored.value.filter { it.id != saved.id } + saved).sortedBy { it.time }
        return saved.id
    }

    override suspend fun setEnabled(
        id: Long,
        enabled: Boolean,
    ) = change(id) { it.copy(enabled = enabled, snoozedUntil = null) }

    override suspend fun setSound(
        id: Long,
        soundUri: String?,
    ) = change(id) { it.copy(soundUri = soundUri) }

    override suspend fun setSnoozedUntil(
        id: Long,
        until: Instant?,
    ) = change(id) { it.copy(snoozedUntil = until) }

    override suspend fun delete(id: Long) {
        stored.value = stored.value.filter { it.id != id }
    }

    private fun change(
        id: Long,
        transform: (Alarm) -> Alarm,
    ) {
        stored.value = stored.value.map { if (it.id == id) transform(it) else it }
    }
}
