package com.cultusos.domain

import com.cultusos.data.AppDao
import com.cultusos.data.FuelLog
import com.cultusos.ecu.EcuSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveState(
    val speedKmh: Int? = null,
    val rpm: Int? = null,
    val instKmPerL: Double? = null,
    val tripKm: Double = 0.0,
    val tripFuelL: Double = 0.0,
)

/**
 * Turns ECU snapshots into live values and persisted fuel logs.
 * Distance is only counted when fuel rate is known for the same instant, so economy is never skewed.
 */
class TripTracker(private val dao: AppDao) {
    private val _live = MutableStateFlow(LiveState())
    val live: StateFlow<LiveState> = _live.asStateFlow()

    private var lastTs = 0L
    private var lastFlush = 0L
    private var pendingKm = 0.0
    private var pendingFuel = 0.0
    private var smoothed: Double? = null

    suspend fun onSnapshot(s: EcuSnapshot) {
        val speed = s.speedKmh
        val rate = s.fuelRateLph
        var cur = _live.value

        if (speed != null && rate != null && lastTs > 0L) {
            val dtH = (s.timestampMs - lastTs).coerceIn(0L, 5_000L) / 3_600_000.0
            val km = speed * dtH
            val l = rate * dtH
            pendingKm += km
            pendingFuel += l
            cur = cur.copy(tripKm = cur.tripKm + km, tripFuelL = cur.tripFuelL + l)
        }
        lastTs = s.timestampMs
        if (lastFlush == 0L) lastFlush = s.timestampMs

        val inst = if (speed != null && rate != null && speed >= 5 && rate > 0.05) {
            val raw = speed / rate
            smoothed = smoothed?.let { it + 0.25 * (raw - it) } ?: raw
            smoothed
        } else {
            smoothed = null
            null
        }
        _live.value = cur.copy(speedKmh = speed, rpm = s.rpm, instKmPerL = inst)

        if (s.timestampMs - lastFlush >= 10_000L) flush(s.timestampMs)
    }

    suspend fun flush(now: Long = System.currentTimeMillis()) {
        if (pendingKm > 0.0 && pendingFuel > 0.0) {
            dao.insertLog(FuelLog(timeMs = now, distanceKm = pendingKm, fuelL = pendingFuel))
        }
        pendingKm = 0.0
        pendingFuel = 0.0
        lastFlush = now
    }

    suspend fun onDisconnected() {
        flush()
        lastTs = 0L
        lastFlush = 0L
        smoothed = null
        _live.value = _live.value.copy(speedKmh = null, rpm = null, instKmPerL = null)
    }

    fun resetTrip() {
        _live.value = _live.value.copy(tripKm = 0.0, tripFuelL = 0.0)
    }
}
