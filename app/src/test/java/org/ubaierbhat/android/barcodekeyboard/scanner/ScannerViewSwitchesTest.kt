package org.ubaierbhat.android.barcodekeyboard.scanner

import android.content.Context
import android.view.ContextThemeWrapper
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
class ScannerViewSwitchesTest {

    private fun themedContext(): Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext<Context>(),
        R.style.Theme_ScannerKeyboard,
    )

    @Test
    fun continuousSwitchReflectsPersistedStateAndPersistsToggling() {
        val context = themedContext()
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

        settings.continuousScan = false
    }

    @Test
    fun formSwitchReflectsPersistedStateAndPersistsToggling() {
        val context = themedContext()
        val settings = ScanSettings(context)
        settings.translateScanActions = false

        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )

        val switch = view.findViewById<MaterialSwitch>(R.id.scanner_form_switch)
        assertFalse(switch.isChecked)

        switch.isChecked = true
        assertTrue(settings.translateScanActions)

        settings.translateScanActions = false
    }

    @Test
    fun freshlyConstructedViewRestoresBothPersistedSwitches() {
        val context = themedContext()
        val settings = ScanSettings(context)
        settings.continuousScan = true
        settings.translateScanActions = true

        val view = ScannerView(context)
        view.setCallbacks(
            onClose = {},
            onError = {},
            onBarcodeResult = {},
            onContinuousChanged = {},
        )

        assertTrue(view.findViewById<MaterialSwitch>(R.id.scanner_continuous_switch).isChecked)
        assertTrue(view.findViewById<MaterialSwitch>(R.id.scanner_form_switch).isChecked)

        settings.continuousScan = false
        settings.translateScanActions = false
    }
}
