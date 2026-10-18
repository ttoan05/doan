package com.example.foodhub.ui.order

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.foodhub.R
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

/**
 * Tiện ích hiển thị giao diện Đơn hàng dùng chung giữa:
 * - Tab Đơn hàng (MainActivity - Fragment Đơn hàng)
 * - Màn hình Chi tiết đơn hàng (OrderDetailActivity)
 */
object OrderUi {

    fun styleStatus(tv: TextView, status: OrderStatus) {
        val (text, bgColor, textColor) = when (status) {
            OrderStatus.PROCESSING -> Triple("Đang chuẩn bị", "#FFF4E5", "#B45309")
            OrderStatus.SHIPPING -> Triple("Đang giao hàng", "#EFF6FF", "#1D4ED8")
            OrderStatus.DELIVERED -> Triple("Hoàn thành", "#ECFDF5", "#047857")
            OrderStatus.CANCELLED -> Triple("Đã hủy", "#FEF2F2", "#B91C1C")
        }
        tv.text = text
        tv.setTextColor(Color.parseColor(textColor))
        tv.background = GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            cornerRadius = 12f * tv.resources.displayMetrics.density
        }
    }

    fun applyStatusBadge(tv: TextView, status: OrderStatus) {
        styleStatus(tv, status)
    }

    /** Thêm 1 dòng món: "Phở bò đặc biệt x2 ........ 45.000đ" */
    fun addItemLine(container: LinearLayout, item: CartItem) {
        val line = LayoutInflater.from(container.context)
            .inflate(R.layout.item_order_line, container, false)
        val ivImage = line.findViewById<ImageView>(R.id.ivLineImage)
        val tvEmoji = line.findViewById<TextView>(R.id.tvLineEmoji)

        SampleData.loadCartItemImage(ivImage, tvEmoji, item)

        val nameWithNote = if (item.note.isNotBlank()) {
            "${item.name} x${item.quantity}\n📝 ${item.note}"
        } else {
            "${item.name} x${item.quantity}"
        }

        line.findViewById<TextView>(R.id.tvLineName).text = nameWithNote
        line.findViewById<TextView>(R.id.tvLinePrice).text = FormatUtils.money(item.price)

        container.addView(line)
    }
}
