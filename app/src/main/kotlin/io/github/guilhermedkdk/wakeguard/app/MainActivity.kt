package io.github.guilhermedkdk.wakeguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

internal class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WakeGuardApplication).container
        setContent { WakeGuardApp(container.alarmBook, container.ringing::rehearse) }
    }
}
