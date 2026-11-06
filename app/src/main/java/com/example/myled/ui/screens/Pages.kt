package com.example.myled.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myled.R
import com.example.myled.ble.COLOR_MUSIC_MODES
import com.example.myled.ble.EFFECTS
import com.example.myled.ble.LedEffect
import com.example.myled.ble.LedProtocol
import com.example.myled.data.AppTheme
import com.example.myled.data.LedState
import com.example.myled.ui.components.ColorWheel
import com.example.myled.ui.components.LabeledSlider
import com.example.myled.ui.components.MyLedLogo
import com.example.myled.ui.components.NeuButton
import com.example.myled.ui.components.NeuShapeSmall
import com.example.myled.ui.components.NeuSlider
import com.example.myled.ui.components.PowerIcon
import com.example.myled.ui.components.neuPressed
import com.example.myled.ui.theme.NeuTheme

val PageHorizontalPadding = 36.dp

@Composable
private fun PageTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
    )
}

// ------------------------------------------------------------------ Home

@Composable
fun HomePage(state: LedState, onPower: () -> Unit, onColor: (Float, Int) -> Unit, onBrightness: (Int) -> Unit) {
    val c = NeuTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = PageHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MyLedLogo()
            Spacer(Modifier.weight(1f))
            NeuButton(
                onClick = onPower, selected = state.isOn, shape = CircleShape, elevation = 7.dp,
                modifier = Modifier.size(64.dp), contentDescription = stringResource(R.string.power),
            ) {
                PowerIcon(if (state.isOn) c.accent else c.textMuted, Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.weight(0.5f))
        ColorWheel(
            hue = state.hue, saturation = state.saturation, onChange = onColor,
            modifier = Modifier.fillMaxWidth().widthIn(max = 320.dp).aspectRatio(1f),
        )
        Spacer(Modifier.weight(0.6f))
        LabeledSlider(stringResource(R.string.brightness), state.brightness, onBrightness, Modifier.fillMaxWidth())
        Spacer(Modifier.weight(0.6f))
    }
}

// ------------------------------------------------------- ON/OFF indicator

@Composable
fun ModeIndicator(active: Boolean, onClick: () -> Unit) {
    val c = NeuTheme.colors
    NeuButton(onClick = onClick, selected = active, shape = NeuShapeSmall, elevation = 4.dp, modifier = Modifier.size(64.dp, 32.dp)) {
        Text(
            stringResource(if (active) R.string.on else R.string.off),
            style = MaterialTheme.typography.labelLarge, color = if (active) c.accent else c.text,
        )
    }
}

@Composable
private fun ItemButton(item: LedEffect, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = NeuTheme.colors
    NeuButton(onClick = onClick, selected = selected, shape = RoundedCornerShape(12.dp), elevation = 5.dp, modifier = modifier) {
        Text(
            item.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = if (selected) c.accent else c.text, modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

// --------------------------------------------------------------- Effects

@Composable
fun EffectsPage(state: LedState, onToggle: () -> Unit, onSelect: (Int) -> Unit, onSpeed: (Int) -> Unit) {
    val active = state.mode == LedProtocol.Mode.EFFECT
    Column(Modifier.fillMaxSize().padding(horizontal = PageHorizontalPadding - 10.dp)) {
        PageTitle(stringResource(R.string.title_effects))
        Box(Modifier.padding(start = 10.dp)) { ModeIndicator(active, onToggle) }
        Spacer(Modifier.height(28.dp))
        Row(
            Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp).fillMaxHeight()
                .neuPressed(RoundedCornerShape(16.dp), 5.dp),
        ) {
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(start = 28.dp, end = 12.dp, top = 56.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
            ) {
                items(EFFECTS, key = { it.id }) { e ->
                    ItemButton(e, active && state.effect == e.id, { onSelect(e.id) }, Modifier.fillMaxWidth().height(63.dp))
                }
            }
            Column(
                Modifier.width(80.dp).fillMaxHeight().padding(top = 16.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.speed), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                NeuSlider(
                    state.effectSpeed, onSpeed, Modifier.weight(1f), vertical = true,
                    label = stringResource(R.string.speed),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ----------------------------------------------------------- Color music

@Composable
fun ColorMusicPage(state: LedState, onToggle: () -> Unit, onSelect: (Int) -> Unit, onSpeed: (Int) -> Unit) {
    val active = state.mode == LedProtocol.Mode.COLOR_MUSIC
    Column(Modifier.fillMaxSize().padding(horizontal = PageHorizontalPadding - 10.dp)) {
        PageTitle(stringResource(R.string.title_color_music))
        Box(Modifier.padding(start = 10.dp)) { ModeIndicator(active, onToggle) }
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier.fillMaxWidth().neuPressed(RoundedCornerShape(16.dp), 5.dp).padding(vertical = 20.dp),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(COLOR_MUSIC_MODES, key = { it.id }) { m ->
                    ItemButton(m, active && state.colorMusic == m.id, { onSelect(m.id) }, Modifier.widthIn(min = 101.dp).height(44.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LabeledSlider(
                stringResource(R.string.speed), state.colorMusicSpeed, onSpeed,
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
    }
}

// -------------------------------------------------------------- Settings

@Composable
fun SettingsPage(
    state: LedState,
    theme: AppTheme,
    onNumLeds: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onAbout: () -> Unit,
) {
    val c = NeuTheme.colors
    val focus = LocalFocusManager.current
    var text by remember { mutableStateOf(state.numLeds.toString()) }
    LaunchedEffect(state.numLeds) { text = state.numLeds.toString() }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 1..LedProtocol.MAX_LEDS

    fun submit() {
        if (valid) {
            onNumLeds(parsed!!)
            focus.clearFocus()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = PageHorizontalPadding)) {
        PageTitle(stringResource(R.string.title_settings))
        Row(Modifier.fillMaxWidth()) {
            Column {
                Text(stringResource(R.string.number_of_leds), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.size(width = 101.dp, height = 42.dp).neuPressed(RoundedCornerShape(12.dp), 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter(Char::isDigit).take(4) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center, color = if (valid) c.text else c.accent,
                        ),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.leds_range, LedProtocol.MAX_LEDS),
                    style = MaterialTheme.typography.labelMedium, color = c.textMuted,
                    modifier = Modifier.width(101.dp), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                NeuButton(
                    onClick = ::submit, shape = RoundedCornerShape(10.dp), elevation = 4.dp,
                    modifier = Modifier.size(width = 101.dp, height = 32.dp),
                ) {
                    Text(
                        stringResource(R.string.submit), style = MaterialTheme.typography.labelLarge,
                        color = if (valid) c.text else c.textMuted,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                NeuButton(
                    onClick = onToggleTheme, shape = RoundedCornerShape(10.dp), elevation = 4.dp,
                    modifier = Modifier.size(width = 64.dp, height = 32.dp),
                ) {
                    Text(
                        stringResource(if (theme == AppTheme.DARK) R.string.theme_dark else R.string.theme_light),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Spacer(Modifier.weight(1f))
            NeuButton(
                onClick = onAbout, shape = CircleShape, elevation = 4.dp, modifier = Modifier.size(32.dp),
                contentDescription = stringResource(R.string.about),
            ) {
                Text("?", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
