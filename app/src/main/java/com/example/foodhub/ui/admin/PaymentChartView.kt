package com.example.foodhub.ui.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

class PaymentChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values = listOf(0, 0, 0)
    private val labels = listOf("COD", "MoMo", "Bank QR")
    private val colors = listOf(0xFF16A34A.toInt(), 0xFF9333EA.toInt(), 0xFF2563EB.toInt())
    private val trackColor = 0xFFE2E8F0.toInt()
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF334155.toInt()
        textSize = 12f * resources.displayMetrics.density
        isFakeBoldText = true
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF64748B.toInt()
        textSize = 11f * resources.displayMetrics.density
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
    private val rect = RectF()

    fun setValues(newValues: List<Int>) {
        values = newValues.take(3).let { it + List(3 - it.size) { 0 } }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val maxValue = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val total = values.sum().coerceAtLeast(1)
        val left = 4f * density
        val labelWidth = 66f * density
        val right = width - 8f * density
        val barLeft = left + labelWidth
        val barWidth = (right - barLeft).coerceAtLeast(40f * density)
        val rowHeight = 34f * density
        val barHeight = 10f * density

        values.forEachIndexed { index, value ->
            val centerY = 18f * density + index * rowHeight
            val labelY = centerY + 4f * density
            canvas.drawText(labels[index], left, labelY, labelPaint)

            rect.set(barLeft, centerY - barHeight / 2f, right, centerY + barHeight / 2f)
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, trackPaint)

            val progress = value.toFloat() / maxValue.toFloat()
            rect.set(barLeft, centerY - barHeight / 2f, barLeft + barWidth * progress, centerY + barHeight / 2f)
            barPaint.color = colors[index]
            canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, barPaint)

            val percent = (value * 100f / total).roundToInt()
            percentPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("$percent%", right, centerY + 4f * density, percentPaint)
        }
    }
}
