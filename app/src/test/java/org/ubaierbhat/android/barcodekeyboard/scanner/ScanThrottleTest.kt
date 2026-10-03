package org.ubaierbhat.android.barcodekeyboard.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanThrottleTest {

    @Test
    fun firstCallAllows() {
        var now = 0L
        val throttle = ScanThrottle { now }
        assertTrue(throttle.allow())
    }

    @Test
    fun immediateSecondCallIsBlocked() {
        var now = 0L
        val throttle = ScanThrottle { now }
        assertTrue(throttle.allow())
        assertFalse(throttle.allow())
    }

    @Test
    fun allowsAgainAfterOneThousandMilliseconds() {
        var now = 100L
        val throttle = ScanThrottle { now }
        assertTrue(throttle.allow())
        now = 1099L
        assertFalse(throttle.allow())
        now = 1100L
        assertTrue(throttle.allow())
    }

    @Test
    fun windowMeasuresFromLastAllowedCall() {
        var now = 0L
        val throttle = ScanThrottle { now }
        assertTrue(throttle.allow())
        now = 500L
        assertFalse(throttle.allow())
        now = 1400L
        assertTrue(throttle.allow())
        now = 2000L
        assertFalse(throttle.allow())
        now = 2400L
        assertTrue(throttle.allow())
    }

    @Test
    fun resetReallowsImmediately() {
        var now = 0L
        val throttle = ScanThrottle { now }
        assertTrue(throttle.allow())
        assertFalse(throttle.allow())
        throttle.reset()
        assertTrue(throttle.allow())
    }
}
