package com.example.myled.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myled.LedViewModel
import com.example.myled.R
import com.example.myled.ble.ConnectionState
import com.example.myled.ble.FoundDevice
import com.example.myled.ble.LedProtocol
import com.example.myled.ui.components.CloseIcon
import com.example.myled.ui.components.MyLedMainLogo
import com.example.myled.ui.components.NeuButton
import com.example.myled.ui.components.neuRaised
import com.example.myled.ui.theme.NeuTheme
import kotlinx.coroutines.launch

@Composable
fun AppScreen(vm: LedViewModel, onRequestBluetooth: () -> Unit) {
    val c = NeuTheme.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val pager = rememberPagerState { 4 }
    val scope = rememberCoroutineScope()
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showDevices by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ConnectionBar(connection) {
                onRequestBluetooth()
                vm.ble.startScan()
                showDevices = true
            }
            HorizontalPager(
                state = pager,
                userScrollEnabled = false, // жесты колеса и ползунков не должны листать страницы
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 3,
            ) { page ->
                when (page) {
                    0 -> HomePage(state, vm::togglePower, vm::setColor, vm::setBrightness)
                    1 -> EffectsPage(state, { vm.toggleMode(LedProtocol.Mode.EFFECT) }, vm::selectEffect, vm::setEffectSpeed)
                    2 -> ColorMusicPage(state, { vm.toggleMode(LedProtocol.Mode.COLOR_MUSIC) }, vm::selectColorMusic, vm::setColorMusicSpeed)
                    else -> SettingsPage(state, theme, vm::setNumLeds, vm::toggleTheme) { showAbout = true }
                }
            }
            BottomNav(pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        }

        AnimatedVisibility(showAbout, enter = fadeIn(), exit = fadeOut()) {
            AboutOverlay { showAbout = false }
        }
    }

    if (showDevices) {
        DevicesDialog(
            vm = vm,
            connection = connection,
            onDismiss = { vm.ble.stopScan(); showDevices = false },
            onPick = { vm.connectTo(it.address); showDevices = false },
        )
    }
}

@Composable
private fun ConnectionBar(connection: ConnectionState, onClick: () -> Unit) {
    val c = NeuTheme.colors
    val (text, dot) = when (connection) {
        is ConnectionState.Connected -> stringResource(R.string.status_connected, connection.name) to Color(0xFF5DB06A)
        ConnectionState.Connecting -> stringResource(R.string.status_connecting) to c.accent
        ConnectionState.Disconnected -> stringResource(R.string.status_disconnected) to Color(0xFFB0574F)
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = PageHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BottomNav(current: Int, onSelect: (Int) -> Unit) {
    val c = NeuTheme.colors
    val labels = listOf(R.string.nav_home, R.string.nav_effects, R.string.nav_color_music, R.string.nav_settings)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PageHorizontalPadding, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, label ->
            val selected = i == current
            NeuButton(
                onClick = { onSelect(i) }, selected = selected, shape = RoundedCornerShape(12.dp), elevation = 6.dp,
                modifier = Modifier.size(width = 64.dp, height = 88.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(top = 16.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(label), style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center, maxLines = 2,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(width = 26.dp, height = 3.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (selected) c.accent else c.textMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutOverlay(onClose: () -> Unit) {
    val c = NeuTheme.colors
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
    BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize().background(c.background.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .safeDrawingPadding(),
    ) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clip(CircleShape).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            CloseIcon(c.text, Modifier.size(30.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PageHorizontalPadding).padding(top = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MyLedMainLogo()
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.version, version), style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        }
    }
}

@Composable
private fun DevicesDialog(
    vm: LedViewModel,
    connection: ConnectionState,
    onDismiss: () -> Unit,
    onPick: (FoundDevice) -> Unit,
) {
    val c = NeuTheme.colors
    val devices by vm.ble.devices.collectAsStateWithLifecycle()
    val scanning by vm.ble.scanning.collectAsStateWithLifecycle()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().neuRaised(RoundedCornerShape(20.dp), 6.dp).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.devices_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (scanning) CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(16.dp))
            if (!vm.ble.isBluetoothEnabled) {
                Text(stringResource(R.string.bt_disabled), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            } else if (devices.isEmpty()) {
                Text(stringResource(R.string.devices_empty), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(devices, key = { it.address }) { d ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(d) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.name ?: d.address, style = MaterialTheme.typography.bodyLarge,
                                color = if (d.isLedController) c.accent else c.text,
                            )
                            Text(d.address, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                        }
                        Text("${d.rssi} dBm", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)) {
                if (connection !is ConnectionState.Disconnected) {
                    NeuButton(onClick = { vm.disconnect(); onDismiss() }, shape = RoundedCornerShape(10.dp), elevation = 4.dp,
                        modifier = Modifier.height(36.dp)) {
                        Text(stringResource(R.string.disconnect), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp))
                    }
                }
                NeuButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), elevation = 4.dp, modifier = Modifier.height(36.dp)) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
        }
    }
}
