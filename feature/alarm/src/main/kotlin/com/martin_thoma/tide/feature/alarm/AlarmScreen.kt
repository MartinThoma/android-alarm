package com.martin_thoma.tide.feature.alarm

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.core.designsystem.PlaceholderScreen
import com.martin_thoma.tide.core.designsystem.TideTheme
import kotlinx.serialization.Serializable

@Serializable
data object AlarmRoute : NavKey

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_alarm_title),
        message = stringResource(R.string.feature_alarm_placeholder),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AlarmScreenPreview() {
    TideTheme(darkTheme = false) { AlarmScreen() }
}
