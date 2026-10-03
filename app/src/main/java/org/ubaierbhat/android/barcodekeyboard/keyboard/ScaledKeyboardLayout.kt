package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import org.ubaierbhat.android.barcodekeyboard.R

class ScaledKeyboardLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val desiredHeightPx = context.resources.getDimensionPixelSize(R.dimen.keyboard_total_height)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val target = if (mode != MeasureSpec.UNSPECIFIED && size > 0 && size < desiredHeightPx) {
            size
        } else {
            desiredHeightPx
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(target, MeasureSpec.EXACTLY))
    }
}
