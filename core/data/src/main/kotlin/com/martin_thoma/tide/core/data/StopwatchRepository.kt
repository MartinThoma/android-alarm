package com.martin_thoma.tide.core.data

import android.content.Context
import androidx.core.content.edit
import com.martin_thoma.tide.core.model.Stopwatch
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The one stopwatch, saved on every change so it survives the app being closed or killed (requirements SW-03). */
@Singleton
class StopwatchRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val saved = context.getSharedPreferences("stopwatch", Context.MODE_PRIVATE)
        private val state = MutableStateFlow(load())

        val stopwatch: StateFlow<Stopwatch> = state.asStateFlow()

        @Synchronized
        fun update(change: (Stopwatch) -> Stopwatch) {
            val next = change(state.value)
            if (next == state.value) return
            saved.edit {
                putLong(KEY_ACCUMULATED, next.accumulatedMillis)
                next.runningSince?.let { putLong(KEY_RUNNING_SINCE, it) } ?: remove(KEY_RUNNING_SINCE)
                putString(KEY_LAPS, next.lapMarks.joinToString(","))
            }
            state.value = next
        }

        private fun load() =
            Stopwatch(
                accumulatedMillis = saved.getLong(KEY_ACCUMULATED, 0),
                runningSince = if (saved.contains(KEY_RUNNING_SINCE)) saved.getLong(KEY_RUNNING_SINCE, 0) else null,
                lapMarks =
                    saved
                        .getString(KEY_LAPS, "")
                        .orEmpty()
                        .split(',')
                        .mapNotNull { it.toLongOrNull() },
            )

        private companion object {
            const val KEY_ACCUMULATED = "accumulated"
            const val KEY_RUNNING_SINCE = "running_since"
            const val KEY_LAPS = "laps"
        }
    }
