package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.R

@RunWith(RobolectricTestRunner::class)
class ScannerViewContinuousTest {

    @Test
    fun switchReflectsPersistedStateAndPersistsToggling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = ScanSettings(context)
        settings.continuousScan = false

        var reported: Boolean? = null
        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = { reported = it },
        )

        val switch = view.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch)
        assertFalse(switch.isChecked)

        switch.isChecked = true
        assertTrue(settings.continuousScan)
        assertTrue(view.isContinuousEnabled)
        assertEquals(true, reported)

        val second = ScannerView(context)
        second.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )
        val secondSwitch = second.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch)
        assertTrue("new scanner view restores persisted state on start-equivalent sync",
            second.isContinuousEnabled)
        settings.continuousScan = false
    }
}
