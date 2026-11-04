package com.example.foodhub.ui.home

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.order.OrderAdapter

/**
 * Tách riêng toàn bộ phần render và logic của Tab Đơn hàng (Orders Content).
 */
object MainActivityOrdersView {

    fun showOrdersContent(
        activity: MainActivity,
        selectedOrderFilter: OrderStatus?,
        onFilterChanged: (OrderStatus?) -> Unit
    ) {
        val container = activity.contentContainer
        container.removeAllViews()
        activity.cartBadge = null

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(activity.dp(16), activity.dp(16), activity.dp(16), activity.dp(12))
        }
        val tvTitle = TextView(activity).apply {
            text = "Đơn hàng"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(activity.getColor(R.color.text_dark))
        }
        val tvSub = TextView(activity).apply {
            text = "Lưu lịch sử các đơn hàng trong 3 ngày gần nhất"
            textSize = 12f
            setTextColor(activity.getColor(R.color.text_grey))
            setPadding(0, activity.dp(2), 0, 0)
        }
        header.addView(tvTitle)
        header.addView(tvSub)
        container.addView(header)

        val filters: List<Pair<String, OrderStatus?>> = listOf(
            "Tất cả" to null,
            "Đang xử lý" to OrderStatus.PROCESSING,
            "Đang giao" to OrderStatus.SHIPPING,
            "Lịch sử" to OrderStatus.DELIVERED,
            "Đã hủy" to OrderStatus.CANCELLED
        )

        val filterScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, activity.dp(12))
            layoutParams = lp
        }
        val filterRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(activity.dp(16), 0, activity.dp(16), 0)
        }

        val orderRecycler = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            isNestedScrollingEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(activity.dp(16), 0, activity.dp(16), activity.dp(16))
            layoutParams = lp
        }
        val orderAdapter = OrderAdapter(
            onDetail = { order -> activity.openOrderDetail(order.id) },
            onReorder = { order ->
                CartManager.addItems(order.items)
                activity.refreshCartBadge()
                activity.showToast("Đã thêm ${order.totalQuantity} món vào giỏ hàng")
            }
        )
        orderRecycler.adapter = orderAdapter

        val emptyView = TextView(activity).apply {
            text = "Chưa có đơn hàng nào"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(R.color.text_grey))
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = activity.dp(60)
            layoutParams = lp
        }

        val filterButtons = mutableListOf<Button>()
        var currentFilter = selectedOrderFilter

        fun refreshOrderList() {
            val list = OrderManager.getOrders(currentFilter)
            orderAdapter.submit(list)
            emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            orderRecycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            emptyView.text = when (currentFilter) {
                null -> "Bạn chưa có đơn hàng nào"
                OrderStatus.DELIVERED -> "Chưa có đơn hàng nào đã hoàn thành"
                OrderStatus.PROCESSING -> "Không có đơn hàng nào đang xử lý"
                OrderStatus.SHIPPING -> "Không có đơn hàng nào đang giao"
                OrderStatus.CANCELLED -> "Không có đơn hàng nào đã hủy"
            }

            filterButtons.forEachIndexed { i, btn ->
                val (label, status) = filters[i]
                val count = OrderManager.countOrders(status)
                btn.text = if (count > 0) "$label ($count)" else label
                val isActive = status == currentFilter
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else activity.getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
        }

        filters.forEach { (label, status) ->
            val btn = Button(activity).apply {
                text = label
                textSize = 13f
                isAllCaps = false
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
                setOnClickListener {
                    currentFilter = status
                    onFilterChanged(status)
                    refreshOrderList()
                }
            }
            filterButtons.add(btn)
            filterRow.addView(btn)
        }
        filterScroll.addView(filterRow)
        container.addView(filterScroll)
        container.addView(orderRecycler)
        container.addView(emptyView)

        refreshOrderList()
    }
}
