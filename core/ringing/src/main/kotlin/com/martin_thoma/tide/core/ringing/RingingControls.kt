package com.martin_thoma.tide.core.ringing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Snooze and dismiss for the ringing screen. The notification's actions do the same. */
class RingingControls
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        fun snooze() {
            context.startService(RingingService.intent(context, RingingService.ACTION_SNOOZE))
        }

        /** For a ringing timer: one more minute. */
        fun addMinute() {
            context.startService(RingingService.intent(context, RingingService.ACTION_ADD_MINUTE))
        }

        fun dismiss() {
            context.startService(RingingService.intent(context, RingingService.ACTION_DISMISS))
        }
    }
