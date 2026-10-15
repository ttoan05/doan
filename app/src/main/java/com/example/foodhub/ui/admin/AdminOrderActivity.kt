package com.example.foodhub.ui.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.ui.order.OrderUi
import com.example.foodhub.utils.FormatUtils

class AdminOrderActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llOrderContainer: LinearLayout
    private lateinit var llOrderTabs: LinearLayout
    private lateinit var tvOrderSummary: TextView
    private lateinit var etSearchOrder: EditText

    private var allOrders: List<Order> = emptyList()
    private var selectedTab: String = "Tất cả"
    private var searchQuery: String = ""

    private val tabs = listOf("Tất cả", "Đang xử lý", "Đang giao", "Đã giao", "Đã hủy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_order)

        dbHelper = DatabaseHelper.getInstance(this)

        llOrderContainer = findViewById(R.id.llOrderContainer)
        llOrderTabs      = findViewById(R.id.llOrderTabs)
        tvOrderSummary   = findViewById(R.id.tvOrderSummary)
        etSearchOrder    = findViewById(R.id.etSearchOrder)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnRefreshOrders).setOnClickListener {
            Toast.makeText(this, "Đang làm mới danh sách đơn...", Toast.LENGTH_SHORT).show()
            loadOrders()
        }

        etSearchOrder.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            renderOrderList()
        }

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        allOrders = dbHelper.getAllOrders()
        renderTabs()
        renderOrderList()
    }

    private fun renderTabs() {
        llOrderTabs.removeAllViews()
        tabs.forEach { tabTitle ->
            val isSelected = tabTitle == selectedTab
            val count = when (tabTitle) {
                "Tất cả" -> allOrders.size
                "Đang xử lý" -> allOrders.count { it.status == OrderStatus.PROCESSING }
                "Đang giao" -> allOrders.count { it.status == OrderStatus.SHIPPING }
                "Đã giao" -> allOrders.count { it.status == OrderStatus.DELIVERED }
                "Đã hủy" -> allOrders.count { it.status == OrderStatus.CANCELLED }
                else -> 0
            }

            val btn = Button(this).apply {
                text = "$tabTitle ($count)"
                textSize = 12f
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#4B5563"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 12, 0)
                layoutParams = lp
                setOnClickListener {
                    selectedTab = tabTitle
                    renderTabs()
                    renderOrderList()
                }
            }
            llOrderTabs.addView(btn)
        }
    }

    private fun renderOrderList() {
        llOrderContainer.removeAllViews()

        var filtered = when (selectedTab) {
            "Đang xử lý" -> allOrders.filter { it.status == OrderStatus.PROCESSING }
            "Đang giao" -> allOrders.filter { it.status == OrderStatus.SHIPPING }
            "Đã giao" -> allOrders.filter { it.status == OrderStatus.DELIVERED }
            "Đã hủy" -> allOrders.filter { it.status == OrderStatus.CANCELLED }
            else -> allOrders
        }

        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.id.lowercase().contains(q) ||
                it.receiverName.lowercase().contains(q) ||
                it.phone.contains(q) ||
                it.restaurant.lowercase().contains(q)
            }
        }

        tvOrderSummary.text = "Hiển thị ${filtered.size} / ${allOrders.size} đơn hàng hệ thống"

        if (filtered.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Không có đơn hàng nào trong mục này."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 60, 0, 60)
            }
            llOrderContainer.addView(tvEmpty)
            return
        }

        filtered.forEach { order ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(20, 18, 20, 18)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            // Header row: Mã đơn + Badge trạng thái
            val rowHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvId = TextView(this).apply {
                text = "Mã đơn: ${order.code}"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvStatusBadge = TextView(this).apply {
                OrderUi.styleStatus(this, order.status)
                setPadding(14, 6, 14, 6)
            }

            rowHeader.addView(tvId)
            rowHeader.addView(tvStatusBadge)
            card.addView(rowHeader)

            // Info rows
            val tvDateAndShop = TextView(this).apply {
                text = "🕒 ${FormatUtils.dateTime(order.createdAt)}  •  🏠 ${order.restaurant}"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 4, 0, 6)
            }
            card.addView(tvDateAndShop)

            val tvCustomer = TextView(this).apply {
                text = "👤 Khách: ${order.receiverName} (${order.phone})\n📍 Đ/c: ${order.address}"
                textSize = 13f
                setTextColor(Color.parseColor("#374151"))
                setLineSpacing(3f, 1f)
                setPadding(0, 0, 0, 6)
            }
            card.addView(tvCustomer)

            // Items summary
            val itemsSummary = order.items.joinToString(", ") { "${it.name} (x${it.quantity})" }
            val tvItems = TextView(this).apply {
                text = "🍲 Món (${order.totalQuantity}): $itemsSummary"
                textSize = 12f
                setTextColor(Color.parseColor("#4B5563"))
                setPadding(0, 0, 0, 6)
            }
            card.addView(tvItems)

            // Order note (if any)
            if (order.orderNote.isNotBlank()) {
                val tvNote = TextView(this).apply {
                    text = "📝 Ghi chú: ${order.orderNote}"
                    textSize = 12f
                    setTypeface(null, Typeface.ITALIC)
                    setTextColor(Color.parseColor("#B45309"))
                    setBackgroundColor(Color.parseColor("#FEF3C7"))
                    setPadding(12, 6, 12, 6)
                }
                card.addView(tvNote)
            }

            // Payment & Total
            val rowPayment = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val tvPayMethod = TextView(this).apply {
                text = "💳 ${order.paymentMethod}"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvTotal = TextView(this).apply {
                text = "Tổng: ${FormatUtils.money(order.total)}"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#E63946"))
            }
            rowPayment.addView(tvPayMethod)
            rowPayment.addView(tvTotal)
            card.addView(rowPayment)

            // Divider
            val div = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 4, 0, 10) }
                setBackgroundColor(Color.parseColor("#F3F4F6"))
            }
            card.addView(div)

            // Action buttons row
            val rowActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val btnDetail = Button(this).apply {
                text = "🔍 Xem chi tiết"
                textSize = 12f
                setBackgroundResource(R.drawable.bg_btn_blue)
                setTextColor(Color.WHITE)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 12, 0)
                layoutParams = lp
                setOnClickListener {
                    val intent = Intent(this@AdminOrderActivity, OrderDetailActivity::class.java).apply {
                        putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id)
                    }
                    startActivity(intent)
                }
            }

            val btnUpdate = Button(this).apply {
                text = "⚡ Cập nhật trạng thái"
                textSize = 12f
                setBackgroundResource(R.drawable.bg_btn_red)
                setTextColor(Color.WHITE)
                setOnClickListener { showUpdateStatusDialog(order) }
            }

            rowActions.addView(btnDetail)
            rowActions.addView(btnUpdate)
            card.addView(rowActions)

            llOrderContainer.addView(card)
        }
    }

    private fun showUpdateStatusDialog(order: Order) {
        val options = arrayOf(
            "1. Xác nhận đơn (Nhà hàng đã xác nhận)",
            "2. Đang chuẩn bị món ăn (Bếp đang làm)",
            "3. Bàn giao shipper (Đang giao hàng)",
            "4. Hoàn tất đơn hàng (Đã giao thành công)",
            "5. Hủy đơn hàng (Khách hủy / hết món)"
        )

        AlertDialog.Builder(this)
            .setTitle("Cập nhật đơn #${order.id}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        dbHelper.updateOrderStatus(order.id, OrderStatus.PROCESSING, 1)
                        Toast.makeText(this, "Đã xác nhận đơn hàng #${order.id}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        dbHelper.updateOrderStatus(order.id, OrderStatus.PROCESSING, 2)
                        Toast.makeText(this, "Đã chuyển sang: Đang chuẩn bị món", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        dbHelper.updateOrderStatus(order.id, OrderStatus.SHIPPING, 3)
                        Toast.makeText(this, "Đã chuyển sang: Đang giao hàng", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        dbHelper.updateOrderStatus(order.id, OrderStatus.DELIVERED, 4)
                        Toast.makeText(this, "Đã hoàn tất đơn hàng #${order.id}!", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        dbHelper.updateOrderStatus(order.id, OrderStatus.CANCELLED, -1)
                        Toast.makeText(this, "Đã hủy đơn hàng #${order.id}", Toast.LENGTH_SHORT).show()
                    }
                }
                loadOrders()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }
}
