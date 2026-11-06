package com.example.myled

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myled.ble.BleLedClient
import com.example.myled.ble.ConnectionState
import com.example.myled.ble.LedProtocol
import com.example.myled.ble.LedProtocol.Param
import com.example.myled.data.AppTheme
import com.example.myled.data.LedState
import com.example.myled.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LedViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    val ble = BleLedClient(app)

    private val _state = MutableStateFlow(prefs.loadState())
    val state: StateFlow<LedState> = _state.asStateFlow()

    private val _theme = MutableStateFlow(prefs.theme)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    val connection: StateFlow<ConnectionState> get() = ble.connection

    init {
        ble.onReady = { syncAll() }
    }

    /** Вызывается, когда разрешения выданы и Bluetooth включён. */
    fun onBluetoothReady() {
        if (ble.connection.value != ConnectionState.Disconnected) return
        val last = prefs.lastDeviceAddress
        if (last != null) ble.connect(last) else ble.startScan()
    }

    fun connectTo(address: String) {
        prefs.lastDeviceAddress = address
        ble.connect(address)
    }

    fun disconnect() {
        ble.disconnect()
    }

    // ------------------------------------------------------------ actions

    fun togglePower() {
        val on = !_state.value.isOn
        update { it.copy(isOn = on) }
        send(Param.STATE, if (on) 1 else 0)
    }

    fun setColor(hue: Float, saturation: Int) {
        val prev = _state.value
        update { it.copy(hue = hue, saturation = saturation, mode = LedProtocol.Mode.COLOR) }
        ensureOnAndMode(prev, LedProtocol.Mode.COLOR)
        if (LedProtocol.hueByte(hue) != LedProtocol.hueByte(prev.hue)) send(Param.HUE, LedProtocol.hueByte(hue))
        if (saturation != prev.saturation) send(Param.SATURATION, saturation)
    }

    fun setBrightness(value: Int) {
        if (value == _state.value.brightness) return
        update { it.copy(brightness = value) }
        send(Param.BRIGHTNESS, value)
    }

    fun selectEffect(id: Int) {
        val prev = _state.value
        update { it.copy(effect = id, mode = LedProtocol.Mode.EFFECT) }
        send(Param.EFFECT, id)
        ensureOnAndMode(prev, LedProtocol.Mode.EFFECT)
    }

    fun setEffectSpeed(speed: Int) {
        if (speed == _state.value.effectSpeed) return
        update { it.copy(effectSpeed = speed) }
        send(Param.EFFECT_SPEED, LedProtocol.speedToDelay(speed))
    }

    fun selectColorMusic(id: Int) {
        val prev = _state.value
        update { it.copy(colorMusic = id, mode = LedProtocol.Mode.COLOR_MUSIC) }
        send(Param.COLOR_MUSIC, id)
        ensureOnAndMode(prev, LedProtocol.Mode.COLOR_MUSIC)
    }

    fun setColorMusicSpeed(speed: Int) {
        if (speed == _state.value.colorMusicSpeed) return
        update { it.copy(colorMusicSpeed = speed) }
        send(Param.COLOR_MUSIC_SPEED, LedProtocol.speedToDelay(speed))
    }

    /** Переключатель ON/OFF на страницах эффектов и цветомузыки. OFF возвращает режим цвета. */
    fun toggleMode(mode: Int) {
        val prev = _state.value
        val newMode = if (prev.mode == mode) LedProtocol.Mode.COLOR else mode
        update { it.copy(mode = newMode) }
        ensureOnAndMode(prev, newMode)
    }

    fun setNumLeds(count: Int) {
        val v = count.coerceIn(1, LedProtocol.MAX_LEDS)
        update { it.copy(numLeds = v) }
        ble.send(LedProtocol.numLeds(v))
    }

    fun toggleTheme() {
        val t = if (_theme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
        _theme.value = t
        prefs.theme = t
    }

    // ------------------------------------------------------------ helpers

    private fun ensureOnAndMode(prev: LedState, mode: Int) {
        if (prev.mode != mode) send(Param.MODE, mode)
        if (!prev.isOn) {
            update { it.copy(isOn = true) }
            send(Param.STATE, 1)
        }
    }

    private fun update(block: (LedState) -> LedState) {
        _state.value = block(_state.value)
        prefs.saveState(_state.value)
    }

    private fun send(param: Param, value: Int) = ble.send(LedProtocol.byteParam(param, value))

    /** Контроллер не умеет отдавать состояние, поэтому при подключении отправляем всё. */
    private fun syncAll() {
        val s = _state.value
        ble.clearQueue()
        ble.send(LedProtocol.numLeds(s.numLeds))
        send(Param.BRIGHTNESS, s.brightness)
        send(Param.HUE, LedProtocol.hueByte(s.hue))
        send(Param.SATURATION, s.saturation)
        send(Param.EFFECT, s.effect)
        send(Param.EFFECT_SPEED, LedProtocol.speedToDelay(s.effectSpeed))
        send(Param.COLOR_MUSIC, s.colorMusic)
        send(Param.COLOR_MUSIC_SPEED, LedProtocol.speedToDelay(s.colorMusicSpeed))
        send(Param.MODE, s.mode)
        send(Param.STATE, if (s.isOn) 1 else 0)
    }

    override fun onCleared() {
        ble.release()
    }
}
