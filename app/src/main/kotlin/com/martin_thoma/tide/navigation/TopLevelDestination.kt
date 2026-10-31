package com.martin_thoma.tide.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.tide.R
import com.martin_thoma.tide.core.designsystem.TideIcons
import com.martin_thoma.tide.feature.alarm.AlarmRoute
import com.martin_thoma.tide.feature.stopwatch.StopwatchRoute
import com.martin_thoma.tide.feature.timer.TimerRoute

/** The tabs of the navigation bar (requirements GN-01). Each tab keeps its own back stack. */
enum class TopLevelDestination(
    val route: NavKey,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    ALARM(AlarmRoute, R.string.tab_alarm, TideIcons.Alarm, TideIcons.AlarmFilled),
    STOPWATCH(StopwatchRoute, R.string.tab_stopwatch, TideIcons.Stopwatch, TideIcons.StopwatchFilled),
    TIMER(TimerRoute, R.string.tab_timer, TideIcons.Timer, TideIcons.TimerFilled),
}
