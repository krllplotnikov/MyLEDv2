package com.example.myled.data

import android.content.Context
import com.example.myled.ble.LedProtocol

/**
 * Состояние ленты на стороне приложения. Контроллер не отдаёт своё состояние по BLE,
 * поэтому приложение хранит его локально и отправляет целиком при подключении.
 *
 * Значения по умолчанию совпадают с ADDRESS_STRIP_DEFAULT_* в прошивке.
 */
data class LedState(
    val numLeds: Int = 60,
    val isOn: Boolean = true,
    val mode: Int = LedProtocol.Mode.COLOR,
    /** 0..100 */
    val brightness: Int = 100,
    /** Градусы 0..360 (на контроллер уходит не больше 255, см. LedProtocol.hueByte). */
    val hue: Float = 0f,
    /** 0..100 */
    val saturation: Int = 100,
    val effect: Int = 1,
    /** 0..100, 100 — быстрее всего */
    val effectSpeed: Int = LedProtocol.delayToSpeed(25),
    val colorMusic: Int = 1,
    /** 0..100, 100 — быстрее всего */
    val colorMusicSpeed: Int = LedProtocol.delayToSpeed(5),
)

enum class AppTheme { LIGHT, DARK }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("myled", Context.MODE_PRIVATE)

    fun loadState(): LedState {
        val d = LedState()
        return LedState(
            numLeds = sp.getInt("numLeds", d.numLeds),
            isOn = sp.getBoolean("isOn", d.isOn),
            mode = sp.getInt("mode", d.mode),
            brightness = sp.getInt("brightness", d.brightness),
            hue = sp.getFloat("hue", d.hue),
            saturation = sp.getInt("saturation", d.saturation),
            effect = sp.getInt("effect", d.effect),
            effectSpeed = sp.getInt("effectSpeed", d.effectSpeed),
            colorMusic = sp.getInt("colorMusic", d.colorMusic),
            colorMusicSpeed = sp.getInt("colorMusicSpeed", d.colorMusicSpeed),
        )
    }

    fun saveState(s: LedState) {
        sp.edit()
            .putInt("numLeds", s.numLeds)
            .putBoolean("isOn", s.isOn)
            .putInt("mode", s.mode)
            .putInt("brightness", s.brightness)
            .putFloat("hue", s.hue)
            .putInt("saturation", s.saturation)
            .putInt("effect", s.effect)
            .putInt("effectSpeed", s.effectSpeed)
            .putInt("colorMusic", s.colorMusic)
            .putInt("colorMusicSpeed", s.colorMusicSpeed)
            .apply()
    }

    var theme: AppTheme
        get() = AppTheme.entries.getOrElse(sp.getInt("theme", 0)) { AppTheme.LIGHT }
        set(value) = sp.edit().putInt("theme", value.ordinal).apply()

    var lastDeviceAddress: String?
        get() = sp.getString("lastDevice", null)
        set(value) = sp.edit().putString("lastDevice", value).apply()
}
