package io.github.guilhermedkdk.wakeguard.ringing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.guilhermedkdk.wakeguard.alarm.AlarmBook
import io.github.guilhermedkdk.wakeguard.alarm.AlarmTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Woken when the time of an alarm comes: settles the alarm in the book and starts the ringing.
internal class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != AlarmTime.ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarm = AlarmBook.of(context).markRung(AlarmTime.alarmId(intent))
                if (alarm != null) RingingController.of(context).start(alarm.id)
            } finally {
                pending.finish()
            }
        }
    }
}
