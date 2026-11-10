package io.github.guilhermedkdk.wakeguard.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.alarm.AlarmBook
import io.github.guilhermedkdk.wakeguard.alarm.AlarmEditorScreen
import io.github.guilhermedkdk.wakeguard.alarm.AlarmsScreen

private enum class Tab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    HOME(R.string.tab_home, R.drawable.ic_home),
    ALARMS(R.string.tab_alarms, R.drawable.ic_alarm),
    SETTINGS(R.string.tab_settings, R.drawable.ic_settings),
}

// Id the editor uses while the alarm being created has none yet.
private const val NEW_ALARM = 0L

@Composable
internal fun WakeGuardApp(alarmBook: AlarmBook) {
    var current by rememberSaveable { mutableStateOf(Tab.HOME) }
    // Alarm open in the editor; null shows the tabs.
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    WakeGuardTheme {
        val alarmId = editing
        if (alarmId != null) {
            AlarmEditorScreen(alarmBook, alarmId.takeIf { it != NEW_ALARM }, onClose = { editing = null })
            return@WakeGuardTheme
        }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == current,
                            onClick = { current = tab },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            when (current) {
                Tab.HOME -> {
                    Placeholder(R.string.home_empty, Modifier.padding(padding))
                }

                Tab.ALARMS -> {
                    AlarmsScreen(
                        book = alarmBook,
                        onCreate = { editing = NEW_ALARM },
                        onEdit = { editing = it },
                        modifier = Modifier.padding(padding),
                    )
                }

                Tab.SETTINGS -> {
                    Placeholder(R.string.settings_empty, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun Placeholder(
    @StringRes text: Int,
    modifier: Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(text))
    }
}
