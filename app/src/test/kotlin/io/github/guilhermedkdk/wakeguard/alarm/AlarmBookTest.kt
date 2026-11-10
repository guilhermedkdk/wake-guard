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
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class AlarmBookTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AlarmDatabase::class.java)
            .build()
    private val book = AlarmBook(database)

    @After
    fun close() = database.close()

    private fun alarm(time: String) =
        Alarm(time = LocalTime.parse(time), challenges = listOf(ChallengeSpec(ChallengeType.MATH)))

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
