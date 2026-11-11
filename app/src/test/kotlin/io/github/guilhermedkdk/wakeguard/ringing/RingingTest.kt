package io.github.guilhermedkdk.wakeguard.ringing

import io.github.guilhermedkdk.wakeguard.alarm.Alarm
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

class RingingTest {
    private val start = Instant.parse("2026-11-10T10:00:00Z")
    private val math = ChallengeSpec(ChallengeType.MATH)
    private val shake = ChallengeSpec(ChallengeType.SHAKE)
    private val alarm = Alarm(time = LocalTime.of(7, 0), challenges = listOf(math, shake))

    private fun after(seconds: Long) = start.plusSeconds(seconds)

    private fun ringing(
        alarm: Alarm = this.alarm,
        maxDuration: Duration? = Duration.ofMinutes(2),
    ) = Ringing(alarm, rehearsal = false, startedAt = start, maxDuration = maxDuration)

    @Test
    fun `starts on the first challenge of the alarm`() {
        assertEquals(RingingState(alarm, rehearsal = false, challengeIndex = 0), ringing().state)
    }

    @Test
    fun `completing a challenge moves on to the next one`() {
        val ringing = ringing()

        ringing.challengeCompleted(after(10))

        assertEquals(shake, ringing.state.challenge)
        assertNull(ringing.outcome)
    }

    @Test
    fun `completing every challenge completes the ringing`() {
        val ringing = ringing()

        ringing.challengeCompleted(after(10))
        ringing.challengeCompleted(after(20))

        assertEquals(RingingOutcome.COMPLETED, ringing.outcome)
    }

    @Test
    fun `ringing expires when the maximum duration passes`() {
        val ringing = ringing()

        ringing.tick(after(119))
        assertNull(ringing.outcome)

        ringing.tick(after(120))
        assertEquals(RingingOutcome.EXPIRED, ringing.outcome)
    }

    @Test
    fun `ringing without a maximum duration never expires`() {
        val ringing = ringing(maxDuration = null)

        ringing.tick(after(24 * 60 * 60))

        assertNull(ringing.outcome)
    }

    @Test
    fun `the outcome does not change once the ringing is over`() {
        val expired = ringing()
        expired.tick(after(120))
        expired.challengeCompleted(after(121))
        expired.challengeCompleted(after(122))
        assertEquals(RingingOutcome.EXPIRED, expired.outcome)

        val completed = ringing()
        completed.challengeCompleted(after(10))
        completed.challengeCompleted(after(20))
        completed.tick(after(120))
        assertEquals(RingingOutcome.COMPLETED, completed.outcome)
    }

    @Test
    fun `sound keeps playing through interaction unless the alarm mutes during the challenge`() {
        val ringing = ringing()

        ringing.interacted(after(10))

        assertFalse(ringing.muted(after(11)))
    }

    @Test
    fun `interaction mutes for 20 seconds when the alarm mutes during the challenge`() {
        val ringing = ringing(alarm.copy(muteDuringChallenge = true))
        assertFalse(ringing.muted(after(5)))

        ringing.interacted(after(10))

        assertTrue(ringing.muted(after(29)))
        assertFalse(ringing.muted(after(30)))
    }

    @Test
    fun `muting does not hold back the maximum duration`() {
        val ringing = ringing(alarm.copy(muteDuringChallenge = true))

        ringing.interacted(after(115))
        ringing.tick(after(120))

        assertEquals(RingingOutcome.EXPIRED, ringing.outcome)
    }

    @Test
    fun `volume is full from the start unless the alarm rises`() {
        assertEquals(1f, ringing().volume(after(0)), 0.001f)
    }

    @Test
    fun `rising volume climbs from 20 percent to full in 30 seconds`() {
        val ringing = ringing(alarm.copy(risingVolume = true))

        assertEquals(0.2f, ringing.volume(after(0)), 0.001f)
        assertEquals(0.6f, ringing.volume(after(15)), 0.001f)
        assertEquals(1f, ringing.volume(after(30)), 0.001f)
        assertEquals(1f, ringing.volume(after(90)), 0.001f)
    }
}
