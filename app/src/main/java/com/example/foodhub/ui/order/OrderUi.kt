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

/** Các hàm dùng chung cho danh sách đơn hàng và chi tiết đơn hàng. */
object OrderUi {

    /** Tô màu "chip" trạng thái đơn hàng. */
    fun styleStatus(tv: TextView, status: OrderStatus) {
        val textColor: String
        val bgColor: String
        when (status) {
            OrderStatus.PROCESSING -> { textColor = "#B45309"; bgColor = "#FEF3C7" }
            OrderStatus.SHIPPING   -> { textColor = "#2563EB"; bgColor = "#E0EAFF" }
            OrderStatus.DELIVERED  -> { textColor = "#1A7A5E"; bgColor = "#DDF3EA" }
            OrderStatus.CANCELLED  -> { textColor = "#E63946"; bgColor = "#FDECEE" }
        }
        tv.text = status.label
        tv.setTextColor(Color.parseColor(textColor))
        tv.background = GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            cornerRadius = 12f * tv.resources.displayMetrics.density
        }
    }

    /** Thêm 1 dòng món: "Phở bò đặc biệt x2 ........ 45.000đ" */
    fun addItemLine(container: LinearLayout, item: CartItem) {
        val line = LayoutInflater.from(container.context)
            .inflate(R.layout.item_order_line, container, false)
        val ivImage = line.findViewById<ImageView>(R.id.ivLineImage)
        val tvEmoji = line.findViewById<TextView>(R.id.tvLineEmoji)

        val actualImgRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
        if (actualImgRes != 0) {
            ivImage.setImageResource(actualImgRes)
            ivImage.visibility = View.VISIBLE
            tvEmoji.visibility = View.GONE
        } else {
            ivImage.visibility = View.GONE
            tvEmoji.visibility = View.VISIBLE
            tvEmoji.text = item.emoji
        }

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
