package com.example.foodhub.ui.order

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.cart.CartActivity
import com.example.foodhub.utils.FormatUtils

class OrderDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
    }

    private var orderId: String = ""

    // Các bước hiển thị trên thanh tiến trình
    private val steps = listOf(
        "Đã đặt hàng",
        "Nhà hàng xác nhận",
        "Đang chuẩn bị",
        "Đang giao",
        "Đã giao"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCancelOrder).setOnClickListener { confirmCancel() }
        findViewById<Button>(R.id.btnReorderDetail).setOnClickListener { reorder() }

        bind()
    }

    override fun onResume() {
        super.onResume()
        // Khi trạng thái đơn thay đổi (đơn mới tự chuyển bước) thì cập nhật lại màn hình
        OrderManager.onChanged = { bind() }
        bind()
    }

    override fun onPause() {
        super.onPause()
        OrderManager.onChanged = null
    }

    // ─── Hiển thị dữ liệu ──────────────────────────────
    private fun bind() {
        val order = OrderManager.findById(orderId)
        if (order == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.tvDetailRestaurant).text = order.restaurant
        findViewById<TextView>(R.id.tvDetailCode).text = "Mã đơn hàng: ${order.code}"
        findViewById<TextView>(R.id.tvDetailDate).text =
            "Ngày đặt: ${FormatUtils.dateTime(order.createdAt)}"
        OrderUi.styleStatus(findViewById(R.id.tvDetailStatus), order.status)

        bindProgress(order)
        bindItems(order)

        findViewById<TextView>(R.id.tvDetailSubtotal).text = FormatUtils.money(order.subtotal)
        findViewById<TextView>(R.id.tvDetailShipping).text =
            if (order.shippingFee == 0) "Miễn phí" else FormatUtils.money(order.shippingFee)
        findViewById<TextView>(R.id.tvDetailTotal).text = FormatUtils.money(order.total)

        findViewById<TextView>(R.id.tvDetailReceiver).text = order.receiverName
        findViewById<TextView>(R.id.tvDetailPhone).text = order.phone
        findViewById<TextView>(R.id.tvDetailAddress).text = order.address
        findViewById<TextView>(R.id.tvDetailPayment).text = order.paymentMethod

        // Nút phía dưới
        val btnCancel = findViewById<Button>(R.id.btnCancelOrder)
        val btnReorder = findViewById<Button>(R.id.btnReorderDetail)
        btnCancel.visibility = if (order.canCancel()) View.VISIBLE else View.GONE
        btnReorder.visibility = if (order.canReorder()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.llActions).visibility =
            if (order.canCancel() || order.canReorder()) View.VISIBLE else View.GONE
    }

    private fun bindItems(order: Order) {
        val container = findViewById<LinearLayout>(R.id.llDetailItems)
        container.removeAllViews()
        order.items.forEach { OrderUi.addItemLine(container, it) }
    }

    private fun bindProgress(order: Order) {
        val container = findViewById<LinearLayout>(R.id.llProgress)
        container.removeAllViews()

        val red = getColor(R.color.red_primary)
        val grey = getColor(R.color.text_grey)
        val dark = getColor(R.color.text_dark)
        val lineGrey = getColor(R.color.border_grey)

        // Đơn đã hủy: chỉ hiển thị "Đã hủy"
        if (order.status == OrderStatus.CANCELLED) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_order_step, container, false)
            val dot = row.findViewById<TextView>(R.id.tvStepDot)
            dot.text = "✕"
            dot.setBackgroundResource(R.drawable.bg_step_done)
            row.findViewById<View>(R.id.vStepLine).visibility = View.GONE
            val label = row.findViewById<TextView>(R.id.tvStepLabel)
            label.text = "Đã hủy"
            label.setTextColor(red)
            label.setTypeface(null, android.graphics.Typeface.BOLD)
            container.addView(row)
            return
        }

        val current = order.progressStep()

        steps.forEachIndexed { index, title ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_order_step, container, false)
            val dot = row.findViewById<TextView>(R.id.tvStepDot)
            val line = row.findViewById<View>(R.id.vStepLine)
            val label = row.findViewById<TextView>(R.id.tvStepLabel)

            label.text = title

            if (index <= current) {
                // Bước đã xong hoặc đang ở bước này
                dot.setBackgroundResource(R.drawable.bg_step_done)
                dot.text = if (index < current || order.status == OrderStatus.DELIVERED) "✓" else ""
                label.setTextColor(if (index == current) red else dark)
                label.setTypeface(
                    null,
                    if (index == current) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
            } else {
                // Bước chưa tới
                dot.setBackgroundResource(R.drawable.bg_step_pending)
                dot.text = ""
                label.setTextColor(grey)
                label.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            if (index == steps.size - 1) {
                line.visibility = View.GONE
            } else {
                line.setBackgroundColor(if (index < current) red else lineGrey)
            }

            container.addView(row)
        }
    }

    // ─── Hủy đơn ───────────────────────────────────────
    private fun confirmCancel() {
        val order = OrderManager.findById(orderId) ?: return
        if (!order.canCancel()) {
            Toast.makeText(this, "Đơn hàng này không thể hủy", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn ${order.code} không?")
            .setPositiveButton("Hủy đơn") { _, _ ->
                if (OrderManager.cancel(orderId)) {
                    Toast.makeText(this, "Đã hủy đơn ${order.code}", Toast.LENGTH_SHORT).show()
                    bind()
                }
            }
            .setNegativeButton("Không", null)
            .show()
    }

    // ─── Đặt lại ───────────────────────────────────────
    private fun reorder() {
        val order = OrderManager.findById(orderId) ?: return
        CartManager.addItems(order.items)
        Toast.makeText(
            this,
            "Đã thêm ${order.totalQuantity} món vào giỏ hàng",
            Toast.LENGTH_SHORT
        ).show()
        startActivity(Intent(this, CartActivity::class.java))
    }
}
