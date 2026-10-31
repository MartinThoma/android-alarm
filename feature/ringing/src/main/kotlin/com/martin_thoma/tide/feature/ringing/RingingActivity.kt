package com.martin_thoma.tide.feature.ringing

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.martin_thoma.tide.core.designsystem.TideTheme
import com.martin_thoma.tide.core.domain.TimeSource
import com.martin_thoma.tide.core.ringing.RingingControls
import com.martin_thoma.tide.core.ringing.RingingState
import com.martin_thoma.tide.core.ringing.RingingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** The full-screen ringing UI, also over the lock screen (requirements AL-11). */
@AndroidEntryPoint
class RingingActivity : ComponentActivity() {
    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var controls: RingingControls

    @Inject lateinit var timeSource: TimeSource

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bright water by day takes dark status bar icons; the night sea takes light ones.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Only snooze and the dismiss slider end the alarm; back must not silence it by accident.
        onBackPressedDispatcher.addCallback(this) {}
        // Closes when the alarm stops, however that happens (also from the notification or auto-silence).
        lifecycleScope.launch {
            ringingState.status.first { it == RingingStatus.Idle }
            finish()
        }
        setContent {
            TideTheme {
                val status by ringingState.status.collectAsStateWithLifecycle()
                val now by produceState(timeSource.now()) {
                    while (true) {
                        delay(ChronoUnit.MILLIS.between(value, value.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)))
                        value = timeSource.now()
                    }
                }
                RingingContent(status = status, now = now, onSnooze = controls::snooze, onDismiss = controls::dismiss)
            }
        }
    }
}
