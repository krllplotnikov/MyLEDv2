package com.example.myled.ble

import java.util.UUID

/**
 * Протокол обмена с контроллером AddressStrip_Controller (ESP32).
 *
 * Один пакет = 2 или 3 байта: [параметр, значение] или [0x00, hi, lo] для количества светодиодов.
 * Пакет пишется в характеристику [CHARACTERISTIC_UUID] сервиса [SERVICE_UUID].
 */
object LedProtocol {

    /** Имя, под которым контроллер рекламируется (BLEServer_Init("LED")). */
    const val DEVICE_NAME = "LED"

    /** GATTS_SERVICE_UUID_TEST_A = 0x00FF */
    val SERVICE_UUID: UUID = UUID.fromString("000000ff-0000-1000-8000-00805f9b34fb")

    /** GATTS_CHAR_UUID_TEST_A = 0xFF01 (READ | WRITE | NOTIFY) */
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    /** WS2812B_MAX_PIXELS в прошивке. */
    const val MAX_LEDS = 720

    enum class Param(val code: Byte) {
        NUM_LEDS(0x00),
        STATE(0x01),
        MODE(0x02),
        BRIGHTNESS(0x03),
        HUE(0x04),
        SATURATION(0x05),
        EFFECT(0x06),
        EFFECT_SPEED(0x07),
        COLOR_MUSIC(0x08),
        COLOR_MUSIC_SPEED(0x09),
    }

    /** enum AddressStrip_Mode */
    object Mode {
        const val COLOR = 1
        const val EFFECT = 2
        const val COLOR_MUSIC = 3
    }

    fun numLeds(count: Int): ByteArray {
        val v = count.coerceIn(1, MAX_LEDS)
        // Контроллер собирает значение как (data[1] << 8) | data[2] — big-endian.
        return byteArrayOf(Param.NUM_LEDS.code, (v shr 8).toByte(), (v and 0xFF).toByte())
    }

    fun byteParam(param: Param, value: Int): ByteArray {
        require(param != Param.NUM_LEDS) { "Use numLeds()" }
        return byteArrayOf(param.code, value.coerceIn(0, 255).toByte())
    }

    /**
     * Прошивка трактует оттенок как градусы (0..360), но по протоколу передаётся один байт,
     * поэтому оттенки 256..359° недостижимы и прижимаются к 255.
     */
    fun hueByte(hueDegrees: Float): Int = hueDegrees.toInt().coerceIn(0, 255)

    /**
     * Скорость в прошивке — задержка шага анимации в мс (меньше = быстрее).
     * В интерфейсе ползунок 0..100, где 100 = быстрее всего.
     */
    private const val MIN_DELAY_MS = 1
    private const val MAX_DELAY_MS = 100

    fun speedToDelay(speed: Int): Int =
        MAX_DELAY_MS - (speed.coerceIn(0, 100) * (MAX_DELAY_MS - MIN_DELAY_MS) / 100)

    fun delayToSpeed(delay: Int): Int =
        ((MAX_DELAY_MS - delay.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)) * 100 / (MAX_DELAY_MS - MIN_DELAY_MS))
}

/** Эффект прошивки: id = значение enum AddressStrip_Effect. */
data class LedEffect(val id: Int, val name: String)

/** Список эффектов (enum AddressStrip_Effect). Новые эффекты прошивки добавлять сюда. */
val EFFECTS = listOf(
    LedEffect(1, "Rainbow"),
)

/** Список режимов цветомузыки (enum AddressStrip_ColorMusic). */
val COLOR_MUSIC_MODES = listOf(
    LedEffect(1, "Spectre"),
    LedEffect(2, "Rainbow train"),
)
