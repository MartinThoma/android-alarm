package com.martin_thoma.tide.core.data

import com.martin_thoma.tide.core.database.AlarmDao
import com.martin_thoma.tide.core.database.AlarmEntity
import com.martin_thoma.tide.core.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

internal class RoomAlarmRepository
    @Inject
    constructor(
        private val dao: AlarmDao,
    ) : AlarmRepository {
        override val alarms: Flow<List<Alarm>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

        override suspend fun getAll(): List<Alarm> = dao.getAll().map { it.toModel() }

        override suspend fun get(id: Long): Alarm? = dao.get(id)?.toModel()

        override suspend fun save(alarm: Alarm): Long {
            val rowId = dao.upsert(alarm.toEntity())
            return if (alarm.id == 0L) rowId else alarm.id
        }

        override suspend fun setEnabled(
            id: Long,
            enabled: Boolean,
        ) = dao.setEnabled(id, enabled)

        override suspend fun delete(id: Long) = dao.delete(id)
    }

internal fun AlarmEntity.toModel() =
    Alarm(
        id = id,
        time = LocalTime.of(hour, minute),
        repeatDays = DayOfWeek.entries.filterTo(mutableSetOf()) { repeatDays and it.bit() != 0 },
        enabled = enabled,
        label = label,
        volumePercent = volumePercent,
        soundUri = soundUri,
        vibrate = vibrate,
        snoozeMinutes = snoozeMinutes,
    )

internal fun Alarm.toEntity() =
    AlarmEntity(
        id = id,
        hour = time.hour,
        minute = time.minute,
        repeatDays = repeatDays.fold(0) { bits, day -> bits or day.bit() },
        enabled = enabled,
        label = label,
        volumePercent = volumePercent,
        soundUri = soundUri,
        vibrate = vibrate,
        snoozeMinutes = snoozeMinutes,
    )

private fun DayOfWeek.bit() = 1 shl (value - 1)
