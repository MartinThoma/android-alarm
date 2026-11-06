package com.martin_thoma.drift.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.martin_thoma.drift.core.designsystem.FittedText
import com.martin_thoma.drift.feature.alarm.AlarmEditorRoute
import com.martin_thoma.drift.feature.alarm.AlarmEditorScreen
import com.martin_thoma.drift.feature.alarm.AlarmRoute
import com.martin_thoma.drift.feature.alarm.AlarmScreen
import com.martin_thoma.drift.feature.alarm.AlarmSoundRoute
import com.martin_thoma.drift.feature.alarm.AlarmSoundScreen
import com.martin_thoma.drift.feature.settings.HealthCheckLink
import com.martin_thoma.drift.feature.settings.HealthCheckRoute
import com.martin_thoma.drift.feature.settings.HealthCheckScreen
import com.martin_thoma.drift.feature.settings.HealthNotice
import com.martin_thoma.drift.feature.stopwatch.StopwatchRoute
import com.martin_thoma.drift.feature.stopwatch.StopwatchScreen
import com.martin_thoma.drift.feature.timer.NewTimerRoute
import com.martin_thoma.drift.feature.timer.NewTimerScreen
import com.martin_thoma.drift.feature.timer.TimerDetailRoute
import com.martin_thoma.drift.feature.timer.TimerDetailScreen
import com.martin_thoma.drift.feature.timer.TimerRoute
import com.martin_thoma.drift.feature.timer.TimerScreen

@Composable
fun DriftApp(
    requestedTab: TopLevelDestination? = null,
    onTabShown: () -> Unit = {},
) {
    var currentTab by rememberSaveable { mutableStateOf(TopLevelDestination.ALARM) }
    // A notification can open a tab, e.g. the running stopwatch.
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            currentTab = requestedTab
            onTabShown()
        }
    }
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
                    label = { FittedText(stringResource(destination.label)) },
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
                    entry<AlarmRoute> {
                        AlarmScreen(
                            onOpenAlarm = { backStack.add(AlarmEditorRoute(it)) },
                            notice = { HealthNotice(onOpen = { backStack.add(HealthCheckRoute) }, Modifier.padding(bottom = 16.dp)) },
                            footer = { HealthCheckLink(onOpen = { backStack.add(HealthCheckRoute) }, Modifier.padding(top = 8.dp)) },
                        )
                    }
                    entry<HealthCheckRoute> { HealthCheckScreen(onBack = { backStack.removeLastOrNull() }) }
                    entry<AlarmEditorRoute> { key ->
                        AlarmEditorScreen(
                            alarmId = key.alarmId,
                            onBack = { backStack.removeLastOrNull() },
                            onChooseSound = { backStack.add(AlarmSoundRoute(key.alarmId)) },
                        )
                    }
                    entry<AlarmSoundRoute> { key -> AlarmSoundScreen(alarmId = key.alarmId, onBack = { backStack.removeLastOrNull() }) }
                    entry<StopwatchRoute> { StopwatchScreen() }
                    entry<TimerRoute> {
                        TimerScreen(onNewTimer = { backStack.add(NewTimerRoute) }, onOpenTimer = { backStack.add(TimerDetailRoute(it)) })
                    }
                    entry<TimerDetailRoute> { key -> TimerDetailScreen(timerId = key.timerId, onBack = { backStack.removeLastOrNull() }) }
                    entry<NewTimerRoute> { NewTimerScreen(onDone = { backStack.removeLastOrNull() }) }
                },
        )
    }
}
