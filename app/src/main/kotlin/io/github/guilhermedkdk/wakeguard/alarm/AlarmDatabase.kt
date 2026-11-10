package io.github.guilhermedkdk.wakeguard.alarm

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "alarm")
internal data class AlarmRow(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val minuteOfDay: Int,
    // Bit 0 is Monday, bit 6 is Sunday.
    val days: Int,
    val name: String,
    // Null is the app's default sound.
    val soundUri: String?,
    val vibrate: Boolean,
    val risingVolume: Boolean,
    val muteDuringChallenge: Boolean,
    val active: Boolean,
)

@Entity(
    tableName = "alarm_challenge",
    primaryKeys = ["alarmId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = AlarmRow::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ChallengeRow(
    val alarmId: Long,
    val position: Int,
    // Enum names: renaming a ChallengeType or Difficulty needs a migration.
    val type: String,
    val difficulty: String,
    val amount: Int,
)

internal data class AlarmWithChallenges(
    @Embedded val alarm: AlarmRow,
    @Relation(parentColumn = "id", entityColumn = "alarmId") val challenges: List<ChallengeRow>,
)

@Dao
internal abstract class AlarmDao {
    @Transaction
    @Query("SELECT * FROM alarm ORDER BY minuteOfDay, id")
    abstract fun observeAll(): Flow<List<AlarmWithChallenges>>

    @Transaction
    @Query("SELECT * FROM alarm WHERE id = :id")
    abstract suspend fun find(id: Long): AlarmWithChallenges?

    @Query("UPDATE alarm SET active = :active WHERE id = :id")
    abstract suspend fun setActive(
        id: Long,
        active: Boolean,
    )

    @Query("DELETE FROM alarm WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun save(
        alarm: AlarmRow,
        challenges: List<ChallengeRow>,
    ): Long {
        val id =
            if (alarm.id == 0L) {
                insert(alarm)
            } else {
                update(alarm)
                alarm.id
            }
        clearChallenges(id)
        insert(challenges.map { it.copy(alarmId = id) })
        return id
    }

    @Insert
    protected abstract suspend fun insert(alarm: AlarmRow): Long

    @Update
    protected abstract suspend fun update(alarm: AlarmRow)

    @Insert
    protected abstract suspend fun insert(challenges: List<ChallengeRow>)

    @Query("DELETE FROM alarm_challenge WHERE alarmId = :alarmId")
    protected abstract suspend fun clearChallenges(alarmId: Long)
}

@Database(entities = [AlarmRow::class, ChallengeRow::class], version = 1)
internal abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
}
