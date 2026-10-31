package com.martin_thoma.tide.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.martin_thoma.tide.feature.alarm.AlarmRoute
import com.martin_thoma.tide.feature.alarm.AlarmScreen
import com.martin_thoma.tide.feature.alarm.AlarmSoundRoute
import com.martin_thoma.tide.feature.alarm.AlarmSoundScreen
import com.martin_thoma.tide.feature.stopwatch.StopwatchRoute
import com.martin_thoma.tide.feature.stopwatch.StopwatchScreen
import com.martin_thoma.tide.feature.timer.TimerRoute
import com.martin_thoma.tide.feature.timer.TimerScreen

@Composable
fun TideApp() {
    var currentTab by rememberSaveable { mutableStateOf(TopLevelDestination.ALARM) }
    val backStacks = TopLevelDestination.entries.associateWith { rememberNavBackStack(it.route) }
    val backStack = backStacks.getValue(currentTab)

    // Back on the root of another tab returns to the Alarm tab before leaving the app.
    BackHandler(enabled = currentTab != TopLevelDestination.ALARM && backStack.size == 1) {
        currentTab = TopLevelDestination.ALARM
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = destination == currentTab
                item(
                    selected = selected,
                    onClick = { currentTab = destination },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<AlarmRoute> { AlarmScreen(onOpenAlarm = { backStack.add(AlarmSoundRoute(it)) }) }
                    entry<AlarmSoundRoute> { key -> AlarmSoundScreen(alarmId = key.alarmId, onBack = { backStack.removeLastOrNull() }) }
                    entry<StopwatchRoute> { StopwatchScreen() }
                    entry<TimerRoute> { TimerScreen() }
                },
        )
    }
}
