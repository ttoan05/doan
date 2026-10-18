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
            Toast.makeText(this, "Đã làm mới danh sách đơn hàng", Toast.LENGTH_SHORT).show()
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

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

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

            val tvTab = TextView(this).apply {
                text = "$tabTitle ($count)"
                textSize = 12.5f
                gravity = Gravity.CENTER
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) R.drawable.bg_cat_active else R.drawable.bg_tab_chip_normal)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#475569"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(36)
                )
                lp.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = lp
                setPadding(dpToPx(14), 0, dpToPx(14), 0)
                setOnClickListener {
                    selectedTab = tabTitle
                    renderTabs()
                    renderOrderList()
                }
            }
            llOrderTabs.addView(tvTab)
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
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(60), dpToPx(24), dpToPx(40))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val tvIcon = TextView(this).apply {
                text = "📦"
                textSize = 44f
                gravity = Gravity.CENTER
            }
            val tvEmpty = TextView(this).apply {
                text = "Không tìm thấy đơn hàng nào phù hợp"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, dpToPx(10), 0, 0)
            }
            emptyCard.addView(tvIcon)
            emptyCard.addView(tvEmpty)
            llOrderContainer.addView(emptyCard)
            return
        }

        val inflater = layoutInflater
        filtered.forEach { order ->
            val itemView = inflater.inflate(R.layout.item_admin_order, llOrderContainer, false)

            val tvCode = itemView.findViewById<TextView>(R.id.tvOrderCode)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvOrderStatus)
            val tvMeta = itemView.findViewById<TextView>(R.id.tvOrderMeta)
            val tvCustomer = itemView.findViewById<TextView>(R.id.tvCustomerName)
            val tvAddress = itemView.findViewById<TextView>(R.id.tvCustomerAddress)
            val tvItems = itemView.findViewById<TextView>(R.id.tvOrderItems)
            val llNote = itemView.findViewById<View>(R.id.llOrderNote)
            val tvNote = itemView.findViewById<TextView>(R.id.tvOrderNote)
            val tvPayment = itemView.findViewById<TextView>(R.id.tvPaymentMethod)
            val tvTotal = itemView.findViewById<TextView>(R.id.tvOrderTotal)
            val btnDetail = itemView.findViewById<View>(R.id.btnOrderDetail)
            val btnUpdate = itemView.findViewById<View>(R.id.btnUpdateStatus)

            // Header info
            tvCode.text = "Mã đơn: #${order.code}"
            OrderUi.styleStatus(tvStatus, order.status)
            tvMeta.text = "${FormatUtils.dateTime(order.createdAt)}  •  ${order.restaurant}"

            // Customer
            tvCustomer.text = "Khách hàng: ${order.receiverName} • ${order.phone}"
            tvAddress.text = "Địa chỉ: ${order.address}"

            // Items
            val itemsSummary = order.items.joinToString(", ") { "${it.name} (x${it.quantity})" }
            tvItems.text = "Món (${order.totalQuantity}): $itemsSummary"

            // Note
            if (order.orderNote.isNotBlank()) {
                llNote.visibility = View.VISIBLE
                tvNote.text = "Ghi chú: ${order.orderNote}"
            } else {
                llNote.visibility = View.GONE
            }

            // Payment & Total
            tvPayment.text = order.paymentMethod
            tvTotal.text = FormatUtils.money(order.total)

            // Actions
            btnDetail.setOnClickListener {
                val intent = Intent(this@AdminOrderActivity, OrderDetailActivity::class.java).apply {
                    putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id)
                }
                startActivity(intent)
            }

            btnUpdate.setOnClickListener {
                showUpdateStatusDialog(order)
            }

            llOrderContainer.addView(itemView)
        }
    }

    private fun showUpdateStatusDialog(order: Order) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_order_status, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDialogOrderInfo).text =
            "Đơn #${order.code} • Khách: ${order.receiverName}"

        dialogView.findViewById<View>(R.id.btnDialogClose).setOnClickListener {
            dialog.dismiss()
        }

        // 1. Xác nhận đơn
        dialogView.findViewById<View>(R.id.btnStatusConfirm).setOnClickListener {
            dbHelper.updateOrderStatus(order.id, OrderStatus.PROCESSING, 1)
            Toast.makeText(this, "Đã xác nhận đơn hàng #${order.code}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadOrders()
        }

        // 2. Đang chuẩn bị món
        dialogView.findViewById<View>(R.id.btnStatusPreparing).setOnClickListener {
            dbHelper.updateOrderStatus(order.id, OrderStatus.PROCESSING, 2)
            Toast.makeText(this, "Đã chuyển sang: Đang chuẩn bị món", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadOrders()
        }

        // 3. Đang giao hàng
        dialogView.findViewById<View>(R.id.btnStatusShipping).setOnClickListener {
            dbHelper.updateOrderStatus(order.id, OrderStatus.SHIPPING, 3)
            Toast.makeText(this, "Đã chuyển sang: Đang giao hàng", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadOrders()
        }

        // 4. Hoàn tất đơn hàng
        dialogView.findViewById<View>(R.id.btnStatusDelivered).setOnClickListener {
            dbHelper.updateOrderStatus(order.id, OrderStatus.DELIVERED, 4)
            Toast.makeText(this, "Đã hoàn tất đơn hàng #${order.code}!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadOrders()
        }

        // 5. Hủy đơn
        dialogView.findViewById<View>(R.id.btnStatusCancel).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng #${order.code}?")
                .setPositiveButton("Hủy đơn") { _, _ ->
                    dbHelper.updateOrderStatus(order.id, OrderStatus.CANCELLED, -1)
                    Toast.makeText(this, "Đã hủy đơn hàng #${order.code}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadOrders()
                }
                .setNegativeButton("Không", null)
                .show()
        }

        dialog.show()
    }
}
