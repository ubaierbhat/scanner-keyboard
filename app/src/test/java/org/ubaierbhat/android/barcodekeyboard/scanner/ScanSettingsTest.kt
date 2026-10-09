package org.ubaierbhat.android.barcodekeyboard.scanner

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanSettingsTest {

    @Test
    fun defaultsAreOff() {
        val settings = ScanSettings(ApplicationProvider.getApplicationContext())
        assertFalse(settings.continuousScan)
        assertFalse(settings.translateScanActions)
    }

    @Test
    fun valuesPersistAcrossInstances() {
        val first = ScanSettings(ApplicationProvider.getApplicationContext())
        first.continuousScan = true
        first.translateScanActions = true

        val second = ScanSettings(ApplicationProvider.getApplicationContext())
        assertTrue(second.continuousScan)
        assertTrue(second.translateScanActions)

        second.continuousScan = false
        assertEquals(false, first.continuousScan)
    }
}
