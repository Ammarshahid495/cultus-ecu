package com.cultusos.ui.vehicle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cultusos.ReminderUi
import com.cultusos.VehicleUi
import com.cultusos.data.SERVICE_TYPES
import com.cultusos.data.ServiceRecord
import com.cultusos.ecu.LinkState
import com.cultusos.ui.components.*
import com.cultusos.ui.theme.Cx
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehicleScreen(
    ui: VehicleUi,
    link: LinkState,
    bondedDevices: () -> List<Pair<String, String>>,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onAddService: (String, String) -> Unit,
    onDeleteService: (ServiceRecord) -> Unit,
    onInterval: (String, Int) -> Unit,
    onEnabled: (String, Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    var picker by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) picker = true }

    fun pickAdapter() {
        val needs = Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        if (needs) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else picker = true
    }

    Row(
        Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 52.dp, bottom = 84.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Label("Engine")
                Spec("Engine", "G10B · 993 cc · 3-cyl")
                Spec("Fuel system", "EFI")
                Spec("Transmission", "Manual")
                Spec("ECU", "Original Suzuki")
            }
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Label("Diagnostics & adapter")
                val status = when (link) {
                    LinkState.Disconnected -> "Not connected"
                    LinkState.Connecting -> "Connecting…"
                    is LinkState.Connected -> link.device
                    is LinkState.Failed -> "Failed: ${link.reason}"
                }
                Spec("Adapter", status)
                Spec("Fault codes", "Not read yet")
                Spacer(Modifier.weight(1f))
                val connected = link is LinkState.Connected || link == LinkState.Connecting
                Pill(if (connected) "Disconnect" else "Choose adapter", enabled = true) {
                    if (connected) onDisconnect() else pickAdapter()
                }
                Text("Fault-code reading is added after the adapter and protocol investigation.", color = Cx.Ink3, fontSize = 10.sp)
            }
        }

        Card(Modifier.weight(1.15f).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Label("Service history")
                Pill("Add record", enabled = true) { adding = true }
            }
            if (ui.services.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No service records yet.", color = Cx.Ink3, fontSize = 13.sp)
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(ui.services, key = { it.id }) { r ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(r.type, color = Cx.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.dateMs)) +
                                    if (r.note.isNotBlank()) " · ${r.note}" else "", color = Cx.Ink3, fontSize = 11.sp)
                            }
                            Text("Delete", color = Cx.Ink3, fontSize = 11.sp, modifier = Modifier.clickable { onDeleteService(r) }.padding(6.dp))
                        }
                        HorizontalDivider(color = Color.White.copy(0.06f))
                    }
                }
            }
        }

        Card(Modifier.weight(1f).fillMaxHeight()) {
            Label("Reminders")
            LazyColumn(Modifier.weight(1f)) {
                items(ui.reminders, key = { it.type }) { r ->
                    ReminderRow(r, onInterval, onEnabled)
                    HorizontalDivider(color = Color.White.copy(0.06f))
                }
            }
        }
    }

    if (picker) {
        val devices = remember { bondedDevices() }
        AlertDialog(
            onDismissRequest = { picker = false },
            containerColor = Cx.Graphite,
            title = { Text("Paired Bluetooth adapters", color = Cx.Ink) },
            text = {
                if (devices.isEmpty()) Text("No paired devices. Pair the OBD adapter in Android settings first.", color = Cx.Ink2)
                else LazyColumn { items(devices) { (name, addr) ->
                    Text(name, color = Cx.Ink, modifier = Modifier.fillMaxWidth().clickable { picker = false; onConnect(addr) }.padding(vertical = 12.dp))
                } }
            },
            confirmButton = { TextButton({ picker = false }) { Text("Close") } },
        )
    }

    if (adding) {
        var type by remember { mutableStateOf(SERVICE_TYPES.first()) }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = Cx.Graphite,
            title = { Text("Add service record", color = Cx.Ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SERVICE_TYPES.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { Chip(it, type == it) { type = it } } }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
                }
            },
            confirmButton = { TextButton({ onAddService(type, note.trim()); adding = false }) { Text("Save") } },
            dismissButton = { TextButton({ adding = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ReminderRow(r: ReminderUi, onInterval: (String, Int) -> Unit, onEnabled: (String, Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.type, color = Cx.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Switch(r.enabled, { onEnabled(r.type, it) }, modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(checkedTrackColor = Cx.IceDeep, checkedThumbColor = Color.White))
        }
        val used = r.remainingKm?.let { ((r.intervalKm - it) / r.intervalKm).toFloat().coerceIn(0f, 1f) } ?: 0f
        val due = r.remainingKm != null && r.remainingKm < 800
        LinearProgressIndicator(progress = { used }, Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = if (due) Cx.Amber else Cx.Ice, trackColor = Color.White.copy(0.08f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(r.remainingKm?.let { if (it >= 0) "%,.0f km left".format(Locale.US, it) else "Overdue by %,.0f km".format(Locale.US, -it) }
                ?: "No record yet", color = if (due) Cx.Amber else Cx.Ink3, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Step("−") { onInterval(r.type, r.intervalKm - 500) }
                Text("%,d km".format(Locale.US, r.intervalKm), color = Cx.Ink2, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                Step("+") { onInterval(r.type, r.intervalKm + 500) }
            }
        }
    }
}

@Composable
private fun Step(text: String, onClick: () -> Unit) {
    Text(text, color = Cx.Ink, fontSize = 14.sp, modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 2.dp))
}

@Composable
private fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun Spec(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, color = Cx.Ink2, fontSize = 12.sp)
        Text(v, color = Cx.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pill(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(text, color = if (enabled) Cx.Ink else Cx.Ink3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(0.08f)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp))
}
