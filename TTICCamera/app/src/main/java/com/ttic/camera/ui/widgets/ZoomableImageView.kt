package com.ttic.camera.ui.widgets

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

/**
 * Simple zoomable/pannable ImageView with clamped scale (0.5x - 2x).
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val matrixInternal = Matrix()

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            constrainTranslation()
            applyMatrix()
        }
        return true
    }

    private fun applyMatrix() {
        matrixInternal.reset()
        matrixInternal.postScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        matrixInternal.postTranslate(translateX, translateY)
        imageMatrix = matrixInternal
    }

    /**
     * Map a rect in view coordinates to drawable coordinates.
     */
    fun mapViewRectToDrawableRect(viewRect: RectF): RectF? {
        val drawable = drawable ?: return null
        val inverse = Matrix()
        if (!imageMatrix.invert(inverse)) return null
        val rect = RectF(viewRect)
        inverse.mapRect(rect)
        val dw = drawable.intrinsicWidth.toFloat()
        val dh = drawable.intrinsicHeight.toFloat()
        rect.left = rect.left.coerceIn(0f, dw)
        rect.top = rect.top.coerceIn(0f, dh)
        rect.right = rect.right.coerceIn(rect.left, dw)
        rect.bottom = rect.bottom.coerceIn(rect.top, dh)
        return rect
    }

    private fun constrainTranslation() {
        // Rough bounds to avoid drifting too far; keeps interactions simple for MVP.
        val maxOffsetX = width * 0.5f
        val maxOffsetY = height * 0.5f
        translateX = translateX.coerceIn(-maxOffsetX, maxOffsetX)
        translateY = translateY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = scaleFactor * detector.scaleFactor
            scaleFactor = min(2f, max(0.5f, newScale))
            applyMatrix()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (scaleFactor != 1f) {
                translateX -= distanceX
                translateY -= distanceY
                applyMatrix()
            }
            return true
        }
    }
}
