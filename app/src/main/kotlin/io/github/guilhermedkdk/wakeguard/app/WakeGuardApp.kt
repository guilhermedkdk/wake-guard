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

private enum class Tab(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @StringRes val placeholder: Int,
) {
    HOME(R.string.tab_home, R.drawable.ic_home, R.string.home_empty),
    ALARMS(R.string.tab_alarms, R.drawable.ic_alarm, R.string.alarms_empty),
    SETTINGS(R.string.tab_settings, R.drawable.ic_settings, R.string.settings_empty),
}

@Composable
internal fun WakeGuardApp() {
    var current by rememberSaveable { mutableStateOf(Tab.HOME) }
    WakeGuardTheme {
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(current.placeholder))
            }
        }
    }
}
