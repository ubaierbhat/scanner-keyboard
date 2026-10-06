package org.ubaierbhat.android.barcodekeyboard.service

import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
class NavigationBarInsetsTest {

    private fun dispatchNavBarInset(root: FrameLayout, bottom: Int, left: Int = 0, right: Int = 0) {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(Type.systemBars(), androidx.core.graphics.Insets.of(left, 0, right, bottom))
            .build()
        ViewCompat.dispatchApplyWindowInsets(root, insets)
    }

    @Test
    fun navBarBottomInsetBecomesRootBottomPadding() {
        val root = FrameLayout(ApplicationProvider.getApplicationContext())
        BarcodeKeyboardService.attachNavigationBarInsets(root)
        dispatchNavBarInset(root, bottom = 126)
        assertEquals(126, root.paddingBottom)
    }

    @Test
    fun zeroInsetKeepsPaddingZero() {
        val root = FrameLayout(ApplicationProvider.getApplicationContext())
        BarcodeKeyboardService.attachNavigationBarInsets(root)
        dispatchNavBarInset(root, bottom = 0)
        assertEquals(0, root.paddingBottom)
    }

    @Test
    fun sideInsetsAppliedInLandscape() {
        val root = FrameLayout(ApplicationProvider.getApplicationContext())
        BarcodeKeyboardService.attachNavigationBarInsets(root)
        dispatchNavBarInset(root, bottom = 0, left = 126)
        assertEquals(126, root.paddingLeft)
        assertEquals(0, root.paddingBottom)
    }
}
