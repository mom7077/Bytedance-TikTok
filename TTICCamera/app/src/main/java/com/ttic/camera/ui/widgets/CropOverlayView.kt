package com.ttic.camera.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun getRect(): RectF = RectF(rect)
    private val shadePaint = Paint().apply {
        color = 0x66000000
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private val touchSlop = 40f
    private val minSize = 150f

    private var activeHandle: Handle? = null

    enum class Handle { MOVE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (rect.isEmpty) {
            val margin = min(w, h) * 0.1f
            rect.set(margin, margin, w - margin, h - margin)
        } else {
            // keep normalized when size changes
            val norm = getNormalizedRect()
            rect.set(
                norm.left * w,
                norm.top * h,
                norm.right * w,
                norm.bottom * h
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // shade outside
        canvas.save()
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, shadePaint)
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), shadePaint)
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, shadePaint)
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, shadePaint)
        canvas.restore()

        canvas.drawRect(rect, borderPaint)
        val handleSize = 14f
        // corners
        canvas.drawCircle(rect.left, rect.top, handleSize, handlePaint)
        canvas.drawCircle(rect.right, rect.top, handleSize, handlePaint)
        canvas.drawCircle(rect.left, rect.bottom, handleSize, handlePaint)
        canvas.drawCircle(rect.right, rect.bottom, handleSize, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = detectHandle(event.x, event.y)
                return activeHandle != null
            }
            MotionEvent.ACTION_MOVE -> {
                activeHandle?.let { handle ->
                    updateRect(handle, event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = null
            }
        }
        return true
    }

    private fun detectHandle(x: Float, y: Float): Handle? {
        val nearLeft = x in (rect.left - touchSlop)..(rect.left + touchSlop)
        val nearRight = x in (rect.right - touchSlop)..(rect.right + touchSlop)
        val nearTop = y in (rect.top - touchSlop)..(rect.top + touchSlop)
        val nearBottom = y in (rect.bottom - touchSlop)..(rect.bottom + touchSlop)
        return when {
            nearLeft && nearTop -> Handle.TOP_LEFT
            nearRight && nearTop -> Handle.TOP_RIGHT
            nearLeft && nearBottom -> Handle.BOTTOM_LEFT
            nearRight && nearBottom -> Handle.BOTTOM_RIGHT
            nearLeft -> Handle.LEFT
            nearRight -> Handle.RIGHT
            nearTop -> Handle.TOP
            nearBottom -> Handle.BOTTOM
            rect.contains(x, y) -> Handle.MOVE
            else -> null
        }
    }

    private fun updateRect(handle: Handle, x: Float, y: Float) {
        when (handle) {
            Handle.MOVE -> {
                val dx = x - rect.centerX()
                val dy = y - rect.centerY()
                rect.offset(dx, dy)
                constrain()
            }
            Handle.LEFT -> rect.left = x
            Handle.RIGHT -> rect.right = x
            Handle.TOP -> rect.top = y
            Handle.BOTTOM -> rect.bottom = y
            Handle.TOP_LEFT -> {
                rect.top = y
                rect.left = x
            }
            Handle.TOP_RIGHT -> {
                rect.top = y
                rect.right = x
            }
            Handle.BOTTOM_LEFT -> {
                rect.bottom = y
                rect.left = x
            }
            Handle.BOTTOM_RIGHT -> {
                rect.bottom = y
                rect.right = x
            }
        }
        fixInvariants()
    }

    private fun constrain() {
        val dx = when {
            rect.left < 0 -> -rect.left
            rect.right > width -> width - rect.right
            else -> 0f
        }
        val dy = when {
            rect.top < 0 -> -rect.top
            rect.bottom > height -> height - rect.bottom
            else -> 0f
        }
        rect.offset(dx, dy)
    }

    private fun fixInvariants() {
        if (rect.width() < minSize) {
            val mid = rect.centerX()
            rect.left = mid - minSize / 2
            rect.right = mid + minSize / 2
        }
        if (rect.height() < minSize) {
            val mid = rect.centerY()
            rect.top = mid - minSize / 2
            rect.bottom = mid + minSize / 2
        }
        // keep within bounds
        rect.left = rect.left.coerceIn(0f, width - minSize)
        rect.right = rect.right.coerceIn(rect.left + minSize, width.toFloat())
        rect.top = rect.top.coerceIn(0f, height - minSize)
        rect.bottom = rect.bottom.coerceIn(rect.top + minSize, height.toFloat())
    }

    fun setAspectRatio(ratioW: Int, ratioH: Int) {
        if (ratioW <= 0 || ratioH <= 0 || width == 0 || height == 0) return
        val targetRatio = ratioW.toFloat() / ratioH
        val containerRatio = width.toFloat() / height
        val padding = min(width, height) * 0.08f
        if (containerRatio > targetRatio) {
            val h = height - padding * 2
            val w = h * targetRatio
            val left = (width - w) / 2
            val top = padding
            rect.set(left, top, left + w, top + h)
        } else {
            val w = width - padding * 2
            val h = w / targetRatio
            val top = (height - h) / 2
            val left = padding
            rect.set(left, top, left + w, top + h)
        }
        invalidate()
    }

    fun resetFreeform() {
        val margin = min(width, height) * 0.08f
        rect.set(margin, margin, width - margin, height - margin)
        invalidate()
    }

    fun getNormalizedRect(): RectF {
        return RectF(
            rect.left / width,
            rect.top / height,
            rect.right / width,
            rect.bottom / height
        )
    }
}
