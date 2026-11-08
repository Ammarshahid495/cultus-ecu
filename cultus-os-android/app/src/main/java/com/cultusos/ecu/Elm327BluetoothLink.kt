package com.cultusos.ecu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.coroutines.coroutineContext

/**
 * Candidate link for an ELM327-compatible Bluetooth adapter using standard OBD-II Mode 01 PIDs.
 *
 * UNVERIFIED for the 2010 Cultus: the original Suzuki ECU may not answer 010C / 010D / 0110.
 * If it does not, the fields stay null and the UI shows dashes. Replace [poll] once the adapter
 * and protocol investigation is complete.
 */
@SuppressLint("MissingPermission")
class Elm327BluetoothLink(
    private val adapter: BluetoothAdapter?,
    private val address: String,
) : EcuLink {

    private val _state = MutableStateFlow<LinkState>(LinkState.Disconnected)
    override val state: StateFlow<LinkState> = _state.asStateFlow()

    override fun snapshots(): Flow<EcuSnapshot> = flow {
        _state.value = LinkState.Connecting
        var socket: BluetoothSocket? = null
        try {
            val bt = adapter ?: error("Bluetooth is not available")
            val device = bt.getRemoteDevice(address)
            bt.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SPP)
            socket.connect()
            val input = socket.inputStream
            val output = socket.outputStream

            suspend fun ask(cmd: String): String {
                while (input.available() > 0) input.read()
                output.write((cmd + "\r").toByteArray())
                output.flush()
                val sb = StringBuilder()
                val deadline = System.currentTimeMillis() + 2_000
                while (System.currentTimeMillis() < deadline) {
                    while (input.available() > 0) {
                        val c = input.read().toChar()
                        if (c == '>') return sb.toString()
                        sb.append(c)
                    }
                    delay(15)
                }
                return sb.toString()
            }

            for (c in listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATSP0")) ask(c)
            _state.value = LinkState.Connected(device.name ?: address)

            while (coroutineContext.isActive) {
                val speed = bytesAfter(ask("010D"), "410D", 1)?.get(0)
                val rpm = bytesAfter(ask("010C"), "410C", 2)?.let { (it[0] * 256 + it[1]) / 4 }
                val mafGs = bytesAfter(ask("0110"), "4110", 2)?.let { (it[0] * 256 + it[1]) / 100.0 }
                emit(EcuSnapshot(System.currentTimeMillis(), speed, rpm, mafGs?.let(::mafToLitresPerHour)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = LinkState.Failed(e.message ?: "Connection failed")
        } finally {
            runCatching { socket?.close() }
            if (_state.value !is LinkState.Failed) _state.value = LinkState.Disconnected
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Petrol: stoichiometric air/fuel 14.7, density about 745 g/L. */
        fun mafToLitresPerHour(gramsPerSecond: Double) = gramsPerSecond * 3600.0 / (14.7 * 745.0)

        fun bytesAfter(response: String, prefix: String, count: Int): IntArray? {
            val clean = response.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
            val i = clean.indexOf(prefix)
            if (i < 0 || clean.length < i + prefix.length + 2 * count) return null
            return IntArray(count) {
                val s = i + prefix.length + 2 * it
                clean.substring(s, s + 2).toInt(16)
            }
        }
    }
}
