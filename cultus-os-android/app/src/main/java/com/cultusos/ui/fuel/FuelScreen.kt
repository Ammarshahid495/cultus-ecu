package com.cultusos.ui.fuel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cultusos.FuelUi
import com.cultusos.domain.FuelMath
import com.cultusos.domain.HistoryPoint
import com.cultusos.domain.Period
import com.cultusos.ui.components.*
import com.cultusos.ui.theme.Cx
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

private fun f1(v: Double?) = v?.let { String.format(Locale.US, "%.1f", it) } ?: "—"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuelScreen(ui: FuelUi, onWindow: (Int) -> Unit, onPeriod: (Period) -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(start = 28.dp, end = 28.dp, top = 52.dp, bottom = 84.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Window card
        Column(Modifier.weight(0.37f).fillMaxHeight().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("Efficiency window")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FuelMath.WINDOWS_KM.forEach { km ->
                    Chip("Last %,d km".format(Locale.US, km), ui.windowKm == km) { onWindow(km) }
                }
            }
            Spacer(Modifier.weight(1f))
            Label("Average over window")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Num(f1(ui.window.kmPerL), 60.sp, color = Cx.Eco)
                UnitText("km/L", Modifier.padding(bottom = 10.dp))
            }
            if (ui.hasData && ui.window.distanceKm < ui.windowKm - 0.5) {
                Text("Only ${f1(ui.window.distanceKm)} km logged so far.", color = Cx.Amber, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Distance", ui.window.distanceKm.takeIf { ui.hasData }?.let { "${f1(it)} km" }, Modifier.weight(1f))
                Stat("Fuel", ui.window.fuelL.takeIf { ui.hasData }?.let { "${f1(it)} L" }, Modifier.weight(1f))
                Stat("L/100 km", f1(ui.window.lPer100), Modifier.weight(1f))
            }
            Text("Average = total distance ÷ total fuel, never the mean of readings.", color = Cx.Ink3, fontSize = 10.sp)
        }

        // History card
        Column(Modifier.weight(0.63f).fillMaxHeight().glass().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Label("History · km/L")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Period.entries.forEach { p -> Chip(p.label, ui.period == p) { onPeriod(p) } }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (ui.points.isEmpty()) {
                    Text("No trips recorded yet. Drive with the ECU adapter connected and history will appear here.",
                        color = Cx.Ink3, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                } else {
                    HistoryChart(ui.points, line = ui.period == Period.OVERALL, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Label(label, size = 9.sp)
        Num(value ?: "—", 18.sp, weight = FontWeight.Light)
    }
}

@Composable
private fun HistoryChart(points: List<HistoryPoint>, line: Boolean, modifier: Modifier) {
    val tm = rememberTextMeasurer()
    val axis = TextStyle(color = Cx.Ink3, fontSize = 10.sp)
    Canvas(modifier) {
        val left = 30.dp.toPx()
        val bottom = 20.dp.toPx()
        val top = 6.dp.toPx()
        val vals = points.map { it.kmPerL }
        val lo = floor(minOf(8.0, vals.min()) / 2) * 2
        val hi = ceil(maxOf(16.0, vals.max()) / 2) * 2
        val h = size.height - top - bottom
        fun y(v: Double) = (top + (hi - v) / (hi - lo) * h).toFloat()

        var g = lo
        while (g <= hi) {
            drawLine(Color.White.copy(0.07f), Offset(left, y(g)), Offset(size.width, y(g)), 1f)
            drawText(tm, g.toInt().toString(), Offset(0f, y(g) - 7.dp.toPx()), axis)
            g += 2
        }
        val slot = (size.width - left) / points.size
        if (line) {
            val path = Path()
            points.forEachIndexed { i, p ->
                val x = left + slot * (i + 0.5f)
                if (i == 0) path.moveTo(x, y(p.kmPerL)) else path.lineTo(x, y(p.kmPerL))
            }
            val area = Path().apply {
                addPath(path)
                lineTo(left + slot * (points.size - 0.5f), top + h)
                lineTo(left + slot * 0.5f, top + h)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(Cx.Eco.copy(0.35f), Color.Transparent), top, top + h))
            drawPath(path, Cx.Eco, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val last = points.last()
            drawCircle(Cx.Eco, 4.5.dp.toPx(), Offset(left + slot * (points.size - 0.5f), y(last.kmPerL)))
        } else {
            points.forEachIndexed { i, p ->
                val bw = slot * 0.6f
                val x = left + slot * i + slot * 0.2f
                val yy = y(p.kmPerL)
                drawRoundRect(
                    if (i == points.lastIndex) Cx.Eco else Cx.Ice.copy(0.55f),
                    Offset(x, yy), Size(bw, top + h - yy), CornerRadius(4.dp.toPx()),
                )
            }
        }
        val step = ceil(points.size / 8.0).toInt().coerceAtLeast(1)
        points.forEachIndexed { i, p ->
            if (i % step == 0 || i == points.lastIndex) {
                drawText(tm, p.label, Offset(left + slot * i + slot * 0.1f, size.height - bottom + 4.dp.toPx()), axis)
            }
        }
    }
}
