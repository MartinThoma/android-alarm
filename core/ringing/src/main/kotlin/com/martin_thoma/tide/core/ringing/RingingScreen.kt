package com.martin_thoma.tide.core.ringing

import android.content.Context
import android.content.Intent

/**
 * Opens the full-screen ringing UI. `:feature:ringing` binds it, since `:core:ringing`
 * must not depend on a feature module.
 */
fun interface RingingScreen {
    fun intent(context: Context): Intent
}
