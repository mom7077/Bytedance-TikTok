package com.ttic.camera.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

data class OverlayText(
    var text: String,
    var sizeSp: Float,
    var color: Int,
    var alpha: Int,
    var rotation: Float,
    var typeface: Typeface,
    var centerX: Float,
    var centerY: Float,
    var scale: Float = 1f
)

/**
 * Lightweight overlay to place/drag/scale/rotate a single text block before committing to bitmap.
 */
class TextOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f * resources.displayMetrics.scaledDensity
    }

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())

    private var overlay: OverlayText? = null

    fun setOverlay(text: OverlayText) {
        overlay = text
        invalidate()
    }

    fun clearOverlay() {
        overlay = null
        invalidate()
    }

    fun currentOverlay(): OverlayText? = overlay

    fun updateRotation(degrees: Float) {
        overlay?.let {
            it.rotation = degrees
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val hasOverlay = overlay != null
        if (!hasOverlay) return false
        gestureDetector.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = overlay ?: return
        paint.color = data.color
        paint.alpha = (data.alpha / 100f * 255).toInt().coerceIn(0, 255)
        paint.textSize = data.sizeSp * resources.displayMetrics.scaledDensity * data.scale
        paint.typeface = data.typeface

        val layout = StaticLayout.Builder
            .obtain(data.text, 0, data.text.length, paint, (width * 0.8f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(data.centerX, data.centerY)
        canvas.rotate(data.rotation)
        canvas.translate(-layout.width / 2f, -layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            overlay?.let {
                it.centerX -= distanceX
                it.centerY -= distanceY
                invalidate()
            }
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            overlay?.let {
                val newScale = it.scale * detector.scaleFactor
                it.scale = min(2f, max(0.5f, newScale))
                invalidate()
            }
            return true
        }
    }
}
