package com.example.foodhub.ui.admin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.foodhub.R
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.utils.FormatUtils

/**
 * Helper hỗ trợ tạo các View hiển thị (Top món ăn bán chạy, Thẻ giao dịch) cho AdminRevenueActivity.
 */
object AdminRevenueViewHelper {

    fun dp(context: Context, v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    fun gradientRankBg(idx: Int): GradientDrawable {
        val c = when (idx) {
            0 -> Color.parseColor("#F59E0B")
            1 -> Color.parseColor("#94A3B8")
            2 -> Color.parseColor("#EA580C")
            else -> Color.parseColor("#CBD5E1")
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(c)
        }
    }

    fun statusBg(context: Context, status: OrderStatus): GradientDrawable {
        val color = when (status) {
            OrderStatus.DELIVERED -> "#16A34A"
            OrderStatus.SHIPPING -> "#2563EB"
            OrderStatus.PROCESSING -> "#F59E0B"
            OrderStatus.CANCELLED -> "#DC2626"
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 20).toFloat()
            setColor(Color.parseColor(color))
        }
    }

    fun buildTopFoodCard(
        context: Context,
        idx: Int,
        name: String,
        qty: Int,
        total: Int,
        sampleImageRes: Int?
    ): View {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = CardView(context).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#F8FAFC"))
            useCompatPadding = false
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val rank = TextView(context).apply {
            text = "#${idx + 1}"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            background = gradientRankBg(idx)
            setPadding(0, 0, 0, 0)
        }
        val image = ImageView(context).apply {
            val lp2 = LinearLayout.LayoutParams(dp(48), dp(48))
            lp2.setMargins(dp(10), 0, 0, 0)
            layoutParams = lp2
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = null
            if (sampleImageRes != null && sampleImageRes != 0) setImageResource(sampleImageRes)
            else setImageResource(R.drawable.img_banh_mi)
        }
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val lp3 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp3.setMargins(dp(12), 0, 0, 0)
            layoutParams = lp3
        }
        val tvName = TextView(context).apply {
            text = name
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvMeta = TextView(context).apply {
            text = "Đã bán: $qty phần  •  Doanh thu: ${FormatUtils.money(total)}"
            textSize = 12.5f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, dp(2), 0, 0)
        }
        texts.addView(tvName)
        texts.addView(tvMeta)
        row.addView(rank)
        row.addView(image)
        row.addView(texts)
        card.addView(row)
        return card
    }

    fun buildTransactionCard(
        context: Context,
        order: Order,
        onOrderClick: (Order) -> Unit
    ): View {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = CardView(context).apply {
            radius = dp(12).toFloat()
            cardElevation = 1f
            setCardBackgroundColor(Color.WHITE)
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvCode = TextView(context).apply {
            text = order.code
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvStatus = TextView(context).apply {
            text = order.status.label
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setTextColor(Color.WHITE)
            background = statusBg(context, order.status)
        }
        header.addView(tvCode)
        header.addView(tvStatus)

        val tvTime = TextView(context).apply {
            text = "${FormatUtils.dateTime(order.createdAt)}  •  ${order.restaurant}"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, dp(4), 0, 0)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvCustomer = TextView(context).apply {
            text = "${order.receiverName} • ${order.phone}"
            textSize = 12.5f
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0, dp(6), 0, 0)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvItems = TextView(context).apply {
            text = order.items.joinToString(", ") { "${it.name} x${it.quantity}" }
            textSize = 12.5f
            setTextColor(Color.parseColor("#334155"))
            setPadding(0, dp(4), 0, 0)
        }
        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val tvPay = TextView(context).apply {
            text = order.paymentMethod
            textSize = 11.5f
            setTextColor(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvTotal = TextView(context).apply {
            text = FormatUtils.money(order.total)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (order.status == OrderStatus.DELIVERED) Color.parseColor("#047857") else Color.parseColor("#0F172A"))
        }
        footer.addView(tvPay)
        footer.addView(tvTotal)

        col.addView(header)
        col.addView(tvTime)
        col.addView(tvCustomer)
        col.addView(tvItems)
        col.addView(footer)
        card.addView(col)
        card.isClickable = true
        card.isFocusable = true
        card.foreground = context.getDrawable(android.R.drawable.list_selector_background)
        card.setOnClickListener {
            onOrderClick(order)
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, dp(8), 0, 0)
        card.layoutParams = lp
        return card
    }
}
