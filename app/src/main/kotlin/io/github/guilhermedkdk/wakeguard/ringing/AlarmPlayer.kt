package io.github.guilhermedkdk.wakeguard.ringing

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.net.toUri
import io.github.guilhermedkdk.wakeguard.R
import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import io.github.guilhermedkdk.wakeguard.alarm.AlarmSound

// The sound and the vibration of one ringing, on the alarm channel, from creation until release.
internal class AlarmPlayer(
    private val context: Context,
    private val alarm: Alarm,
) {
    private val alarmAudio =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private var vibrating = false

    // A system sound can be deleted after it was picked; the app's own sound always opens.
    private val media =
        (alarm.sound as? AlarmSound.System)?.let { runCatching { open(it.uri.toUri()) }.getOrNull() }
            ?: open(defaultSound(context))

    // Zero silences the sound and stops the vibration.
    fun setVolume(volume: Float) {
        media.setVolume(volume, volume)
        vibrate(alarm.vibrate && volume > 0f)
    }

    fun release() {
        vibrate(false)
        media.release()
    }

    private fun open(sound: Uri) =
        MediaPlayer().apply {
            try {
                setAudioAttributes(alarmAudio)
                setDataSource(context, sound)
                isLooping = true
                // Keeps the CPU awake while it plays with the screen off.
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
                start()
            } catch (failure: Exception) {
                release()
                throw failure
            }
        }

    private fun vibrate(on: Boolean) {
        if (on == vibrating) return
        vibrating = on
        if (!on) return vibrator.cancel()
        val pattern = VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(pattern, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, alarmAudio)
        }
    }

    companion object {
        fun defaultSound(context: Context): Uri = "android.resource://${context.packageName}/${R.raw.alarm}".toUri()
    }
}
