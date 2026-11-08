package com.cultusos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cultusos.ui.cockpit.CockpitScreen
import com.cultusos.ui.components.CockpitBackground
import com.cultusos.ui.components.GlassNavBar
import com.cultusos.ui.fuel.FuelScreen
import com.cultusos.ui.theme.CultusTheme
import com.cultusos.ui.vehicle.VehicleScreen

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        setContent { CultusTheme { AppRoot(vm) } }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val live by vm.live.collectAsStateWithLifecycle()
    val link by vm.link.collectAsStateWithLifecycle()
    val fuel by vm.fuel.collectAsStateWithLifecycle()
    val vehicle by vm.vehicle.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        CockpitBackground()
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "screen",
        ) { t ->
            when (t) {
                0 -> CockpitScreen(live, fuel.overall, link, vm::resetTrip)
                1 -> FuelScreen(fuel, vm::setWindow, vm::setPeriod)
                else -> VehicleScreen(
                    ui = vehicle, link = link,
                    bondedDevices = vm::bondedDevices,
                    onConnect = vm::connect, onDisconnect = vm::disconnect,
                    onAddService = { type, note -> vm.addService(type, note) },
                    onDeleteService = { vm.deleteService(it) },
                    onInterval = { type, km -> vm.setInterval(type, km) },
                    onEnabled = { type, on -> vm.setEnabled(type, on) },
                )
            }
        }
        GlassNavBar(tab, { tab = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}
