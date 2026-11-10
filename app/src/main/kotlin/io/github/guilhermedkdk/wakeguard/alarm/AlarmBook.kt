package io.github.guilhermedkdk.wakeguard.alarm

import android.content.Context
import androidx.room.Room
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeSpec
import io.github.guilhermedkdk.wakeguard.challenge.ChallengeType
import io.github.guilhermedkdk.wakeguard.challenge.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

/** The alarms kept on the device. */
class AlarmBook internal constructor(
    database: AlarmDatabase,
) {
    private val dao = database.alarms()

    /** Every alarm, earliest time of day first. */
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { rows -> rows.map { it.toAlarm() } }

    suspend fun find(id: Long): Alarm? = dao.find(id)?.toAlarm()

    suspend fun save(alarm: Alarm): Alarm = alarm.copy(id = dao.save(alarm.toRow(), alarm.challengeRows()))

    suspend fun setActive(
        id: Long,
        active: Boolean,
    ) = dao.setActive(id, active)

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        fun open(context: Context): AlarmBook =
            AlarmBook(Room.databaseBuilder(context, AlarmDatabase::class.java, "alarms.db").build())
    }
}

private fun AlarmWithChallenges.toAlarm() =
    Alarm(
        id = alarm.id,
        time = LocalTime.of(alarm.minuteOfDay / 60, alarm.minuteOfDay % 60),
        days = DayOfWeek.entries.filterTo(mutableSetOf()) { alarm.days and it.bit != 0 },
        name = alarm.name,
        sound = alarm.soundUri?.let(AlarmSound::System) ?: AlarmSound.Default,
        vibrate = alarm.vibrate,
        risingVolume = alarm.risingVolume,
        challenges =
            challenges.sortedBy { it.position }.map {
                ChallengeSpec(ChallengeType.valueOf(it.type), Difficulty.valueOf(it.difficulty), it.amount)
            },
        muteDuringChallenge = alarm.muteDuringChallenge,
        active = alarm.active,
    )

private fun Alarm.toRow() =
    AlarmRow(
        id = id,
        minuteOfDay = time.hour * 60 + time.minute,
        days = days.fold(0) { bits, day -> bits or day.bit },
        name = name,
        soundUri = (sound as? AlarmSound.System)?.uri,
        vibrate = vibrate,
        risingVolume = risingVolume,
        muteDuringChallenge = muteDuringChallenge,
        active = active,
    )

private fun Alarm.challengeRows() =
    challenges.mapIndexed { position, spec ->
        ChallengeRow(id, position, spec.type.name, spec.difficulty.name, spec.amount)
    }

private val DayOfWeek.bit get() = 1 shl ordinal
