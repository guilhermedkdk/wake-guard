package io.github.guilhermedkdk.wakeguard.alarm

import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTest {
    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    // 2026-11-10 is a Tuesday.
    private fun at(
        day: Int,
        hour: Int,
        minute: Int,
    ) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, saoPaulo)

    private fun alarm(time: String) =
        Alarm(time = LocalTime.parse(time), challenges = listOf(ChallengeSpec(ChallengeType.MATH)))

    @Test
    fun `single alarm rings today when its time is still ahead`() {
        assertEquals(at(10, 7, 0), alarm("07:00").nextRinging(at(10, 6, 30)))
    }

    @Test
    fun `single alarm rings tomorrow when its time already passed today`() {
        assertEquals(at(11, 7, 0), alarm("07:00").nextRinging(at(10, 7, 1)))
    }

    @Test
    fun `repeating alarm waits for its next chosen day`() {
        val weekends = alarm("07:00").copy(days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))

        assertEquals(at(14, 7, 0), weekends.nextRinging(at(10, 6, 30)))
    }

    @Test
    fun `inactive alarm has no next ringing`() {
        assertNull(alarm("07:00").copy(active = false).nextRinging(at(10, 6, 30)))
    }

    @Test
    fun `alarm whose only day is today waits a week once its time passed`() {
        val tuesdays = alarm("07:00").copy(days = setOf(DayOfWeek.TUESDAY))

        assertEquals(at(17, 7, 0), tuesdays.nextRinging(at(10, 7, 0)))
    }

    @Test
    fun `time skipped by daylight saving rings at the first minute that exists`() {
        // Lisbon jumps from 01:00 to 02:00 on 2026-03-29.
        val lisbon = ZoneId.of("Europe/Lisbon")
        val midnight = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, lisbon)

        assertEquals(
            ZonedDateTime.of(2026, 3, 29, 2, 30, 0, 0, lisbon),
            alarm("01:30").nextRinging(midnight),
        )
    }

    @Test
    fun `name longer than 40 characters is refused`() {
        assertThrows(IllegalArgumentException::class.java) { alarm("07:00").copy(name = "a".repeat(41)) }
    }

    @Test
    fun `alarm needs between one and five challenges`() {
        val math = ChallengeSpec(ChallengeType.MATH)

        assertThrows(IllegalArgumentException::class.java) { alarm("07:00").copy(challenges = emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { alarm("07:00").copy(challenges = List(6) { math }) }
    }
}
