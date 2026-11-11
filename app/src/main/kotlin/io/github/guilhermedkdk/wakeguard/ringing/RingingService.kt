package io.github.guilhermedkdk.wakeguard.ringing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import io.github.guilhermedkdk.wakeguard.alarm.AlarmBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

// Owns the ringing: its rules, sound, vibration and clock. The screen only draws what is published.
internal class RingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller by lazy { RingingController.of(this) }
    private var busy = false
    private var ringing: Ringing? = null
    private var player: AlarmPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Must come within seconds of startForegroundService, even for a start that is then ignored.
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, REHEARSAL) ?: REHEARSAL
        // One ringing at a time: an alarm that comes during another is ignored.
        if (busy) return START_NOT_STICKY
        busy = true
        scope.launch {
            val alarm =
                if (alarmId ==
                    REHEARSAL
                ) {
                    controller.takeRehearsed()
                } else {
                    AlarmBook.of(this@RingingService).find(alarmId)
                }
            if (alarm == null) finish() else ring(alarm, rehearsal = alarmId == REHEARSAL)
        }
        // A real ringing comes back if the process dies; a rehearsal does not.
        return if (alarmId == REHEARSAL) START_NOT_STICKY else START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        release()
        scope.cancel()
    }

    fun challengeCompleted() {
        ringing?.challengeCompleted(Instant.now())
        sync()
    }

    fun interacted() {
        ringing?.interacted(Instant.now())
        sync()
    }

    fun endRehearsal() {
        if (ringing?.state?.rehearsal == true) finish()
    }

    private suspend fun ring(
        alarm: Alarm,
        rehearsal: Boolean,
    ) {
        ringing = Ringing(alarm, rehearsal, Instant.now(), MAX_DURATION)
        player = AlarmPlayer(this, alarm)
        controller.service = this
        sync()
        // Opens the screen right away when the app is in front; otherwise the notification does it.
        startActivity(RingingActivity.intent(this))
        while (true) {
            delay(TICK_MILLIS)
            sync()
        }
    }

    private fun sync() {
        val ringing = ringing ?: return
        val now = Instant.now()
        ringing.tick(now)
        if (ringing.outcome != null) return finish()
        player?.setVolume(if (ringing.muted(now)) 0f else ringing.volume(now))
        controller.publish(ringing.state)
    }

    private fun finish() {
        release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        // Last, because one of these children may be the caller.
        scope.coroutineContext.cancelChildren()
    }

    private fun release() {
        player?.release()
        player = null
        ringing = null
        busy = false
        controller.service = null
        controller.publish(null)
    }

    private fun notification(): Notification {
        val channel =
            NotificationChannel(CHANNEL, getString(R.string.ringing_channel), NotificationManager.IMPORTANCE_HIGH)
        // The service plays the sound and vibrates itself.
        channel.setSound(null, null)
        channel.enableVibration(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val screen =
            PendingIntent.getActivity(
                this,
                0,
                RingingActivity.intent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification
            .Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.ringing_notification_title))
            .setContentText(getString(R.string.ringing_notification_text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(screen)
            // Opens the screen over the lock screen when the phone is locked or its screen is off.
            .setFullScreenIntent(screen, true)
            .build()
    }

    companion object {
        const val REHEARSAL = 0L
        private const val EXTRA_ALARM_ID = "alarmId"
        private const val CHANNEL = "ringing"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MILLIS = 250L

        // Becomes a setting together with the Ajustes tab.
        private val MAX_DURATION: Duration = Duration.ofMinutes(2)

        fun intent(
            context: Context,
            alarmId: Long,
        ): Intent = Intent(context, RingingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
