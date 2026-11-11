package io.github.guilhermedkdk.wakeguard.alarm

import java.time.Instant

// At most one pending ringing per alarm: scheduling again replaces the previous one.
internal interface AlarmScheduler {
    fun schedule(
        alarmId: Long,
        at: Instant,
    )

    fun cancel(alarmId: Long)
}
