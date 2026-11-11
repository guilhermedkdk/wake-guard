package io.github.guilhermedkdk.wakeguard.ringing

import android.app.Application
import android.content.Context
import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Starts ringings and tells the rest of the app whether one is in progress. */
class RingingController private constructor(
    private val context: Application,
) {
    private val current = MutableStateFlow<RingingState?>(null)

    /** The ringing in progress, or null when nothing rings. */
    val state: StateFlow<RingingState?> = current.asStateFlow()

    // The service while it rings; the ringing screen sends its events here.
    internal var service: RingingService? = null

    // A rehearsed alarm may never have been saved, so it reaches the service in memory.
    private var rehearsed: Alarm? = null

    /** Rings a saved alarm for real. Ignored while another ringing is in progress. */
    fun start(alarmId: Long) = context.startForegroundService(RingingService.intent(context, alarmId))

    /** Rings [alarm] as a rehearsal, which can be ended at any time. */
    fun rehearse(alarm: Alarm) {
        rehearsed = alarm
        context.startForegroundService(RingingService.intent(context, RingingService.REHEARSAL))
    }

    internal fun takeRehearsed(): Alarm? = rehearsed.also { rehearsed = null }

    internal fun publish(state: RingingState?) {
        current.value = state
    }

    companion object {
        @Volatile private var instance: RingingController? = null

        /** The one controller of the process, shared by the service, the screens and the receivers. */
        fun of(context: Context): RingingController =
            instance ?: synchronized(this) {
                instance ?: RingingController(context.applicationContext as Application).also { instance = it }
            }
    }
}
