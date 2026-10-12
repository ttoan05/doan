package com.example.foodhub.ui.checkout

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.ui.home.MainActivity
import com.example.foodhub.ui.order.OrderUi
import com.example.foodhub.utils.FormatUtils

class CheckoutActivity : AppCompatActivity() {

    private lateinit var etReceiver: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var rgPayment: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CartManager.ensureLoaded(this)
        OrderManager.ensureLoaded(this)
        setContentView(R.layout.activity_checkout)

        etReceiver = findViewById(R.id.etReceiver)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        rgPayment = findViewById(R.id.rgPayment)

        // Điền sẵn tên và số điện thoại người dùng đã lưu
        if (UserProfileManager.name.isNotEmpty()) {
            etReceiver.setText(UserProfileManager.name)
        }
        if (UserProfileManager.phone.isNotEmpty()) {
            etPhone.setText(UserProfileManager.phone)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnConfirmOrder).setOnClickListener { confirmOrder() }

        bindSummary()
    }

    private fun bindSummary() {
        val container = findViewById<LinearLayout>(R.id.llCheckoutItems)
        container.removeAllViews()

        val items = CartManager.getItems()
        items.forEach { OrderUi.addItemLine(container, it) }

        // Phí giao hàng tính theo từng nhà hàng (mỗi nhà hàng là 1 đơn)
        val subtotal = CartManager.totalPrice()
        val shipping = items.groupBy { it.restaurant }.values.sumOf { list ->
            OrderManager.shippingFeeFor(list.sumOf { it.total })
        }

        findViewById<TextView>(R.id.tvCheckoutSubtotal).text = FormatUtils.money(subtotal)
        findViewById<TextView>(R.id.tvCheckoutShipping).text =
            if (shipping == 0) "Miễn phí" else FormatUtils.money(shipping)
        findViewById<TextView>(R.id.tvCheckoutTotal).text = FormatUtils.money(subtotal + shipping)
    }

    private fun paymentMethod(): String = when (rgPayment.checkedRadioButtonId) {
        R.id.rbMomo -> "Ví MoMo"
        R.id.rbBank -> "Chuyển khoản ngân hàng"
        else -> "Thanh toán khi nhận hàng (COD)"
    }

    private fun confirmOrder() {
        if (CartManager.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show()
            return
        }

        val name = etReceiver.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty()) {
            etReceiver.error = "Vui lòng nhập tên người nhận"
            etReceiver.requestFocus()
            return
        }
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 9 || digits.length > 11) {
            etPhone.error = "Số điện thoại không hợp lệ"
            etPhone.requestFocus()
            return
        }
        if (address.isEmpty()) {
            etAddress.error = "Vui lòng nhập địa chỉ giao hàng"
            etAddress.requestFocus()
            return
        }

        // Tạo đơn hàng thật và lưu vào SQLite cho tài khoản hiện tại
        OrderManager.createOrders(
            cartItems = CartManager.getItems().map { it.copy() },
            receiverName = name,
            phone = phone,
            address = address,
            paymentMethod = paymentMethod()
        )

        // Xóa sạch giỏ hàng trong SQLite sau khi đặt thành công
        CartManager.clear()

        Toast.makeText(this, "Đặt hàng thành công 🎉", Toast.LENGTH_SHORT).show()

        // Quay về MainActivity và mở tab Đơn hàng
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("selected_tab", "orders")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
