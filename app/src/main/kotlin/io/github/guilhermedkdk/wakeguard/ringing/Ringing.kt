package io.github.guilhermedkdk.wakeguard.ringing

import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import java.time.Duration
import java.time.Instant

/** What a ringing in progress shows: the alarm and the challenge it is on. */
data class RingingState(
    val alarm: Alarm,
    val rehearsal: Boolean,
    val challengeIndex: Int,
) {
    val challenge: ChallengeSpec get() = alarm.challenges[challengeIndex]
}

internal enum class RingingOutcome { COMPLETED, EXPIRED }

// The rules of one ringing. Time only comes in through the arguments, so the caller owns the clock.
internal class Ringing(
    private val alarm: Alarm,
    private val rehearsal: Boolean,
    private val startedAt: Instant,
    // Null rings until every challenge is completed.
    private val maxDuration: Duration?,
) {
    private var challengeIndex = 0
    private var lastInteraction: Instant? = null

    var outcome: RingingOutcome? = null
        private set

    val state get() = RingingState(alarm, rehearsal, challengeIndex)

    fun challengeCompleted(now: Instant) {
        if (outcome != null) return
        lastInteraction = now
        if (challengeIndex == alarm.challenges.lastIndex) outcome = RingingOutcome.COMPLETED else challengeIndex++
    }

    fun interacted(now: Instant) {
        lastInteraction = now
    }

    fun tick(now: Instant) {
        if (outcome == null && maxDuration != null && Duration.between(startedAt, now) >= maxDuration) {
            outcome = RingingOutcome.EXPIRED
        }
    }

    fun muted(now: Instant): Boolean {
        val last = lastInteraction ?: return false
        return alarm.muteDuringChallenge && Duration.between(last, now) < MUTE_AFTER_INTERACTION
    }

    fun volume(now: Instant): Float {
        if (!alarm.risingVolume) return 1f
        val risen = Duration.between(startedAt, now).toMillis().toFloat() / RISE.toMillis()
        return RISE_FROM + (1f - RISE_FROM) * risen.coerceIn(0f, 1f)
    }

    private companion object {
        val MUTE_AFTER_INTERACTION: Duration = Duration.ofSeconds(20)
        val RISE: Duration = Duration.ofSeconds(30)
        const val RISE_FROM = 0.2f
    }
}
