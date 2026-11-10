package io.github.guilhermedkdk.wakeguard.alarm

import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

/** A configured wake-up time. An id of 0 means it was never saved. */
data class Alarm(
    val id: Long = 0,
    val time: LocalTime,
    val days: Set<DayOfWeek> = emptySet(),
    val name: String = "",
    val sound: AlarmSound = AlarmSound.Default,
    val vibrate: Boolean = true,
    val risingVolume: Boolean = false,
    val challenges: List<ChallengeSpec>,
    val muteDuringChallenge: Boolean = false,
    val active: Boolean = true,
) {
    init {
        require(name.length <= MAX_NAME_LENGTH) { "name is longer than $MAX_NAME_LENGTH characters" }
        require(challenges.size in 1..MAX_CHALLENGES) { "an alarm has 1 to $MAX_CHALLENGES challenges" }
    }

    fun nextRinging(now: ZonedDateTime): ZonedDateTime? {
        if (!active) return null
        // Day 7 covers an alarm whose only weekday is today and already passed.
        return (0L..7L)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { days.isEmpty() || it.dayOfWeek in days }
            .map { ZonedDateTime.of(it, time, now.zone) }
            .first { it > now }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MAX_CHALLENGES = 5
    }
}

sealed interface AlarmSound {
    data object Default : AlarmSound

    data class System(
        val uri: String,
    ) : AlarmSound
}
