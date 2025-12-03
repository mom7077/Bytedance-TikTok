package com.ttic.camera.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.ttic.camera.ui.widgets.OverlayText
import kotlin.math.max
import kotlin.math.min

object EditorProcessor {

    fun applyOperations(base: Bitmap, operations: List<EditorOperation>): Bitmap {
        if (operations.isEmpty()) return base.copy(Bitmap.Config.ARGB_8888, true)
        var result = base.copy(Bitmap.Config.ARGB_8888, true)
        operations.forEach { op ->
            val processed = when (op) {
                is EditorOperation.Adjust -> adjust(result, op.brightness, op.contrast)
                is EditorOperation.Rotate -> rotate(result, op.degrees)
                is EditorOperation.Flip -> flip(result, op.horizontal)
                is EditorOperation.Crop -> cropCenter(result, op.ratioWidth, op.ratioHeight)
                is EditorOperation.Text -> drawText(result, op.overlay)
            }
            if (processed !== result) {
                result.recycleSafely()
                result = processed
            }
        }
        return result
    }

    private fun adjust(src: Bitmap, brightness: Int, contrast: Int): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // contrast range -50..150 -> convert to scale
        val contrastScale = (contrast + 100) / 100f
        val translate = brightness * 2.55f
        val cm = ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, translate,
                0f, contrastScale, 0f, 0f, translate,
                0f, 0f, contrastScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun flip(src: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) {
                postScale(-1f, 1f, src.width / 2f, src.height / 2f)
            } else {
                postScale(1f, -1f, src.width / 2f, src.height / 2f)
            }
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun cropCenter(src: Bitmap, ratioW: Int, ratioH: Int): Bitmap {
        val targetRatio = ratioW.toFloat() / ratioH
        val srcRatio = src.width.toFloat() / src.height
        var cropWidth = src.width
        var cropHeight = src.height
        if (srcRatio > targetRatio) {
            // wider than target -> crop width
            cropWidth = (src.height * targetRatio).toInt()
        } else if (srcRatio < targetRatio) {
            cropHeight = (src.width / targetRatio).toInt()
        }
        val left = max(0, (src.width - cropWidth) / 2)
        val top = max(0, (src.height - cropHeight) / 2)
        return Bitmap.createBitmap(src, left, top, cropWidth, cropHeight)
    }

    private fun drawText(src: Bitmap, overlay: OverlayText): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = overlay.color
            alpha = (overlay.alpha / 100f * 255).toInt().coerceIn(0, 255)
            textSize = overlay.sizeSp * result.densityScale() * overlay.scale
            textAlign = Paint.Align.CENTER
            typeface = overlay.typeface
        }
        canvas.save()
        canvas.translate(overlay.centerX, overlay.centerY)
        canvas.rotate(overlay.rotation)
        val lines = overlay.text.split("\n")
        val lineHeight = paint.fontMetrics.bottom - paint.fontMetrics.top
        val totalHeight = lineHeight * lines.size
        lines.forEachIndexed { index, line ->
            val y = -totalHeight / 2 + lineHeight * (index + 1) - paint.fontMetrics.bottom
            canvas.drawText(line, 0f, y, paint)
        }
        canvas.restore()
        return result
    }

    private fun Bitmap.densityScale(): Float = this.density.takeIf { it > 0 }?.div(160f) ?: 1f

    private fun Bitmap.recycleSafely() {
        if (!isRecycled) {
            runCatching { recycle() }
        }
    }
}
