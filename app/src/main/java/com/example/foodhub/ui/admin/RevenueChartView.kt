package com.example.foodhub.ui.admin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class RevenueChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var values: List<Long> = emptyList()
    private var labels: List<String> = emptyList()
    private var dayKeys: List<String> = emptyList()

    var onDaySelected: ((String) -> Unit)? = null
    var selectedKey: String? = null

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2E8F0.toInt(); strokeWidth = 1f }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF64748B.toInt(); textSize = 11f * resources.displayMetrics.scaledDensity / 2.8f; textAlign = Paint.Align.CENTER }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt(); textSize = 10f * resources.displayMetrics.scaledDensity / 2.8f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barSelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt() }

    private val rect = RectF()
    private val hitRects = mutableListOf<RectF>()

    fun setData(keys: List<String>, lbls: List<String>, vals: List<Long>, selected: String? = selectedKey) {
        dayKeys = keys
        labels = lbls
        values = vals
        selectedKey = selected
        invalidate()
    }

    fun setValues(newValues: List<Long>) {
        values = newValues
        labels = newValues.indices.map { (it + 1).toString() }
        dayKeys = labels
        selectedKey = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        hitRects.clear()
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val leftPad = 12f
        val rightPad = 12f
        val bottomPad = 34f
        val topPad = 24f
        // baseline
        canvas.drawLine(leftPad, h - bottomPad, w - rightPad, h - bottomPad, axisPaint)

        if (values.isEmpty()) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF94A3B8.toInt(); textSize = 13f * resources.displayMetrics.scaledDensity / 2.8f; textAlign = Paint.Align.CENTER }
            canvas.drawText("Chưa có dữ liệu", w / 2f, h / 2f, p)
            return
        }

        val maxVal = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val n = values.size
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - bottomPad
        val gap = 6f * resources.displayMetrics.density
        val barW = (chartW - gap * (n + 1)) / n.coerceAtLeast(1)
        val cw = barW.coerceIn(8f * resources.displayMetrics.density, 42f * resources.displayMetrics.density)

        // grid line dashed subtle
        axisPaint.alpha = 120
        canvas.drawLine(leftPad, h - bottomPad - chartH * 0.5f, w - rightPad, h - bottomPad - chartH * 0.5f, axisPaint)
        axisPaint.alpha = 255

        values.forEachIndexed { index, v ->
            val x = leftPad + gap + index * (cw + gap)
            val ratio = v.toFloat() / maxVal
            val barH = chartH * ratio
            val top = h - bottomPad - barH
            val bottom = h - bottomPad
            rect.set(x, top, x + cw, bottom)
            hitRects.add(RectF(rect))

            val isSelected = dayKeys.getOrNull(index) == selectedKey
            if (isSelected) {
                barPaint.color = 0xFF0F172A.toInt()
                canvas.drawRoundRect(rect, 8f, 8f, barPaint)
            } else {
                barPaint.color = 0xFFE63946.toInt()
                canvas.drawRoundRect(rect, 8f, 8f, barPaint)
            }

            // value label on top if not too crowded
            if (n <= 10 && v > 0) {
                val txt = if (v >= 1_000_000) "${v / 1000}k" else v.toString()
                canvas.drawText(txt, x + cw / 2f, (top - 6f).coerceAtLeast(topPad), valuePaint)
            }
            // day label
            val lbl = labels.getOrNull(index) ?: ""
            canvas.drawText(lbl, x + cw / 2f, h - 12f, labelPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            hitRects.forEachIndexed { idx, r ->
                // expand hit area vertically
                val expanded = RectF(r.left - 6f, 0f, r.right + 6f, height.toFloat())
                if (expanded.contains(x, y)) {
                    val key = dayKeys.getOrNull(idx) ?: return@forEachIndexed
                    selectedKey = key
                    onDaySelected?.invoke(key)
                    invalidate()
                    performClick()
                    return true
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
