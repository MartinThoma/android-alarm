package com.martin_thoma.tide.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.PlaceholderScreen
import com.martin_thoma.tide.core.designsystem.TideTheme
import kotlinx.serialization.Serializable

@Serializable
data object TimerRoute : NavKey

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_timer_title),
        message = stringResource(R.string.feature_timer_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPreview() {
    TideTheme(dynamicColor = false) { TimerScreen() }
}
