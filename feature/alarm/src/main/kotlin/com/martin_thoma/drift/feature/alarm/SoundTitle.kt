package com.martin_thoma.drift.feature.alarm

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.martin_thoma.drift.core.ringing.AlarmSounds
import com.martin_thoma.drift.core.ringing.R as RingingR

/** The name of the sound an alarm with [soundUri] rings with: built in, the user's own, or none. */
@Composable
internal fun soundTitle(soundUri: String?): String =
    when {
        AlarmSounds.isSilent(soundUri) -> stringResource(RingingR.string.core_ringing_sound_none)
        else -> AlarmSounds.builtIn(soundUri)?.let { stringResource(it.title) } ?: AlarmSounds.custom(soundUri)?.title.orEmpty()
    }
