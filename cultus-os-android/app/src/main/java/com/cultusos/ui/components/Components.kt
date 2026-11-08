package com.cultusos.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cultusos.ui.theme.Cx

/** Translucent glass surface. Backdrop blur is approximated by layered gradients over the glow background. */
fun Modifier.glass(radius: Dp = 20.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.035f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.22f), Color.White.copy(0.04f))), shape)
}

@Composable
fun CockpitBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Cx.Carbon)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Cx.Ice.copy(0.20f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.62f), radius = size.width * 0.45f))
                drawRect(Brush.radialGradient(listOf(Cx.Ice.copy(0.08f), Color.Transparent),
                    center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.40f))
            },
    )
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = Cx.Ink2, size: TextUnit = 10.sp) {
    Text(text.uppercase(), modifier, color = color, fontSize = size,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp, maxLines = 1)
}

@Composable
fun Num(text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = Cx.Ink,
        weight: FontWeight = FontWeight.ExtraLight) {
    Text(text, modifier, color = color, fontSize = size, fontWeight = weight,
        fontFeatureSettings = "tnum", maxLines = 1)
}

@Composable
fun UnitText(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, color = Cx.Ink2, fontSize = 11.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
}

/** 270 degree arc gauge with soft glow. [progress] is 0..1. */
@Composable
fun ArcGauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 7.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "arc")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke * 1.5f)) {
            val w = stroke.toPx()
            drawArc(Color.White.copy(0.08f), 135f, 270f, false, style = Stroke(w, cap = StrokeCap.Round))
            if (p > 0.001f) {
                drawArc(color.copy(0.28f), 135f, 270f * p, false, style = Stroke(w * 2.2f, cap = StrokeCap.Round))
                drawArc(color, 135f, 270f * p, false, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun TelemetryTile(label: String, value: String, unit: String, modifier: Modifier = Modifier, onLongPress: (() -> Unit)? = null) {
    Column(
        modifier
            .glass(18.dp)
            .pointerInput(onLongPress) { detectTapGestures(onLongPress = { onLongPress?.invoke() }) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Label(label)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Num(value, 26.sp, weight = FontWeight.Light)
            UnitText(unit, Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    Text(
        text,
        Modifier
            .clip(shape)
            .background(if (selected) Cx.Ice.copy(0.18f) else Color.White.copy(0.06f))
            .border(1.dp, if (selected) Cx.Ice.copy(0.5f) else Color.White.copy(0.06f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = if (selected) Color.White else Cx.Ink2,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Floating glass navigation with a spring-driven sliding indicator. */
@Composable
fun GlassNavBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val labels = listOf("Cockpit", "Fuel", "Vehicle")
    BoxWithConstraints(
        modifier
            .width(340.dp)
            .height(54.dp)
            .clip(CircleShape)
            .background(Color(0xFF141A22).copy(0.62f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.25f), Color.White.copy(0.05f))), CircleShape)
            .padding(5.dp),
    ) {
        val itemW = maxWidth / 3
        val x by animateDpAsState(itemW * selected, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "pill")
        Box(
            Modifier
                .offset(x = x)
                .width(itemW)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(0.24f), Color.White.copy(0.08f)))),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, l ->
                val on = i == selected
                val tint = if (on) Color.White else Cx.Ink2
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavIcon(i, tint)
                    Spacer(Modifier.width(6.dp))
                    Text(l, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun NavIcon(index: Int, tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val s = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        when (index) {
            0 -> {
                drawArc(tint, 180f, 180f, false, Offset(w * 0.08f, w * 0.15f), androidx.compose.ui.geometry.Size(w * 0.84f, w * 0.84f), style = s)
                drawLine(tint, Offset(w * 0.5f, w * 0.57f), Offset(w * 0.72f, w * 0.34f), 1.8.dp.toPx(), StrokeCap.Round)
            }
            1 -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, w * 0.08f)
                    cubicTo(w * 0.5f, w * 0.08f, w * 0.85f, w * 0.45f, w * 0.85f, w * 0.65f)
                    cubicTo(w * 0.85f, w * 0.88f, w * 0.15f, w * 0.88f, w * 0.15f, w * 0.65f)
                    cubicTo(w * 0.15f, w * 0.45f, w * 0.5f, w * 0.08f, w * 0.5f, w * 0.08f)
                }
                drawPath(p, tint, style = s)
            }
            else -> {
                drawCircle(tint, w * 0.22f, Offset(w * 0.66f, w * 0.34f), style = s)
                drawLine(tint, Offset(w * 0.5f, w * 0.5f), Offset(w * 0.14f, w * 0.88f), 1.8.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
