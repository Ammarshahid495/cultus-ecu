package com.cultusos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Cx {
    val Carbon = Color(0xFF06080B)
    val Graphite = Color(0xFF0C1117)
    val Ink = Color(0xFFEEF3F7)
    val Ink2 = Color(0xFF97A4B0)
    val Ink3 = Color(0xFF5B6772)
    val Ice = Color(0xFF7FD6FF)
    val IceDeep = Color(0xFF3AA7E0)
    val Eco = Color(0xFF6FF0C0)
    val Frost = Color(0xFFE4F8FF)
    val Amber = Color(0xFFFFB454)
    val Alert = Color(0xFFFF6B6B)
}

@Composable
fun CultusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cx.Ice,
            background = Cx.Carbon,
            surface = Cx.Graphite,
            onBackground = Cx.Ink,
            onSurface = Cx.Ink,
        ),
        content = content,
    )
}
