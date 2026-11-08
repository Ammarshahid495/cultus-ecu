package com.cultusos

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cultusos.data.*
import com.cultusos.domain.*
import com.cultusos.ecu.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FuelUi(
    val windowKm: Int = 100,
    val window: FuelResult = FuelResult(0.0, 0.0),
    val period: Period = Period.DAILY,
    val points: List<HistoryPoint> = emptyList(),
    val overall: FuelResult = FuelResult(0.0, 0.0),
    val hasData: Boolean = false,
)

data class ReminderUi(
    val type: String,
    val intervalKm: Int,
    val enabled: Boolean,
    /** Null until a service of this type has been recorded. */
    val remainingKm: Double?,
    val lastDateMs: Long?,
)

data class VehicleUi(
    val services: List<ServiceRecord> = emptyList(),
    val reminders: List<ReminderUi> = emptyList(),
    val totalKm: Double = 0.0,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val tracker = TripTracker(dao)
    private var linkJob: Job? = null
    private var stateJob: Job? = null

    val live: StateFlow<LiveState> = tracker.live

    private val _link = MutableStateFlow<LinkState>(LinkState.Disconnected)
    val link: StateFlow<LinkState> = _link.asStateFlow()

    private val windowKm = MutableStateFlow(100)
    private val period = MutableStateFlow(Period.DAILY)

    val fuel: StateFlow<FuelUi> = combine(dao.logsNewestFirst(), windowKm, period) { logs, w, p ->
        FuelUi(
            windowKm = w,
            window = FuelMath.lastWindow(logs, w.toDouble()),
            period = p,
            points = FuelMath.history(logs, p),
            overall = FuelMath.total(logs),
            hasData = logs.isNotEmpty(),
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelUi())

    val vehicle: StateFlow<VehicleUi> = combine(dao.services(), dao.reminders(), dao.totalKm()) { s, r, km ->
        VehicleUi(
            services = s,
            reminders = r.sortedBy { SERVICE_TYPES.indexOf(it.type) }.map { rem ->
                val last = s.firstOrNull { it.type == rem.type }
                ReminderUi(
                    type = rem.type,
                    intervalKm = rem.intervalKm,
                    enabled = rem.enabled,
                    remainingKm = last?.let { rem.intervalKm - (km - it.loggedKmAtService) },
                    lastDateMs = last?.dateMs,
                )
            },
            totalKm = km,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehicleUi())

    init {
        viewModelScope.launch {
            if (dao.reminderCount() == 0) DEFAULT_REMINDERS.forEach { dao.upsertReminder(it) }
        }
    }

    fun setWindow(km: Int) { windowKm.value = km }
    fun setPeriod(p: Period) { period.value = p }
    fun resetTrip() = tracker.resetTrip()

    fun addService(type: String, note: String) = viewModelScope.launch {
        dao.insertService(ServiceRecord(type = type, dateMs = System.currentTimeMillis(),
            loggedKmAtService = vehicle.value.totalKm, note = note))
    }

    fun deleteService(r: ServiceRecord) = viewModelScope.launch { dao.deleteService(r) }

    fun setInterval(type: String, km: Int) = viewModelScope.launch {
        val cur = vehicle.value.reminders.firstOrNull { it.type == type } ?: return@launch
        dao.upsertReminder(Reminder(type, km.coerceAtLeast(500), cur.enabled))
    }

    fun setEnabled(type: String, on: Boolean) = viewModelScope.launch {
        val cur = vehicle.value.reminders.firstOrNull { it.type == type } ?: return@launch
        dao.upsertReminder(Reminder(type, cur.intervalKm, on))
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<Pair<String, String>> = try {
        getApplication<Application>().getSystemService(BluetoothManager::class.java)
            ?.adapter?.bondedDevices?.map { (it.name ?: it.address) to it.address } ?: emptyList()
    } catch (e: SecurityException) {
        emptyList()
    }

    fun connect(address: String) {
        disconnect()
        val adapter = getApplication<Application>().getSystemService(BluetoothManager::class.java)?.adapter
        val l = Elm327BluetoothLink(adapter, address)
        stateJob = viewModelScope.launch { l.state.collect { _link.value = it } }
        linkJob = viewModelScope.launch {
            try {
                l.snapshots().collect { tracker.onSnapshot(it) }
            } finally {
                withContext(NonCancellable) { tracker.onDisconnected() }
            }
        }
    }

    fun disconnect() {
        linkJob?.cancel()
        stateJob?.cancel()
        _link.value = LinkState.Disconnected
    }
}
