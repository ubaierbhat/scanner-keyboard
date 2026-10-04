package org.ubaierbhat.android.barcodekeyboard.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import org.ubaierbhat.android.barcodekeyboard.R

class KeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : TextView(context, attrs, defStyleAttr) {

    var onPress: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onRepeat: (() -> Unit)? = null
    var onHoldTouch: ((MotionEvent) -> Unit)? = null

    var dragHoldEnabled: Boolean = false

    var keyIcon: Drawable? = null
        set(value) {
            field = value
            value?.setBounds(0, 0, value.intrinsicWidth, value.intrinsicHeight)
            invalidate()
        }

    var isKeyValueChecked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                refreshDrawableState()
            }
        }

    private val handler = Handler(Looper.getMainLooper())
    var longPressTimeoutMs: Long = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop
    private var holdFired = false
    private var downX = 0f
    private var downY = 0f

    private val longPressRunnable = Runnable {
        holdFired = true
        onLongPress?.invoke()
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            holdFired = true
            onRepeat?.invoke()
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    init {
        gravity = Gravity.CENTER
        maxLines = 1
        isClickable = true
        setBackgroundResource(R.drawable.key_background)
        val a = context.obtainStyledAttributes(attrs, R.styleable.KeyView, defStyleAttr, 0)
        keyIcon = a.getDrawable(R.styleable.KeyView_keyIcon)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val icon = keyIcon ?: return
        canvas.save()
        canvas.translate(
            (width - icon.bounds.width()) / 2f,
            (height - icon.bounds.height()) / 2f,
        )
        icon.draw(canvas)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                holdFired = false
                downX = event.x
                downY = event.y
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (onLongPress != null) {
                    handler.postDelayed(longPressRunnable, longPressTimeoutMs)
                }
                if (onRepeat != null) {
                    handler.postDelayed(repeatRunnable, REPEAT_INITIAL_DELAY_MS)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val longPressCallback = onLongPress
                if (dragHoldEnabled && longPressCallback != null) {
                    if (!holdFired && movedPastSlop(event)) {
                        handler.removeCallbacks(longPressRunnable)
                        holdFired = true
                        longPressCallback.invoke()
                    }
                    if (holdFired) {
                        onHoldTouch?.invoke(event)
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(repeatRunnable)
                if (dragHoldEnabled && holdFired) {
                    onHoldTouch?.invoke(event)
                }
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    holdFired = false
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        if (!isEnabled || holdFired) {
            holdFired = false
            return true
        }
        onPress?.invoke()
        return true
    }

    private fun movedPastSlop(event: MotionEvent): Boolean {
        val dx = event.x - downX
        val dy = event.y - downY
        return dx * dx + dy * dy > touchSlopPx * touchSlopPx
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        super.onDetachedFromWindow()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (isKeyValueChecked) {
            mergeDrawableStates(state, CHECKED_STATE_SET)
        }
        return state
    }

    companion object {
        private const val REPEAT_INITIAL_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 50L
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}
