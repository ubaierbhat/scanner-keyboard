package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.ubaierbhat.android.barcodekeyboard.R

@RunWith(RobolectricTestRunner::class)
class ScaledKeyboardLayoutTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val desiredPx = context.resources.getDimensionPixelSize(R.dimen.keyboard_total_height)

    private fun keyboardLikeLayout(): ScaledKeyboardLayout =
        ScaledKeyboardLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val toolbarHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                40f,
                context.resources.displayMetrics,
            ).toInt()
            addView(
                View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, toolbarHeightPx)
                },
            )
            repeat(4) {
                addView(
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    },
                )
            }
        }

    @Test
    fun atMostSmallerThanDesiredClampsToSpecAndScalesWeightedRows() {
        val toolbarHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            40f,
            context.resources.displayMetrics,
        ).toInt()
        val perRow = (desiredPx - toolbarHeight) / 8
        val deficit = toolbarHeight + perRow * 4
        val view = keyboardLikeLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(deficit, View.MeasureSpec.AT_MOST),
        )
        assertEquals(deficit, view.measuredHeight)
        assertEquals(toolbarHeight, view.getChildAt(0).measuredHeight)
        for (i in 1..4) {
            assertEquals(perRow, view.getChildAt(i).measuredHeight)
        }
    }

    @Test
    fun atMostLargerThanDesiredKeepsDesiredHeight() {
        val view = keyboardLikeLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(desiredPx * 3, View.MeasureSpec.AT_MOST),
        )
        assertEquals(desiredPx, view.measuredHeight)
    }

    @Test
    fun exactlySmallerThanDesiredClampsToSpec() {
        val view = keyboardLikeLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(desiredPx / 2, View.MeasureSpec.EXACTLY),
        )
        assertEquals(desiredPx / 2, view.measuredHeight)
    }

    @Test
    fun unspecifiedKeepsDesiredHeight() {
        val view = keyboardLikeLayout()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        assertEquals(desiredPx, view.measuredHeight)
    }
}
