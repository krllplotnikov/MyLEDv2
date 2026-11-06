package com.example.myled

import com.example.myled.ble.LedProtocol
import com.example.myled.ble.LedProtocol.Param
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LedProtocolTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun hueIsTwoBytesBigEndian() {
        assertArrayEquals(bytes(0x04, 0x00, 0x00), LedProtocol.hue(0))
        assertArrayEquals(bytes(0x04, 0x00, 0xFF), LedProtocol.hue(255))
        assertArrayEquals(bytes(0x04, 0x01, 0x2C), LedProtocol.hue(300))
        assertArrayEquals(bytes(0x04, 0x01, 0x67), LedProtocol.hue(359))
    }

    @Test
    fun hueDegreesWrapsFullCircle() {
        assertEquals(0, LedProtocol.hueDegrees(360f))
        assertEquals(359, LedProtocol.hueDegrees(359.9f))
        assertEquals(120, LedProtocol.hueDegrees(120.4f))
    }

    @Test
    fun numLedsIsTwoBytesBigEndian() {
        assertArrayEquals(bytes(0x00, 0x00, 60), LedProtocol.numLeds(60))
        assertArrayEquals(bytes(0x00, 0x02, 0xD0), LedProtocol.numLeds(720))
        assertArrayEquals(bytes(0x00, 0x02, 0xD0), LedProtocol.numLeds(5000))
    }

    @Test
    fun saturationStaysOneByte() {
        assertArrayEquals(bytes(0x05, 100), LedProtocol.byteParam(Param.SATURATION, 100))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hueRejectsSingleByteEncoding() {
        LedProtocol.byteParam(Param.HUE, 10)
    }
}
