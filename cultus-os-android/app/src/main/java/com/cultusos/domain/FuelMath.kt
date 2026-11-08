package com.cultusos.domain

import com.cultusos.data.FuelLog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Economy is always total distance / total fuel. Never the mean of individual readings. */
data class FuelResult(val distanceKm: Double, val fuelL: Double) {
    val kmPerL: Double? get() = if (fuelL > 1e-6 && distanceKm > 0.0) distanceKm / fuelL else null
    val lPer100: Double? get() = kmPerL?.let { 100.0 / it }
}

enum class Period(val label: String) { DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), OVERALL("Overall") }

data class HistoryPoint(val label: String, val kmPerL: Double)

object FuelMath {
    val WINDOWS_KM = listOf(50, 100, 200, 500, 1000, 2000, 3000)

    fun total(logs: List<FuelLog>) = FuelResult(logs.sumOf { it.distanceKm }, logs.sumOf { it.fuelL })

    /** Walks back from the newest log until [windowKm] is covered; the oldest log is used proportionally. */
    fun lastWindow(newestFirst: List<FuelLog>, windowKm: Double): FuelResult {
        var km = 0.0
        var fuel = 0.0
        for (log in newestFirst) {
            if (km >= windowKm) break
            if (log.distanceKm <= 0.0) continue
            val take = minOf(log.distanceKm, windowKm - km)
            km += take
            fuel += log.fuelL * (take / log.distanceKm)
        }
        return FuelResult(km, fuel)
    }

    fun history(newestFirst: List<FuelLog>, period: Period, zone: ZoneId = ZoneId.systemDefault()): List<HistoryPoint> {
        val byDay: Map<LocalDate, List<FuelLog>> = newestFirst.groupBy {
            Instant.ofEpochMilli(it.timeMs).atZone(zone).toLocalDate()
        }
        return when (period) {
            Period.DAILY -> grouped(byDay, { it.toEpochDay() }, { LocalDate.ofEpochDay(it).dayOfMonth.toString() }, 14)
            Period.WEEKLY -> grouped(
                byDay,
                { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay() },
                { LocalDate.ofEpochDay(it).let { d -> "${d.dayOfMonth}/${d.monthValue}" } },
                12,
            )
            Period.MONTHLY -> grouped(
                byDay,
                { it.year * 12L + (it.monthValue - 1) },
                { Month.of((it % 12).toInt() + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                12,
            )
            Period.OVERALL -> {
                var km = 0.0
                var fuel = 0.0
                byDay.toSortedMap().entries.mapNotNull { (day, logs) ->
                    km += logs.sumOf { it.distanceKm }
                    fuel += logs.sumOf { it.fuelL }
                    if (fuel > 1e-6) HistoryPoint("${day.dayOfMonth}/${day.monthValue}", km / fuel) else null
                }.takeLast(120)
            }
        }
    }

    private fun grouped(
        byDay: Map<LocalDate, List<FuelLog>>,
        key: (LocalDate) -> Long,
        label: (Long) -> String,
        keep: Int,
    ): List<HistoryPoint> =
        byDay.entries
            .groupBy({ key(it.key) }, { it.value })
            .toSortedMap()
            .entries.toList()
            .takeLast(keep)
            .mapNotNull { (k, lists) ->
                total(lists.flatten()).kmPerL?.let { HistoryPoint(label(k), it) }
            }
}
