package io.github.guilhermedkdk.wakeguard.app

import android.app.Application
import io.github.guilhermedkdk.wakeguard.alarm.AlarmBook
import io.github.guilhermedkdk.wakeguard.ringing.RingingController

internal class WakeGuardApplication : Application() {
    val container by lazy { AppContainer(this) }
}

// The few long-lived objects of the app, built on first use.
internal class AppContainer(
    application: Application,
) {
    val alarmBook by lazy { AlarmBook.of(application) }
    val ringing by lazy { RingingController.of(application) }
}
