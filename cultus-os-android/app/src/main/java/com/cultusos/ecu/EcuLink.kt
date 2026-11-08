package com.cultusos.ecu

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Any value the ECU did not report is null. Nothing is ever simulated. */
data class EcuSnapshot(
    val timestampMs: Long,
    val speedKmh: Int?,
    val rpm: Int?,
    /** Fuel consumption rate in litres per hour. Null when the vehicle gives no usable signal. */
    val fuelRateLph: Double?,
)

sealed interface LinkState {
    data object Disconnected : LinkState
    data object Connecting : LinkState
    data class Connected(val device: String) : LinkState
    data class Failed(val reason: String) : LinkState
}

/** Independent ECU module boundary. UI and database never depend on a concrete adapter or protocol. */
interface EcuLink {
    val state: StateFlow<LinkState>
    /** Cold flow. Collecting connects, cancelling disconnects. */
    fun snapshots(): Flow<EcuSnapshot>
}
