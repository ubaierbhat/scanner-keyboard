package org.ubaierbhat.android.barcodekeyboard.service

import android.Manifest
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.history.HistoryPanelView
import org.ubaierbhat.android.barcodekeyboard.scanner.ScannerView

@RunWith(RobolectricTestRunner::class)
class BarcodeKeyboardServiceRotationTest {

    private lateinit var service: BarcodeKeyboardService

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.setTheme(R.style.Theme_ScannerKeyboard)
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        service = Robolectric.setupService(BarcodeKeyboardService::class.java)
    }

    private fun hasVisibleDescendantOfType(root: View, target: Class<*>): Boolean {
        if (target.isInstance(root) && root.visibility == View.VISIBLE) {
            return true
        }
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                if (hasVisibleDescendantOfType(root.getChildAt(index), target)) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestQuietly(action: () -> Unit) {
        try {
            action()
        } catch (ignored: Throwable) {
        }
    }

    @Test
    fun scannerReattachesToNewContainerAfterRotation() {
        val containerA = service.onCreateInputView()
        requestQuietly { service.onScanRequested() }
        assertTrue(
            "containerA has no visible ScannerView",
            hasVisibleDescendantOfType(containerA, ScannerView::class.java),
        )

        val containerB = service.onCreateInputView()
        requestQuietly { service.onScanRequested() }
        assertTrue(
            "containerB has no visible ScannerView",
            hasVisibleDescendantOfType(containerB, ScannerView::class.java),
        )
    }

    @Test
    fun historyReattachesToNewContainerAfterRotation() {
        val containerA = service.onCreateInputView()
        requestQuietly { service.onHistoryRequested() }
        assertTrue(
            "containerA has no visible HistoryPanelView",
            hasVisibleDescendantOfType(containerA, HistoryPanelView::class.java),
        )

        val containerB = service.onCreateInputView()
        requestQuietly { service.onHistoryRequested() }
        assertTrue(
            "containerB has no visible HistoryPanelView",
            hasVisibleDescendantOfType(containerB, HistoryPanelView::class.java),
        )
    }
}
