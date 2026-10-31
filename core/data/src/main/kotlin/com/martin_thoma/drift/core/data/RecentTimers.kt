package com.martin_thoma.drift.core.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A duration (and label) started before, for one-tap starts (requirements TM-04). */
data class RecentTimer(
    val setMillis: Long,
    val label: String = "",
)

/** The last few timers started, newest first; a few common durations until there are any. */
@Singleton
class RecentTimers
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val saved = context.getSharedPreferences("recent_timers", Context.MODE_PRIVATE)
        private val state = MutableStateFlow(load())

        val recents: StateFlow<List<RecentTimer>> = state.asStateFlow()

        @Synchronized
        fun remember(timer: RecentTimer) {
            val next = (listOf(timer) + loadSaved()).distinct().take(MAX)
            // One line per timer: milliseconds, a tab, the label.
            saved.edit { putString(KEY, next.joinToString("\n") { "${it.setMillis}\t${it.label.replace('\n', ' ').replace('\t', ' ')}" }) }
            state.value = next
        }

        private fun load(): List<RecentTimer> = loadSaved().ifEmpty { DEFAULTS }

        private fun loadSaved(): List<RecentTimer> =
            saved
                .getString(KEY, "")
                .orEmpty()
                .lines()
                .mapNotNull { line ->
                    val millis = line.substringBefore('\t').toLongOrNull() ?: return@mapNotNull null
                    RecentTimer(millis, line.substringAfter('\t', ""))
                }

        private companion object {
            const val KEY = "recent"
            const val MAX = 6
            val DEFAULTS = listOf(1, 3, 5, 10, 15).map { RecentTimer(it * 60_000L) }
        }
    }
