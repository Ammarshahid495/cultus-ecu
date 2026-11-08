package com.cultusos.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** One logging interval while driving. Only written when BOTH distance and fuel were measured. */
@Entity(tableName = "fuel_log")
data class FuelLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMs: Long,
    val distanceKm: Double,
    val fuelL: Double,
)

@Entity(tableName = "service_record")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val dateMs: Long,
    /** Total logged distance (all fuel_log rows) at the moment of service. */
    val loggedKmAtService: Double,
    val note: String = "",
)

@Entity(tableName = "reminder")
data class Reminder(
    @PrimaryKey val type: String,
    val intervalKm: Int,
    val enabled: Boolean = true,
)

val SERVICE_TYPES = listOf("Engine oil", "Oil filter", "Air filter", "Spark plugs", "Coolant")

/** Editable defaults, not vehicle data. Confirm against the Cultus service manual. */
val DEFAULT_REMINDERS = listOf(
    Reminder("Engine oil", 5000),
    Reminder("Oil filter", 5000),
    Reminder("Air filter", 15000),
    Reminder("Spark plugs", 30000),
    Reminder("Coolant", 40000),
)

@Dao
interface AppDao {
    @Insert suspend fun insertLog(log: FuelLog)

    @Query("SELECT * FROM fuel_log ORDER BY timeMs DESC")
    fun logsNewestFirst(): Flow<List<FuelLog>>

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM fuel_log")
    fun totalKm(): Flow<Double>

    @Insert suspend fun insertService(record: ServiceRecord)

    @Delete suspend fun deleteService(record: ServiceRecord)

    @Query("SELECT * FROM service_record ORDER BY dateMs DESC, id DESC")
    fun services(): Flow<List<ServiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReminder(r: Reminder)

    @Query("SELECT * FROM reminder")
    fun reminders(): Flow<List<Reminder>>

    @Query("SELECT COUNT(*) FROM reminder")
    suspend fun reminderCount(): Int
}

@Database(
    entities = [FuelLog::class, ServiceRecord::class, Reminder::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "cultus.db")
                .build().also { instance = it }
        }
    }
}
