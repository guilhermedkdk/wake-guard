package io.github.guilhermedkdk.wakeguard.alarm

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import io.github.guilhermedkdk.wakeguard.challenge.Difficulty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class AlarmBookTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AlarmDatabase::class.java)
            .build()
    private val scheduler = FakeScheduler()

    // 2026-11-10 is a Tuesday.
    private var now = at(10, 6, 30)
    private val book = AlarmBook(database, scheduler) { now }

    @After
    fun close() = database.close()

    private fun at(
        day: Int,
        hour: Int,
        minute: Int,
    ) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, ZoneId.of("America/Sao_Paulo"))

    private fun alarm(time: String) =
        Alarm(time = LocalTime.parse(time), challenges = listOf(ChallengeSpec(ChallengeType.MATH)))

    @Test
    fun `saved alarm is scheduled for its next ringing`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            assertEquals(mapOf(saved.id to at(10, 7, 0).toInstant()), scheduler.scheduled)
        }

    @Test
    fun `editing an alarm moves its schedule`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            book.save(saved.copy(time = LocalTime.of(6, 0)))

            assertEquals(mapOf(saved.id to at(11, 6, 0).toInstant()), scheduler.scheduled)
        }

    @Test
    fun `alarm saved inactive is not scheduled`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            book.save(saved.copy(active = false))

            assertEquals(emptyMap<Long, Instant>(), scheduler.scheduled)
        }

    @Test
    fun `deactivating cancels the schedule and reactivating restores it`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            book.setActive(saved.id, false)
            assertEquals(emptyMap<Long, Instant>(), scheduler.scheduled)

            book.setActive(saved.id, true)
            assertEquals(mapOf(saved.id to at(10, 7, 0).toInstant()), scheduler.scheduled)
        }

    @Test
    fun `deleting an alarm cancels its schedule`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            book.delete(saved.id)

            assertEquals(emptyMap<Long, Instant>(), scheduler.scheduled)
        }

    @Test
    fun `single alarm that rang turns itself off`() =
        runTest {
            val saved = book.save(alarm("07:00"))
            now = at(10, 7, 0)

            assertEquals(saved, book.markRung(saved.id))

            assertEquals(saved.copy(active = false), book.find(saved.id))
            assertEquals(emptyMap<Long, Instant>(), scheduler.scheduled)
        }

    @Test
    fun `repeating alarm that rang is scheduled for its next day`() =
        runTest {
            val saved = book.save(alarm("07:00").copy(days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)))
            now = at(10, 7, 0)

            book.markRung(saved.id)

            assertEquals(saved, book.find(saved.id))
            assertEquals(mapOf(saved.id to at(12, 7, 0).toInstant()), scheduler.scheduled)
        }

    @Test
    fun `alarm deleted or switched off before its time does not ring`() =
        runTest {
            val off = book.save(alarm("07:00").copy(active = false))

            assertNull(book.markRung(off.id))
            assertNull(book.markRung(999))
        }

    @Test
    fun `rescheduling everything restores what the system forgot and skips inactive alarms`() =
        runTest {
            val single = book.save(alarm("06:00"))
            val weekly = book.save(alarm("07:00").copy(days = setOf(DayOfWeek.SATURDAY)))
            book.save(alarm("08:00").copy(active = false))
            scheduler.scheduled.clear()
            now = at(10, 6, 30)

            book.rescheduleAll()

            assertEquals(
                mapOf(single.id to at(11, 6, 0).toInstant(), weekly.id to at(14, 7, 0).toInstant()),
                scheduler.scheduled,
            )
        }

    @Test
    fun `saved alarm comes back exactly as it was saved`() =
        runTest {
            val saved =
                book.save(
                    Alarm(
                        time = LocalTime.of(6, 45),
                        days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                        name = "Academia",
                        sound = AlarmSound.System("content://media/ringtone/7"),
                        vibrate = false,
                        risingVolume = true,
                        challenges =
                            listOf(
                                ChallengeSpec(ChallengeType.SHAKE, Difficulty.HARD, 50),
                                ChallengeSpec(ChallengeType.MATH, Difficulty.EASY, 2),
                            ),
                        muteDuringChallenge = true,
                    ),
                )

            assertNotEquals(0L, saved.id)
            assertEquals(saved, book.find(saved.id))
        }

    @Test
    fun `alarms are listed from the earliest time of day to the latest`() =
        runTest {
            book.save(alarm("22:00"))
            book.save(alarm("06:30"))
            book.save(alarm("13:15"))

            assertEquals(listOf("06:30", "13:15", "22:00"), book.alarms.first().map { it.time.toString() })
        }

    @Test
    fun `saving an alarm again replaces it, challenges included`() =
        runTest {
            val original =
                book.save(
                    alarm("07:00").copy(
                        challenges = listOf(ChallengeSpec(ChallengeType.MATH), ChallengeSpec(ChallengeType.MEMORY)),
                    ),
                )

            val edited =
                original.copy(
                    time = LocalTime.of(8, 0),
                    challenges = listOf(ChallengeSpec(ChallengeType.TYPING)),
                )
            book.save(edited)

            assertEquals(listOf(edited), book.alarms.first())
        }

    @Test
    fun `deactivated alarm stays in the book`() =
        runTest {
            val saved = book.save(alarm("07:00"))

            book.setActive(saved.id, false)

            assertEquals(listOf(saved.copy(active = false)), book.alarms.first())
        }

    @Test
    fun `deleted alarm is gone`() =
        runTest {
            val kept = book.save(alarm("07:00"))
            val deleted = book.save(alarm("08:00"))

            book.delete(deleted.id)

            assertEquals(listOf(kept), book.alarms.first())
            assertNull(book.find(deleted.id))
        }
}

private class FakeScheduler : AlarmScheduler {
    val scheduled = mutableMapOf<Long, Instant>()

    override fun schedule(
        alarmId: Long,
        at: Instant,
    ) {
        scheduled[alarmId] = at
    }

    override fun cancel(alarmId: Long) {
        scheduled.remove(alarmId)
    }
}
