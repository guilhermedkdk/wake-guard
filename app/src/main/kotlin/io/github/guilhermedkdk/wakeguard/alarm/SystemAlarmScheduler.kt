package io.github.guilhermedkdk.wakeguard.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

private const val EXTRA_ALARM_ID = "alarmId"

/** The broadcast the app sends to itself when the time of an alarm comes. */
object AlarmTime {
    const val ACTION = "io.github.guilhermedkdk.wakeguard.ALARM_TIME"

    fun alarmId(intent: Intent): Long = intent.getLongExtra(EXTRA_ALARM_ID, 0)
}

internal class SystemAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(
        alarmId: Long,
        at: Instant,
    ) {
        // setAlarmClock is exact, wakes the device from Doze and shows the alarm icon in the status bar.
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), openApp()), ring(alarmId))
    }

    override fun cancel(alarmId: Long) = manager.cancel(ring(alarmId))

    // The request code tells the alarms apart; extras alone do not make a distinct PendingIntent.
    private fun ring(alarmId: Long) =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            Intent(AlarmTime.ACTION).setPackage(context.packageName).putExtra(EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp() =
        PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
