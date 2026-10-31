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

        fun dismiss() {
            context.startService(RingingService.intent(context, RingingService.ACTION_DISMISS))
        }
    }
