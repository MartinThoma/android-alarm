package com.martin_thoma.drift

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.martin_thoma.drift.core.designsystem.TideTheme
import com.martin_thoma.drift.core.notifications.OpenApp
import com.martin_thoma.drift.navigation.DriftApp
import com.martin_thoma.drift.navigation.TopLevelDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** A tab a notification asked for, until the UI has switched to it. */
    private val requestedTab = mutableStateOf<TopLevelDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bright water by day takes dark status bar icons; the night sea takes light ones.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) requestTab(intent)
        setContent {
            TideTheme {
                DriftApp(requestedTab = requestedTab.value, onTabShown = { requestedTab.value = null })
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
