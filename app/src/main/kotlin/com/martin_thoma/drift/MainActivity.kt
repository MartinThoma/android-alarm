package com.martin_thoma.drift

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.martin_thoma.drift.core.designsystem.TideTheme
import com.martin_thoma.drift.core.notifications.OpenApp
import com.martin_thoma.drift.core.ringing.PhoneSounds
import com.martin_thoma.drift.core.ringing.RingingScreen
import com.martin_thoma.drift.core.ringing.RingingState
import com.martin_thoma.drift.core.ringing.RingingStatus
import com.martin_thoma.drift.feature.alarm.LocalPhoneSounds
import com.martin_thoma.drift.navigation.DriftApp
import com.martin_thoma.drift.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** A tab a notification asked for, until the UI has switched to it. */
    private val requestedTab = mutableStateOf<TopLevelDestination?>(null)

    @Inject lateinit var phoneSounds: PhoneSounds

    @Inject lateinit var ringingState: RingingState

    @Inject lateinit var ringingScreen: RingingScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bright water by day takes dark status bar icons; the night sea takes light ones.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) requestTab(intent)
        // Whatever rings is always in front: opening the app, or a ring starting while it is open,
        // shows the ringing screen. Its notification may be gone, swiped away or never allowed.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ringingState.status
                    .map { it != RingingStatus.Idle }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { startActivity(ringingScreen.intent(this@MainActivity)) }
            }
        }
        setContent {
            val phoneTones by phoneSounds.sounds.collectAsStateWithLifecycle()
            TideTheme {
                CompositionLocalProvider(LocalPhoneSounds provides phoneTones) {
                    DriftApp(requestedTab = requestedTab.value, onTabShown = { requestedTab.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestTab(intent)
    }

    private fun requestTab(intent: Intent) {
        requestedTab.value =
            when (intent.getStringExtra(OpenApp.EXTRA_TAB)) {
                OpenApp.TAB_STOPWATCH -> TopLevelDestination.STOPWATCH
                OpenApp.TAB_TIMER -> TopLevelDestination.TIMER
                else -> null
            }
    }
}
