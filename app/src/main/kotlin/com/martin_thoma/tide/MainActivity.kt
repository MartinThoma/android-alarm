package com.martin_thoma.tide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.martin_thoma.tide.core.designsystem.TideTheme
import com.martin_thoma.tide.navigation.TideApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TideTheme {
                TideApp()
            }
        }
    }
}
