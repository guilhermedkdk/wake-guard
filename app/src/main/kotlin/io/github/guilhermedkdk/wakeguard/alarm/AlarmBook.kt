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
import java.time.ZonedDateTime

/** The alarms kept on the device. Saving, activating or deleting one also schedules or cancels it. */
class AlarmBook internal constructor(
    database: AlarmDatabase,
    private val scheduler: AlarmScheduler,
    // Read on every use: a cached Clock would keep the zone it was created in.
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) {
    private val dao = database.alarms()

    /** Every alarm, earliest time of day first. */
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { rows -> rows.map { it.toAlarm() } }

    suspend fun find(id: Long): Alarm? = dao.find(id)?.toAlarm()

    suspend fun save(alarm: Alarm): Alarm =
        alarm.copy(id = dao.save(alarm.toRow(), alarm.challengeRows())).also(::reschedule)

    suspend fun setActive(
        id: Long,
        active: Boolean,
    ) {
        dao.setActive(id, active)
        find(id)?.let(::reschedule)
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        scheduler.cancel(id)
    }

    /**
     * Settles an alarm whose time has come: a single alarm turns itself off and a repeating one is scheduled
     * for its next day. Returns the alarm to ring, or null when it was deleted or switched off meanwhile.
     */
    suspend fun markRung(id: Long): Alarm? {
        val alarm = find(id)?.takeIf { it.active } ?: return null
        if (alarm.days.isEmpty()) setActive(id, false) else reschedule(alarm)
        return alarm
    }

    /** Schedules every active alarm again, for when the system dropped or misplaced the schedules. */
    suspend fun rescheduleAll() = dao.all().forEach { reschedule(it.toAlarm()) }

    private fun reschedule(alarm: Alarm) {
        val next = alarm.nextRinging(now())
        if (next == null) scheduler.cancel(alarm.id) else scheduler.schedule(alarm.id, next.toInstant())
    }

    companion object {
        @Volatile private var instance: AlarmBook? = null

        /** The one book of the process; receivers and screens must share the same database. */
        fun of(context: Context): AlarmBook =
            instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }

        private fun create(context: Context) =
            AlarmBook(
                Room.databaseBuilder(context, AlarmDatabase::class.java, "alarms.db").build(),
                SystemAlarmScheduler(context),
            )
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
