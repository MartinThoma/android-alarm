package com.martin_thoma.drift.core.data

import com.martin_thoma.drift.core.database.TimerDao
import com.martin_thoma.drift.core.database.TimerEntity
import com.martin_thoma.drift.core.model.Timer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** All timers, oldest first. Like alarms, system alarms for them are rebuilt from here. */
@Singleton
class TimerRepository
    @Inject
    constructor(
        private val dao: TimerDao,
    ) {
        val timers: Flow<List<Timer>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

        suspend fun getAll(): List<Timer> = dao.getAll().map { it.toModel() }

        suspend fun get(id: Long): Timer? = dao.get(id)?.toModel()

        /** Inserts a new timer (id 0) or replaces the one with the same id. Returns the id. */
        suspend fun save(timer: Timer): Long {
            val rowId = dao.upsert(timer.toEntity())
            return if (timer.id == 0L) rowId else timer.id
        }

        /** Applies [change] to the timer with [id], if it still exists. */
        suspend fun update(
            id: Long,
            change: (Timer) -> Timer,
        ) {
            get(id)?.let { save(change(it)) }
        }

        suspend fun delete(id: Long) = dao.delete(id)
    }

internal fun TimerEntity.toModel() = Timer(id, label, setMillis, durationMillis, remainingMillis, endsAt, createdAt)

internal fun Timer.toEntity() = TimerEntity(id, label, setMillis, durationMillis, remainingMillis, endsAt, createdAt)
