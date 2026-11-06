package com.example.myled.ble

import java.util.UUID

/**
 * Протокол обмена с контроллером AddressStrip_Controller (ESP32).
 *
 * Один пакет = 2 или 3 байта: [параметр, значение] или [параметр, hi, lo] для
 * двухбайтовых параметров (количество светодиодов и оттенок).
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

    private val TWO_BYTE_PARAMS = setOf(Param.NUM_LEDS, Param.HUE)

    /** Контроллер собирает двухбайтовое значение как (data[1] << 8) | data[2] — big-endian. */
    private fun wordParam(param: Param, value: Int): ByteArray =
        byteArrayOf(param.code, (value shr 8).toByte(), (value and 0xFF).toByte())

    fun numLeds(count: Int): ByteArray = wordParam(Param.NUM_LEDS, count.coerceIn(1, MAX_LEDS))

    /** Оттенок в градусах 0..359. */
    fun hue(hueDegrees: Int): ByteArray = wordParam(Param.HUE, hueDegrees.coerceIn(0, 359))

    fun byteParam(param: Param, value: Int): ByteArray {
        require(param !in TWO_BYTE_PARAMS) { "$param is a two-byte parameter" }
        return byteArrayOf(param.code, value.coerceIn(0, 255).toByte())
    }

    /** Градусы с цветового круга (0..360) → целое значение для контроллера (0..359). */
    fun hueDegrees(hue: Float): Int = hue.toInt().mod(360)

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
