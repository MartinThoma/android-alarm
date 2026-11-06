package com.martin_thoma.drift.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.martin_thoma.drift.core.designsystem.GlassPane
import com.martin_thoma.drift.core.designsystem.LocalTideColors
import com.martin_thoma.drift.core.designsystem.TideAware
import com.martin_thoma.drift.core.designsystem.TideIcons
import com.martin_thoma.drift.core.designsystem.TideLevel
import com.martin_thoma.drift.core.designsystem.TideSurface
import kotlinx.serialization.Serializable
import com.martin_thoma.drift.core.notifications.R as NotificationsR

@Serializable
data object HealthCheckRoute : NavKey

/** Every setting an alarm depends on, each with a one-tap fix (requirements GN-04). */
@Composable
fun HealthCheckScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val health = rememberHealth()
    val context = LocalContext.current
    HealthCheckContent(health = health, onBack = onBack, onFix = { context.open(it) }, modifier = modifier)
}

/** On the alarm list, only while a setting can stop alarms from ringing or showing; opens the health check. */
@Composable
fun HealthNotice(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val health = rememberHealth()
    if (health.problems > 0) HealthNoticeContent(health.problems, onOpen, modifier)
}

/** A quiet way to the health check while everything is fine. */
@Composable
fun HealthCheckLink(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TideAware(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Ink, not the accent: it is text on the beach, and the accent is hard to read over the water.
            TextButton(onClick = onOpen, colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current)) {
                Text(stringResource(R.string.feature_settings_link))
            }
        }
    }
}

/** The health, read again whenever the app comes back to the front, e.g. from the system settings. */
@Composable
private fun rememberHealth(): Health {
    val viewModel: HealthViewModel = hiltViewModel()
    val health by viewModel.health.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    return health
}

/** Opens a fix; a phone without that screen gets the app's info page, where every setting is. */
private fun Context.open(fix: Fix) {
    try {
        startActivity(fix.intent(this))
    } catch (e: ActivityNotFoundException) {
        Log.w("DriftHealth", "No screen for $fix", e)
        startActivity(Fix.APP_INFO.intent(this))
    }
}

@Composable
internal fun HealthNoticeContent(
    problems: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    GlassPane(modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.feature_settings_notice_open), role = Role.Button, onClick = onOpen)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(TideIcons.Warning), contentDescription = null, tint = colors.danger)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.feature_settings_notice_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    pluralStringResource(R.plurals.feature_settings_health_problems, problems, problems),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onGlassVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HealthCheckContent(
    health: Health,
    onBack: () -> Unit,
    onFix: (Fix) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTideColors.current
    // Nothing to count toward here: low tide.
    TideSurface(level = TideLevel.LOW, modifier = modifier) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colors.onSand,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.feature_settings_health_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(TideIcons.ArrowBack), contentDescription = stringResource(R.string.feature_settings_back))
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = colors.onSand,
                            navigationIconContentColor = colors.onSand,
                        ),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            ) {
                item(key = "summary") {
                    TideAware(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
                        Text(
                            text =
                                if (health.problems == 0) {
                                    stringResource(R.string.feature_settings_health_all_good)
                                } else {
                                    pluralStringResource(R.plurals.feature_settings_health_problems, health.problems, health.problems)
                                },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
                item(key = "checks") {
                    GlassPane(Modifier.fillMaxWidth()) {
                        Column {
                            NotificationsRow(health.notifications, onFix)
                            PaneDivider()
                            CheckRow(
                                status = if (health.fullScreen) Status.OK else Status.PROBLEM,
                                title = stringResource(R.string.feature_settings_full_screen),
                                text =
                                    stringResource(
                                        if (health.fullScreen) {
                                            R.string.feature_settings_full_screen_on
                                        } else {
                                            R.string.feature_settings_full_screen_off
                                        },
                                    ),
                                fix = Fix.FULL_SCREEN.takeUnless { health.fullScreen },
                                fixLabel = stringResource(R.string.feature_settings_turn_on),
                                onFix = onFix,
                            )
                            PaneDivider()
                            CheckRow(
                                status = if (health.exactAlarms) Status.OK else Status.PROBLEM,
                                title = stringResource(R.string.feature_settings_exact_alarms),
                                text =
                                    stringResource(
                                        if (health.exactAlarms) {
                                            R.string.feature_settings_exact_alarms_on
                                        } else {
                                            R.string.feature_settings_exact_alarms_off
                                        },
                                    ),
                                fix = Fix.EXACT_ALARMS.takeUnless { health.exactAlarms },
                                fixLabel = stringResource(R.string.feature_settings_turn_on),
                                onFix = onFix,
                            )
                            PaneDivider()
                            BatteryRow(health.battery, health.samsung, onFix)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsRow(
    notifications: Health.Notifications,
    onFix: (Fix) -> Unit,
) {
    CheckRow(
        status = if (notifications == Health.Notifications.ON) Status.OK else Status.PROBLEM,
        title = stringResource(R.string.feature_settings_notifications),
        text =
            when (notifications) {
                Health.Notifications.ON -> {
                    stringResource(R.string.feature_settings_notifications_on)
                }

                Health.Notifications.OFF -> {
                    stringResource(R.string.feature_settings_notifications_off)
                }

                Health.Notifications.RINGING_TURNED_DOWN -> {
                    stringResource(
                        R.string.feature_settings_ringing_turned_down,
                        stringResource(NotificationsR.string.core_notifications_channel_ringing),
                    )
                }
            },
        fix =
            when (notifications) {
                Health.Notifications.ON -> null
                Health.Notifications.OFF -> Fix.NOTIFICATIONS
                Health.Notifications.RINGING_TURNED_DOWN -> Fix.RINGING_CHANNEL
            },
        fixLabel = stringResource(R.string.feature_settings_turn_on),
        onFix = onFix,
    )
}

@Composable
private fun BatteryRow(
    battery: Health.Battery,
    samsung: Boolean,
    onFix: (Fix) -> Unit,
) {
    CheckRow(
        status =
            when (battery) {
                Health.Battery.UNRESTRICTED -> Status.OK
                Health.Battery.OPTIMISED -> Status.TIP
                Health.Battery.RESTRICTED -> Status.PROBLEM
            },
        title = stringResource(R.string.feature_settings_battery),
        text =
            stringResource(
                when (battery) {
                    Health.Battery.UNRESTRICTED -> R.string.feature_settings_battery_unrestricted
                    Health.Battery.OPTIMISED -> R.string.feature_settings_battery_optimised
                    Health.Battery.RESTRICTED -> R.string.feature_settings_battery_restricted
                },
            ),
        // Samsung's sleeping apps lists are not visible to apps, so the hint stays even when all looks fine.
        note = if (samsung) stringResource(R.string.feature_settings_battery_samsung) else null,
        fix =
            when (battery) {
                Health.Battery.UNRESTRICTED -> null
                Health.Battery.OPTIMISED -> Fix.BATTERY_OPTIMISATION
                Health.Battery.RESTRICTED -> Fix.APP_INFO
            },
        fixLabel =
            stringResource(
                if (battery ==
                    Health.Battery.RESTRICTED
                ) {
                    R.string.feature_settings_open_app_info
                } else {
                    R.string.feature_settings_stop_optimising
                },
            ),
        onFix = onFix,
    )
}

private enum class Status { OK, TIP, PROBLEM }

/** One setting: its state as an icon and in words (never by colour alone), and a fix if it needs one. */
@Composable
private fun CheckRow(
    status: Status,
    title: String,
    text: String,
    fix: Fix?,
    fixLabel: String,
    onFix: (Fix) -> Unit,
    note: String? = null,
) {
    val colors = LocalTideColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Icon(
            painter =
                painterResource(
                    when (status) {
                        Status.OK -> TideIcons.CheckCircle
                        Status.TIP -> TideIcons.Info
                        Status.PROBLEM -> TideIcons.Warning
                    },
                ),
            contentDescription =
                stringResource(
                    when (status) {
                        Status.OK -> R.string.feature_settings_status_ok
                        Status.TIP -> R.string.feature_settings_status_tip
                        Status.PROBLEM -> R.string.feature_settings_status_problem
                    },
                ),
            tint =
                when (status) {
                    Status.OK -> colors.accent
                    Status.TIP -> colors.onGlassVariant
                    Status.PROBLEM -> colors.danger
                },
            modifier = Modifier.padding(top = 2.dp).size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onGlassVariant)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onGlassVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
    if (fix != null) {
        Box(Modifier.fillMaxWidth().padding(start = 60.dp, end = 20.dp, bottom = 14.dp)) {
            Button(
                onClick = { onFix(fix) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) {
                Text(fixLabel)
            }
        }
    }
}

@Composable
private fun PaneDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = LocalContentColor.current.copy(alpha = 0.12f))
}
