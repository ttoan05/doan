package com.example.foodhub.ui.checkout

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.AddressStore
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.PaymentStore
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.VoucherStore
import com.example.foodhub.data.model.SavedAddress
import com.example.foodhub.data.model.Voucher
import com.example.foodhub.ui.home.MainActivity
import com.example.foodhub.ui.order.OrderUi
import com.example.foodhub.utils.FormatUtils

class CheckoutActivity : AppCompatActivity() {

    private lateinit var llMultiRestaurantWarning: LinearLayout
    private lateinit var tvMultiRestaurantWarningText: TextView

    private lateinit var etReceiver: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var cbSaveAddress: CheckBox
    private lateinit var etOrderNote: EditText

    private lateinit var etVoucherCode: EditText
    private lateinit var btnApplyVoucher: Button
    private lateinit var btnPickVoucher: TextView
    private lateinit var llAppliedVoucher: LinearLayout
    private lateinit var tvAppliedVoucherDesc: TextView
    private lateinit var btnRemoveVoucher: TextView

    private lateinit var rgPayment: RadioGroup
    private lateinit var llCheckoutDiscountRow: LinearLayout
    private lateinit var tvCheckoutDiscount: TextView
    private lateinit var tvCheckoutSubtotal: TextView
    private lateinit var tvCheckoutShipping: TextView
    private lateinit var tvCheckoutTotal: TextView

    private var appliedVoucher: Voucher? = null
    private var appliedDiscount: Int = 0
    private var isWarningDismissed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CartManager.ensureLoaded(this)
        OrderManager.ensureLoaded(this)
        setContentView(R.layout.activity_checkout)

        llMultiRestaurantWarning = findViewById(R.id.llMultiRestaurantWarning)
        tvMultiRestaurantWarningText = findViewById(R.id.tvMultiRestaurantWarningText)

        etReceiver = findViewById(R.id.etReceiver)
        etPhone = findViewById(R.id.etPhone)
        etAddress = findViewById(R.id.etAddress)
        cbSaveAddress = findViewById(R.id.cbSaveAddress)
        etOrderNote = findViewById(R.id.etOrderNote)

        etVoucherCode = findViewById(R.id.etVoucherCode)
        btnApplyVoucher = findViewById(R.id.btnApplyVoucher)
        btnPickVoucher = findViewById(R.id.btnPickVoucher)
        llAppliedVoucher = findViewById(R.id.llAppliedVoucher)
        tvAppliedVoucherDesc = findViewById(R.id.tvAppliedVoucherDesc)
        btnRemoveVoucher = findViewById(R.id.btnRemoveVoucher)

        rgPayment = findViewById(R.id.rgPayment)
        when (PaymentStore.getDefaultMethod(this)) {
            PaymentStore.METHOD_MOMO -> rgPayment.check(R.id.rbMomo)
            PaymentStore.METHOD_BANK -> rgPayment.check(R.id.rbBank)
            else -> rgPayment.check(R.id.rbCod)
        }
        llCheckoutDiscountRow = findViewById(R.id.llCheckoutDiscountRow)
        tvCheckoutDiscount = findViewById(R.id.tvCheckoutDiscount)
        tvCheckoutSubtotal = findViewById(R.id.tvCheckoutSubtotal)
        tvCheckoutShipping = findViewById(R.id.tvCheckoutShipping)
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal)

        loadInitialAddress()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnConfirmOrder).setOnClickListener { confirmOrder() }

        findViewById<TextView>(R.id.btnPickSavedAddress).setOnClickListener {
            showPickSavedAddressDialog()
        }

        btnPickVoucher.setOnClickListener {
            showPickVoucherDialog()
        }

        btnApplyVoucher.setOnClickListener {
            val code = etVoucherCode.text.toString().trim()
            applyVoucherCode(code)
        }

        btnRemoveVoucher.setOnClickListener {
            removeVoucher()
        }

        findViewById<View>(R.id.btnCloseWarning)?.setOnClickListener {
            isWarningDismissed = true
            llMultiRestaurantWarning.visibility = View.GONE
        }

        bindSummary()
    }

    private fun loadInitialAddress() {
        val defaultAddr = AddressStore.getDefault(this)
        if (defaultAddr != null) {
            etReceiver.setText(defaultAddr.receiver)
            etPhone.setText(defaultAddr.phone)
            etAddress.setText(defaultAddr.address)
        } else {
            if (UserProfileManager.name.isNotEmpty()) {
                etReceiver.setText(UserProfileManager.name)
            }
            if (UserProfileManager.phone.isNotEmpty()) {
                etPhone.setText(UserProfileManager.phone)
            }
        }
    }

    private fun showPickSavedAddressDialog() {
        val addresses = AddressStore.getAll(this)
        if (addresses.isEmpty()) {
            Toast.makeText(this, "Bạn chưa lưu địa chỉ nào trong sổ địa chỉ", Toast.LENGTH_SHORT).show()
            return
        }

        val items = addresses.map { addr ->
            val defTag = if (addr.isDefault) " [Mặc định]" else ""
            "${addr.label}$defTag - ${addr.receiver} (${addr.phone})\n${addr.address}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Chọn địa chỉ giao hàng")
            .setItems(items) { _, which ->
                val selected = addresses[which]
                etReceiver.setText(selected.receiver)
                etPhone.setText(selected.phone)
                etAddress.setText(selected.address)
                Toast.makeText(this, "Đã chọn: ${selected.label}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showPickVoucherDialog() {
        val vouchers = VoucherStore.VOUCHERS
        val items = vouchers.map { v ->
            val usedTag = if (VoucherStore.isUsed(this, v.code)) " [Đã dùng]" else ""
            "${v.emoji} ${v.code}$usedTag: ${v.title}\n   ${v.desc}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Chọn mã giảm giá")
            .setItems(items) { _, which ->
                val selected = vouchers[which]
                etVoucherCode.setText(selected.code)
                applyVoucherCode(selected.code)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun applyVoucherCode(code: String) {
        if (code.isBlank()) {
            Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show()
            return
        }

        val subtotal = CartManager.totalPrice()
        val shipping = calculateShipping()

        val result = VoucherStore.validate(
            rawCode = code,
            subtotal = subtotal,
            shippingForFreeship = shipping,
            context = this
        )

        if (result.success && result.voucher != null) {
            appliedVoucher = result.voucher
            appliedDiscount = result.discount

            llAppliedVoucher.visibility = View.VISIBLE
            tvAppliedVoucherDesc.text = "✓ Đã áp dụng ${result.voucher.code}: Giảm ${FormatUtils.money(result.discount)}"
            etVoucherCode.setText(result.voucher.code)

            bindSummary()
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, result.message.ifEmpty { "Mã không hợp lệ" }, Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeVoucher() {
        appliedVoucher = null
        appliedDiscount = 0
        llAppliedVoucher.visibility = View.GONE
        etVoucherCode.setText("")
        bindSummary()
        Toast.makeText(this, "Đã bỏ mã giảm giá", Toast.LENGTH_SHORT).show()
    }

    private fun calculateShipping(): Int {
        val items = CartManager.getItems()
        return items.groupBy { it.restaurant }.values.sumOf { list ->
            OrderManager.shippingFeeFor(list.sumOf { it.total })
        }
    }

    private fun bindSummary() {
        val container = findViewById<LinearLayout>(R.id.llCheckoutItems)
        container.removeAllViews()

        val items = CartManager.getItems()
        items.forEach { OrderUi.addItemLine(container, it) }

        // Cảnh báo phí ship khi có nhiều nhà hàng
        val restaurants = items.map { it.restaurant }.distinct()
        if (restaurants.size > 1 && !isWarningDismissed) {
            llMultiRestaurantWarning.visibility = View.VISIBLE
            tvMultiRestaurantWarningText.text =
                "Giỏ hàng của bạn gồm món từ ${restaurants.size} quán khác nhau (${restaurants.joinToString(", ")}). Vì mỗi quán là 1 đơn riêng do tài xế lấy món độc lập, phí giao hàng sẽ được tính riêng cho từng quán."
        } else {
            llMultiRestaurantWarning.visibility = View.GONE
        }

        val subtotal = CartManager.totalPrice()
        val shipping = calculateShipping()

        // Kiểm tra lại tính hợp lệ của voucher
        if (appliedVoucher != null) {
            val recheck = VoucherStore.validate(
                appliedVoucher!!.code,
                subtotal,
                shipping,
                this
            )
            if (recheck.success) {
                appliedDiscount = recheck.discount
            } else {
                appliedVoucher = null
                appliedDiscount = 0
                llAppliedVoucher.visibility = View.GONE
            }
        }

        tvCheckoutSubtotal.text = FormatUtils.money(subtotal)
        tvCheckoutShipping.text = if (shipping == 0) "Miễn phí" else FormatUtils.money(shipping)

        if (appliedDiscount > 0) {
            llCheckoutDiscountRow.visibility = View.VISIBLE
            tvCheckoutDiscount.text = "-${FormatUtils.money(appliedDiscount)}"
        } else {
            llCheckoutDiscountRow.visibility = View.GONE
        }

        val finalTotal = (subtotal + shipping - appliedDiscount).coerceAtLeast(0)
        tvCheckoutTotal.text = FormatUtils.money(finalTotal)
    }

    private fun paymentMethod(): String = when (rgPayment.checkedRadioButtonId) {
        R.id.rbMomo -> "Ví MoMo"
        R.id.rbBank -> "Chuyển khoản ngân hàng"
        else -> "Thanh to\u00e1n khi nh\u1eadn h\u00e0ng (COD)"
    }

    private fun confirmOrder() {
        if (CartManager.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show()
            return
        }

        val name = etReceiver.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val orderNote = etOrderNote.text.toString().trim()

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

        val method = paymentMethod()
        val subtotal = CartManager.totalPrice()
        val shipping = calculateShipping()
        val finalTotal = (subtotal + shipping - appliedDiscount).coerceAtLeast(0)

        // Nếu là MoMo hoặc Chuyển khoản ngân hàng -> Hiện hướng dẫn và mã QR giả lập
        if (method == "Ví MoMo" || method == "Chuyển khoản ngân hàng") {
            showPaymentInstructionDialog(name, phone, address, orderNote, method, finalTotal)
        } else {
            // Thanh toán COD -> Tạo đơn ngay
            executeOrderCreation(name, phone, address, orderNote, method)
        }
    }

    private fun showPaymentInstructionDialog(
        name: String,
        phone: String,
        address: String,
        orderNote: String,
        method: String,
        finalTotal: Int
    ) {
        val isMomo = method == "Ví MoMo"
        val title = if (isMomo) "Thanh toán qua Ví MoMo" else "Thanh toán Chuyển khoản Ngân hàng"

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Khung mã QR giả lập
        val qrCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_card)
            setPadding(30, 24, 30, 24)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
        }

        val tvQrIcon = TextView(this).apply {
            text = if (isMomo) "📱" else "🏦"
            textSize = 48f
            gravity = Gravity.CENTER
        }
        val tvQrCodeVisual = TextView(this).apply {
            text = "▣  QR PAYMENT CODE  ▣\n[ FOODHUB SCAN TO PAY ]"
            textSize = 14f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#1F2937"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
        }
        qrCard.addView(tvQrIcon)
        qrCard.addView(tvQrCodeVisual)
        container.addView(qrCard)

        // Bảng thông tin chuyển khoản
        val tvInfo = TextView(this).apply {
            val bankName = if (isMomo) "Ví điện tử MoMo" else "MB Bank (Ngân hàng Quân Đội)"
            val accNumber = if (isMomo) "0988 776 655" else "8888 0988 776 655"
            val owner = "CÔNG TY CỔ PHẦN FOODHUB"
            val syntax = "FOODHUB ${phone.takeLast(6)}"

            text = """
                • Ngân hàng / Ví: $bankName
                • Số tài khoản / Số ví: $accNumber
                • Người nhận: $owner
                • Số tiền cần chuyển: ${FormatUtils.money(finalTotal)}
                • Nội dung chuyển khoản: $syntax
            """.trimIndent()
            textSize = 13f
            setTextColor(Color.parseColor("#374151"))
            setLineSpacing(6f, 1f)
            setPadding(0, 0, 0, 16)
        }
        container.addView(tvInfo)

        val tvNote = TextView(this).apply {
            text = "Vui lòng quét mã hoặc chuyển đúng số tiền trên. Sau khi chuyển xong, bấm nút 'Xác nhận đã chuyển tiền' bên dưới để hoàn tất đơn hàng."
            textSize = 12f
            setTextColor(Color.parseColor("#9CA3AF"))
            setTypeface(null, Typeface.ITALIC)
        }
        container.addView(tvNote)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("✓ Xác nhận đã chuyển tiền") { _, _ ->
                executeOrderCreation(name, phone, address, orderNote, method)
            }
            .setNegativeButton("Hủy / Quay lại", null)
            .show()
    }

    private fun executeOrderCreation(
        name: String,
        phone: String,
        address: String,
        orderNote: String,
        method: String
    ) {
        // Lưu vào sổ địa chỉ nếu chọn checkbox
        if (cbSaveAddress.isChecked) {
            AddressStore.insert(
                this,
                SavedAddress(
                    label = "Nhà",
                    receiver = name,
                    phone = phone,
                    address = address,
                    isDefault = false
                )
            )
        }

        val voucherCode = appliedVoucher?.code ?: ""

        // Tạo đơn hàng thật và lưu vào SQLite
        OrderManager.createOrders(
            cartItems = CartManager.getItems().map { it.copy() },
            receiverName = name,
            phone = phone,
            address = address,
            paymentMethod = method,
            orderNote = orderNote,
            voucherCode = voucherCode,
            discount = appliedDiscount
        )

        // Đánh dấu mã đã dùng
        if (voucherCode.isNotEmpty()) {
            VoucherStore.markUsed(this, voucherCode)
        }

        // Xóa giỏ hàng
        CartManager.clear()

        Toast.makeText(this, "Đặt hàng thành công 🎉 Cảm ơn bạn!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("selected_tab", "orders")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}
