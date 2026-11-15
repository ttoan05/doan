package com.example.foodhub.ui.checkout

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.example.foodhub.data.model.VoucherType
import com.example.foodhub.ui.home.MainActivity
import com.example.foodhub.ui.order.OrderUi
import com.example.foodhub.utils.FormatUtils
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
        val llMomoPreview = findViewById<LinearLayout>(R.id.llMomoPreview)
        val llBankPreview = findViewById<LinearLayout>(R.id.llBankPreview)
        val btnViewMomoQr = findViewById<View>(R.id.btnViewMomoQr)
        val btnViewBankQr = findViewById<View>(R.id.btnViewBankQr)

        fun updatePaymentPreview(checkedId: Int) {
            llMomoPreview?.visibility = if (checkedId == R.id.rbMomo) View.VISIBLE else View.GONE
            llBankPreview?.visibility = if (checkedId == R.id.rbBank) View.VISIBLE else View.GONE
        }

        rgPayment.setOnCheckedChangeListener { _, checkedId ->
            updatePaymentPreview(checkedId)
        }

        // Mặc định luôn là Thanh toán khi nhận hàng (COD)
        PaymentStore.setDefaultMethod(this, PaymentStore.METHOD_COD)
        rgPayment.check(R.id.rbCod)
        updatePaymentPreview(R.id.rbCod)

        btnViewMomoQr?.setOnClickListener {
            val name = etReceiver.text.toString().trim().ifEmpty { "Khách hàng" }
            val phone = etPhone.text.toString().trim().ifEmpty { "0000" }
            val subtotal = CartManager.totalPrice()
            val shipping = calculateShipping()
            val finalTotal = (subtotal + shipping - appliedDiscount).coerceAtLeast(0)
            showPaymentInstructionDialog(
                name = name,
                phone = phone,
                address = etAddress.text.toString().trim(),
                orderNote = etOrderNote.text.toString().trim(),
                method = "Ví MoMo",
                finalTotal = finalTotal,
                isOrderConfirmation = false
            )
        }

        btnViewBankQr?.setOnClickListener {
            val name = etReceiver.text.toString().trim().ifEmpty { "Khách hàng" }
            val phone = etPhone.text.toString().trim().ifEmpty { "0000" }
            val subtotal = CartManager.totalPrice()
            val shipping = calculateShipping()
            val finalTotal = (subtotal + shipping - appliedDiscount).coerceAtLeast(0)
            showPaymentInstructionDialog(
                name = name,
                phone = phone,
                address = etAddress.text.toString().trim(),
                orderNote = etOrderNote.text.toString().trim(),
                method = "Chuyển khoản ngân hàng",
                finalTotal = finalTotal,
                isOrderConfirmation = false
            )
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
        // Lấy voucher khả dụng từ SQLite theo tài khoản hiện tại
        val vouchers = VoucherStore.getAvailableVouchers(this)

        if (vouchers.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Chọn mã giảm giá")
                .setMessage("Hiện tại bạn chưa có voucher nào khả dụng.")
                .setPositiveButton("Đóng", null)
                .show()
            return
        }

        val items = vouchers.map { voucher ->
            val discountInfo = when (voucher.type) {
                VoucherType.PERCENT -> {
                    if (voucher.maxDiscount > 0) "Giảm ${voucher.value}% (Tối đa ${FormatUtils.money(voucher.maxDiscount)})"
                    else "Giảm ${voucher.value}%"
                }
                VoucherType.AMOUNT -> "Giảm ${FormatUtils.money(voucher.value)}"
                VoucherType.FREESHIP -> "Miễn phí vận chuyển"
            }
            val minOrderInfo = if (voucher.minOrder > 0) " • Đơn tối thiểu ${FormatUtils.money(voucher.minOrder)}" else ""
            "${voucher.emoji} [${voucher.code}] $discountInfo$minOrderInfo\n   ${voucher.title}"
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

        // Nếu là MoMo hoặc Chuyển khoản ngân hàng -> Hiện hướng dẫn và mã QR VietQR thực tế
        if (method == "Ví MoMo" || method == "Chuyển khoản ngân hàng") {
            showPaymentInstructionDialog(
                name = name,
                phone = phone,
                address = address,
                orderNote = orderNote,
                method = method,
                finalTotal = finalTotal,
                isOrderConfirmation = true
            )
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
        finalTotal: Int,
        isOrderConfirmation: Boolean = true
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vietqr_payment, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val isMomo = method == "Ví MoMo"
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvQrDialogTitle)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvQrDialogSub)
        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivQrCode)
        val pbQrLoading = dialogView.findViewById<ProgressBar>(R.id.pbQrLoading)
        val tvQrError = dialogView.findViewById<TextView>(R.id.tvQrError)

        val tvBankName = dialogView.findViewById<TextView>(R.id.tvBankName)
        val tvAccountName = dialogView.findViewById<TextView>(R.id.tvAccountName)
        val tvAccountNumber = dialogView.findViewById<TextView>(R.id.tvAccountNumber)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvAmount)
        val tvTransferNote = dialogView.findViewById<TextView>(R.id.tvTransferNote)
        val tvHint = dialogView.findViewById<TextView>(R.id.tvQrHint)

        val btnCopyAccount = dialogView.findViewById<View>(R.id.btnCopyAccount)
        val btnCopyAmount = dialogView.findViewById<View>(R.id.btnCopyAmount)
        val btnCopyNote = dialogView.findViewById<View>(R.id.btnCopyNote)
        val btnConfirmPaid = dialogView.findViewById<Button>(R.id.btnConfirmPaid)
        val btnCancelQrPayment = dialogView.findViewById<View>(R.id.btnCancelQrPayment)
        val btnQrDialogClose = dialogView.findViewById<View>(R.id.btnQrDialogClose)

        // Thông tin tài khoản nhận tiền thực tế của Trương Thanh Toàn (MB Bank)
        val bankCode = "MB"
        val bankDisplayName = "MB Bank (Ngân hàng Quân Đội)"
        val accountNumber = "0867656271"
        val accountName = "TRUONG THANH TOAN"
        val phoneSuffix = phone.filter { it.isDigit() }.takeLast(4).ifEmpty { "0000" }
        val transferSyntax = "FOODHUB $phoneSuffix"

        if (isMomo) {
            tvTitle.text = "Quét mã MoMo thanh toán"
            tvSub.text = "Ví MoMo • Quét mã QR hoặc VietQR chuyển tiền"
            tvBankName.text = "Ví MoMo / MB Bank"
            tvHint?.text = "💡 Mở ứng dụng MoMo hoặc ứng dụng Ngân hàng của bạn để quét mã QR thanh toán nhanh."
        } else {
            tvTitle.text = "Chuyển khoản ngân hàng (VietQR)"
            tvSub.text = "MB Bank • Quét mã QR Napas 24/7"
            tvBankName.text = bankDisplayName
            tvHint?.text = "💡 Mở app MB Bank, Vietcombank, Techcombank, VPBank... hoặc MoMo để quét mã VietQR chuyển khoản nhanh 24/7."
        }

        tvAccountName.text = accountName
        tvAccountNumber.text = accountNumber
        tvAmount.text = FormatUtils.money(finalTotal)
        tvTransferNote.text = transferSyntax

        fun copyToClipboard(label: String, text: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Đã sao chép $label: $text", Toast.LENGTH_SHORT).show()
        }

        btnCopyAccount.setOnClickListener { copyToClipboard("Số tài khoản", accountNumber) }
        btnCopyAmount.setOnClickListener { copyToClipboard("Số tiền", finalTotal.toString()) }
        btnCopyNote.setOnClickListener { copyToClipboard("Nội dung", transferSyntax) }

        if (isOrderConfirmation) {
            btnConfirmPaid.text = if (isMomo) "✓  TÔI ĐÃ THANH TOÁN MOMO THÀNH CÔNG" else "✓  TÔI ĐÃ CHUYỂN KHOẢN THÀNH CÔNG"
            btnConfirmPaid.setOnClickListener {
                dialog.dismiss()
                executeOrderCreation(name, phone, address, orderNote, method)
            }
            btnCancelQrPayment.visibility = View.VISIBLE
            btnCancelQrPayment.setOnClickListener { dialog.dismiss() }
        } else {
            btnConfirmPaid.text = "ĐÃ HIỂU / ĐÓNG"
            btnConfirmPaid.setOnClickListener { dialog.dismiss() }
            btnCancelQrPayment.visibility = View.GONE
        }

        btnQrDialogClose.setOnClickListener { dialog.dismiss() }

        // Hiển thị ngay lập tức mã QR chuẩn nét offline (0ms latency, không sợ rớt mạng)
        try {
            val localQrBitmap = com.example.foodhub.utils.QrCodeGenerator.createPaymentQrBitmap(
                isMomo = isMomo,
                bankCode = bankCode,
                accountNumber = accountNumber,
                accountName = accountName,
                amount = finalTotal,
                transferNote = transferSyntax
            )
            ivQrCode.setImageBitmap(localQrBitmap)
            ivQrCode.visibility = View.VISIBLE
            pbQrLoading.visibility = View.GONE
            tvQrError.visibility = View.GONE
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Tải ảnh mã QR động từ VietQR API nếu có mạng
        Thread {
            try {
                val cleanSyntax = transferSyntax.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim()
                val encodedSyntax = URLEncoder.encode(cleanSyntax, "UTF-8").replace("+", "%20")
                val encodedName = URLEncoder.encode(accountName, "UTF-8").replace("+", "%20")
                val qrUrl = "https://img.vietqr.io/image/970422-$accountNumber-compact2.png?amount=$finalTotal&addInfo=$encodedSyntax&accountName=$encodedName"

                val url = URL(qrUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.doInput = true
                conn.connect()
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                if (bitmap != null) {
                    runOnUiThread {
                        ivQrCode.setImageBitmap(bitmap)
                        pbQrLoading.visibility = View.GONE
                        ivQrCode.visibility = View.VISIBLE
                        tvQrError.visibility = View.GONE
                    }
                }
            } catch (ignored: Throwable) {
                // Giữ nguyên ảnh QR offline đã hiển thị từ trước
            }
        }.start()

        dialog.show()
    }


    private fun executeOrderCreation(
        name: String,
        phone: String,
        address: String,
        orderNote: String,
        method: String
    ) {
        // Kiểm tra giỏ hàng trước khi tạo đơn
        val cartItems = CartManager.getItems().map { it.copy() }

        if (cartItems.isEmpty()) {
            Toast.makeText(
                this,
                "Giỏ hàng đang trống",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val voucherCode = appliedVoucher?.code ?: etVoucherCode.text.toString().trim()

        // Tạo đơn hàng và lưu vào SQLite
        val createdOrders = OrderManager.createOrders(
            cartItems = cartItems,
            receiverName = name,
            phone = phone,
            address = address,
            paymentMethod = method,
            orderNote = orderNote,
            voucherCode = voucherCode,
            discount = appliedDiscount,
            context = this
        )

        // Nếu không lưu được đơn hàng thì dừng tại đây
        if (createdOrders.isEmpty()) {
            Toast.makeText(
                this,
                "Không thể tạo đơn hàng. Vui lòng thử lại!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Chỉ lưu địa chỉ sau khi tạo đơn thành công
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

        // Đánh dấu voucher đã sử dụng sau khi lưu đơn thành công
        if (voucherCode.isNotEmpty()) {
            VoucherStore.markUsed(this, voucherCode)
        }

        // Xóa giỏ hàng sau khi tạo đơn thành công
        CartManager.clear()

        Toast.makeText(
            this,
            "Đặt hàng thành công 🎉 Cảm ơn bạn!",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            putExtra("selected_tab", "orders")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

}
