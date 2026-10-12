package com.example.foodhub.ui.notification

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/** Vuốt ngang một thông báo để xóa, phía sau hiện nền đỏ + chữ "Xóa". */
class SwipeToDeleteCallback(
    private val canSwipe: (Int) -> Boolean,
    private val onDelete: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E63946")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        // Dòng tiêu đề nhóm ("Hôm nay"...) thì không cho vuốt
        return if (canSwipe(viewHolder.bindingAdapterPosition)) {
            super.getSwipeDirs(recyclerView, viewHolder)
        } else {
            0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onDelete(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
            val density = recyclerView.resources.displayMetrics.density
            val v = viewHolder.itemView
            val radius = 14f * density

            val rect = if (dX > 0) {
                RectF(v.left.toFloat(), v.top.toFloat(), v.left + dX, v.bottom.toFloat())
            } else {
                RectF(v.right + dX, v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat())
            }
            c.drawRoundRect(rect, radius, radius, bgPaint)

            // Vuốt đủ xa mới vẽ chữ để khỏi bị tràn ra ngoài nền đỏ
            if (Math.abs(dX) > 70f * density) {
                textPaint.textSize = 14f * density
                val baseline = (v.top + v.bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                if (dX > 0) {
                    textPaint.textAlign = Paint.Align.LEFT
                    c.drawText("Xóa", v.left + 22f * density, baseline, textPaint)
                } else {
                    textPaint.textAlign = Paint.Align.RIGHT
                    c.drawText("Xóa", v.right - 22f * density, baseline, textPaint)
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
