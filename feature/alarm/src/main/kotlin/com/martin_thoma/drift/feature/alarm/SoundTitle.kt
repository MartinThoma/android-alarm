package com.martin_thoma.drift.feature.alarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.martin_thoma.drift.core.ringing.AlarmSounds
import com.martin_thoma.drift.core.ringing.PhoneSound
import com.martin_thoma.drift.core.ringing.PhoneSounds
import com.martin_thoma.drift.core.ringing.R as RingingR

/** The phone's own tones, so their names can be shown; the app provides them. */
val LocalPhoneSounds = compositionLocalOf<List<PhoneSound>> { emptyList() }

/** The name of the sound an alarm with [soundUri] rings with: built in, the phone's, the user's own, or none. */
@Composable
internal fun soundTitle(soundUri: String?): String =
    when {
        AlarmSounds.isSilent(soundUri) -> {
            stringResource(RingingR.string.core_ringing_sound_none)
        }

        PhoneSounds.isPhoneSound(soundUri) -> {
            LocalPhoneSounds.current.find { it.uri == soundUri }?.title ?: stringResource(R.string.feature_alarm_sound_phone_unknown)
        }

        else -> {
            AlarmSounds.builtIn(soundUri)?.let { stringResource(it.title) } ?: AlarmSounds.custom(soundUri)?.title.orEmpty()
        }
    }
