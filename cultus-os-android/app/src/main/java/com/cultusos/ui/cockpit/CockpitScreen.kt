package com.cultusos.ui.cockpit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cultusos.R
import com.cultusos.domain.FuelResult
import com.cultusos.domain.LiveState
import com.cultusos.ecu.LinkState
import com.cultusos.ui.components.*
import com.cultusos.ui.theme.Cx
import java.util.Locale

private fun f1(v: Double?) = v?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

@Composable
fun CockpitScreen(live: LiveState, overall: FuelResult, link: LinkState, onResetTrip: () -> Unit) {
    val avg = overall.kmPerL
    Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 84.dp)) {
        TopBar(link)
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Speed
            Box(Modifier.weight(0.29f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                ArcGauge(
                    progress = (live.speedKmh ?: 0) / 180f,
                    color = Cx.Ice,
                    modifier = Modifier.fillMaxHeight(0.96f).aspectRatio(1f),
                    stroke = 8.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Label("Speed")
                        Num(live.speedKmh?.toString() ?: "—", 68.sp, weight = FontWeight.ExtraLight)
                        UnitText("km/h")
                    }
                }
            }
            // Car + secondary telemetry
            Column(Modifier.weight(0.42f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painterResource(R.drawable.cultus_render),
                    contentDescription = "My Suzuki Cultus",
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
                    contentScale = ContentScale.Fit,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryTile("Engine", live.rpm?.toString() ?: "—", "rpm", Modifier.weight(1f))
                    TelemetryTile("Trip", f1(live.tripKm.takeIf { it > 0 }), "km", Modifier.weight(1f), onLongPress = onResetTrip)
                    TelemetryTile("Fuel used", f1(live.tripFuelL.takeIf { it > 0 }), "L", Modifier.weight(1f))
                }
            }
            // Economy: instant + average
            Column(Modifier.weight(0.29f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EconomyGauge("Instant", live.instKmPerL, Cx.Eco, Modifier.weight(1f))
                    EconomyGauge("Average", avg, Cx.Frost, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                val delta = if (live.instKmPerL != null && avg != null) live.instKmPerL - avg else null
                Row(
                    Modifier.fillMaxWidth().glass(16.dp).padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label("Instant vs avg", size = 9.sp)
                    val txt = delta?.let { (if (it >= 0) "+" else "−") + f1(kotlin.math.abs(it)) } ?: "—"
                    Num(txt, 18.sp, color = when { delta == null -> Cx.Ink; delta >= 0 -> Cx.Eco; else -> Cx.Amber }, weight = FontWeight.Light)
                }
            }
        }
    }
}

@Composable
private fun EconomyGauge(label: String, value: Double?, color: Color, modifier: Modifier) {
    ArcGauge(((value ?: 0.0) / 25.0).toFloat(), color, modifier.aspectRatio(1f), stroke = 5.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Label(label, size = 9.sp)
            Num(f1(value), 24.sp, weight = FontWeight.Light)
            UnitText("km/L")
        }
    }
}

@Composable
private fun TopBar(link: LinkState) {
    Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row {
            Text("CULTUS ", color = Cx.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = 4.sp)
            Text("OS", color = Cx.Ice, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        }
        val (text, dot) = when (link) {
            LinkState.Disconnected -> "ECU · not connected" to Cx.Ink3
            LinkState.Connecting -> "ECU · connecting" to Cx.Amber
            is LinkState.Connected -> "ECU · ${link.device}" to Cx.Eco
            is LinkState.Failed -> "ECU · failed" to Cx.Alert
        }
        Row(Modifier.glass(99.dp).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Label(text, size = 10.sp)
        }
    }
}
