package com.example.foodhub.ui.order

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.ReviewStore
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.data.model.Review
import com.example.foodhub.ui.cart.CartActivity
import com.example.foodhub.utils.FormatUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.ProgressBar
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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
        findViewById<Button>(R.id.btnWriteReview).setOnClickListener { showReviewDialog() }

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
        val order = OrderManager.findById(orderId) ?: DatabaseHelper.getInstance(this).getOrderById(orderId)
        if (order == null) {
            finish()
            return
        }

        val tvRestaurant = findViewById<TextView>(R.id.tvDetailRestaurant)
        tvRestaurant.text = order.restaurant
        val restaurantPhone = getRestaurantPhone(order.restaurant)
        val onCallRestaurant = View.OnClickListener {
            dialPhoneNumber(restaurantPhone, order.restaurant)
        }
        tvRestaurant.setOnClickListener(onCallRestaurant)
        findViewById<View?>(R.id.btnCallRestaurant)?.setOnClickListener(onCallRestaurant)

        findViewById<TextView>(R.id.tvDetailCode).text = "Mã đơn hàng: ${order.code}"
        findViewById<TextView>(R.id.tvDetailDate).text =
            "Ngày đặt: ${FormatUtils.dateTime(order.createdAt)}"
        OrderUi.styleStatus(findViewById(R.id.tvDetailStatus), order.status)

        bindProgress(order)
        bindItems(order)

        findViewById<TextView>(R.id.tvDetailSubtotal).text = FormatUtils.money(order.subtotal)
        findViewById<TextView>(R.id.tvDetailShipping).text =
            if (order.shippingFee == 0) "Miễn phí" else FormatUtils.money(order.shippingFee)

        // Hiển thị chiết khấu voucher nếu có
        val llDiscount = findViewById<LinearLayout>(R.id.llDetailDiscountRow)
        val tvDiscount = findViewById<TextView>(R.id.tvDetailDiscount)
        val tvDiscountLabel = findViewById<TextView>(R.id.tvDetailDiscountLabel)
        if (order.discount > 0) {
            llDiscount.visibility = View.VISIBLE
            val labelText = if (order.voucherCode.isNotEmpty()) {
                "Giảm giá voucher (${order.voucherCode})"
            } else {
                "Giảm giá voucher"
            }
            tvDiscountLabel.text = labelText
            tvDiscount.text = "-${FormatUtils.money(order.discount)}"
        } else {
            llDiscount.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvDetailTotal).text = FormatUtils.money(order.total)

        findViewById<TextView>(R.id.tvDetailReceiver).text = order.receiverName

        val tvPhone = findViewById<TextView>(R.id.tvDetailPhone)
        tvPhone.text = order.phone
        val onCallPhone = View.OnClickListener {
            dialPhoneNumber(order.phone, order.receiverName)
        }
        tvPhone.setOnClickListener(onCallPhone)
        findViewById<View?>(R.id.btnCallPhone)?.setOnClickListener(onCallPhone)

        findViewById<TextView>(R.id.tvDetailAddress).text = order.address
        findViewById<TextView>(R.id.tvDetailPayment).text = order.paymentMethod

        val btnDetailViewQr = findViewById<TextView>(R.id.btnDetailViewQr)
        val isQrPayment = order.paymentMethod.contains("Chuyển khoản", ignoreCase = true) ||
                order.paymentMethod.contains("MoMo", ignoreCase = true) ||
                order.paymentMethod.contains("VietQR", ignoreCase = true)
        if (isQrPayment) {
            btnDetailViewQr.visibility = View.VISIBLE
            btnDetailViewQr.text = if (order.paymentMethod.contains("MoMo", ignoreCase = true)) {
                "Xem mã QR MoMo thanh toán"
            } else {
                "Xem mã QR chuyển khoản (VietQR)"
            }
            btnDetailViewQr.setOnClickListener {
                showQrPaymentDialog(order)
            }
        } else {
            btnDetailViewQr.visibility = View.GONE
        }

        // Ghi chú đơn hàng
        val llNote = findViewById<LinearLayout>(R.id.llDetailOrderNote)
        val tvNote = findViewById<TextView>(R.id.tvDetailOrderNote)
        if (order.orderNote.isNotBlank()) {
            llNote.visibility = View.VISIBLE
            tvNote.text = order.orderNote
        } else {
            llNote.visibility = View.GONE
        }

        // Đánh giá đơn hàng (Review)
        bindReviewSection(order)

        // Nút phía dưới
        val btnCancel = findViewById<Button>(R.id.btnCancelOrder)
        val btnReorder = findViewById<Button>(R.id.btnReorderDetail)
        btnCancel.visibility = if (order.canCancel()) View.VISIBLE else View.GONE
        btnReorder.visibility = if (order.canReorder()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.llActions).visibility =
            if (order.canCancel() || order.canReorder()) View.VISIBLE else View.GONE
    }

    private fun bindReviewSection(order: Order) {
        val llReview = findViewById<LinearLayout>(R.id.llReviewCard)
        val tvStars = findViewById<TextView>(R.id.tvReviewStars)
        val tvComment = findViewById<TextView>(R.id.tvReviewComment)
        val btnWriteReview = findViewById<Button>(R.id.btnWriteReview)

        // Chỉ hiển thị khu vực đánh giá khi đơn hàng đã giao thành công
        if (order.status != OrderStatus.DELIVERED) {
            llReview.visibility = View.GONE
            return
        }

        llReview.visibility = View.VISIBLE
        val existingReview = ReviewStore.getForOrder(this, order.id)

        if (existingReview != null) {
            tvStars.visibility = View.VISIBLE
            tvStars.text = "⭐️".repeat(existingReview.stars) + " (${existingReview.stars}/5)"
            if (existingReview.comment.isNotBlank()) {
                tvComment.visibility = View.VISIBLE
                tvComment.text = "\"${existingReview.comment}\""
            } else {
                tvComment.visibility = View.GONE
            }
            btnWriteReview.visibility = View.GONE
        } else {
            tvStars.visibility = View.VISIBLE
            tvStars.text = "Bạn chưa đánh giá đơn hàng này"
            tvComment.visibility = View.GONE
            btnWriteReview.visibility = View.VISIBLE
        }
    }

    private fun showReviewDialog() {
        val order = OrderManager.findById(orderId) ?: DatabaseHelper.getInstance(this).getOrderById(orderId) ?: return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 24)
        }

        val tvTitle = TextView(this).apply {
            text = "Bạn đánh giá món ăn từ ${order.restaurant} như thế nào?"
            textSize = 14f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(0, 0, 0, 16)
        }
        container.addView(tvTitle)

        // 5 ngôi sao tương tác
        var selectedStars = 5
        val starsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        val starViews = mutableListOf<TextView>()

        fun updateStarDisplay() {
            starViews.forEachIndexed { index, tv ->
                tv.text = if (index < selectedStars) "⭐️" else "☆"
            }
        }

        for (i in 1..5) {
            val tvStar = TextView(this).apply {
                text = "⭐️"
                textSize = 32f
                setPadding(8, 0, 8, 0)
                setOnClickListener {
                    selectedStars = i
                    updateStarDisplay()
                }
            }
            starViews.add(tvStar)
            starsLayout.addView(tvStar)
        }
        container.addView(starsLayout)

        val etComment = EditText(this).apply {
            hint = "Nhập cảm nhận của bạn về món ăn, thái độ giao hàng... (không bắt buộc)"
            textSize = 14f
            minLines = 3
            setBackgroundResource(R.drawable.bg_field)
            setPadding(20, 20, 20, 20)
        }
        container.addView(etComment)

        AlertDialog.Builder(this)
            .setTitle("Đánh giá món ăn")
            .setView(container)
            .setPositiveButton("Gửi đánh giá") { _, _ ->
                val comment = etComment.text.toString().trim()
                ReviewStore.addReview(
                    this,
                    Review(
                        orderId = order.id,
                        restaurant = order.restaurant,
                        stars = selectedStars,
                        comment = comment,
                        createdAt = System.currentTimeMillis()
                    )
                )
                Toast.makeText(this, "Cảm ơn bạn đã đánh giá ${selectedStars} sao! ❤️", Toast.LENGTH_SHORT).show()
                bind()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ─── Thanh tiến trình 5 bước ───────────────────────
    private fun bindProgress(order: Order) {
        val container = findViewById<LinearLayout>(R.id.llProgress)
        container.removeAllViews()

        val currentStep = order.progressStep()
        val isCancelled = currentStep == -1

        val inflater = LayoutInflater.from(this)

        steps.forEachIndexed { index, name ->
            val stepView = inflater.inflate(R.layout.item_order_step, container, false)
            val tvDot = stepView.findViewById<TextView>(R.id.tvStepDot)
            val tvLabel = stepView.findViewById<TextView>(R.id.tvStepLabel)
            val vLine = stepView.findViewById<View>(R.id.vStepLine)

            tvLabel.text = name

            if (index == steps.size - 1) {
                vLine.visibility = View.INVISIBLE
            } else {
                vLine.visibility = View.VISIBLE
            }

            when {
                isCancelled -> {
                    tvDot.text = "✕"
                    tvDot.setBackgroundResource(R.drawable.bg_step_pending)
                    tvLabel.setTextColor(Color.parseColor("#9E9E9E"))
                    vLine.setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                index < currentStep -> {
                    tvDot.text = "✓"
                    tvDot.setBackgroundResource(R.drawable.bg_step_done)
                    tvLabel.setTextColor(Color.parseColor("#1B1B1B"))
                    vLine.setBackgroundColor(Color.parseColor("#E63946"))
                }
                index == currentStep -> {
                    tvDot.text = "${index + 1}"
                    tvDot.setBackgroundResource(R.drawable.bg_step_done)
                    tvLabel.setTextColor(Color.parseColor("#E63946"))
                    vLine.setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                else -> {
                    tvDot.text = "${index + 1}"
                    tvDot.setBackgroundResource(R.drawable.bg_step_pending)
                    tvLabel.setTextColor(Color.parseColor("#9E9E9E"))
                    vLine.setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
            }

            container.addView(stepView)
        }
    }

    // ─── Danh sách món đã đặt ─────────────────────────
    private fun bindItems(order: Order) {
        val container = findViewById<LinearLayout>(R.id.llDetailItems)
        container.removeAllViews()
        order.items.forEach { item ->
            OrderUi.addItemLine(container, item)
        }
    }

    // ─── Thao tác người dùng ──────────────────────────
    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận hủy đơn")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
            .setPositiveButton("Hủy đơn") { _, _ ->
                val ok = OrderManager.cancel(orderId)
                if (ok) {
                    Toast.makeText(this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show()
                    bind()
                } else {
                    Toast.makeText(this, "Không thể hủy đơn hàng này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun reorder() {
        val order = OrderManager.findById(orderId) ?: DatabaseHelper.getInstance(this).getOrderById(orderId) ?: return
        CartManager.addItems(order.items)
        Toast.makeText(this, "Đã thêm lại các món vào giỏ hàng", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, CartActivity::class.java))
    }

    private fun showQrPaymentDialog(order: Order) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vietqr_payment, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val isMomo = order.paymentMethod.contains("MoMo", ignoreCase = true)
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

        val bankCode = "MB"
        val bankDisplayName = "MB Bank (Ngân hàng Quân Đội)"
        val accountNumber = "0867656271"
        val accountName = "TRUONG THANH TOAN"
        val phoneSuffix = order.phone.filter { it.isDigit() }.takeLast(4).ifEmpty { "0000" }
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
        tvAmount.text = FormatUtils.money(order.total)
        tvTransferNote.text = transferSyntax

        fun copyToClipboard(label: String, text: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Đã sao chép $label: $text", Toast.LENGTH_SHORT).show()
        }

        btnCopyAccount.setOnClickListener { copyToClipboard("Số tài khoản", accountNumber) }
        btnCopyAmount.setOnClickListener { copyToClipboard("Số tiền", order.total.toString()) }
        btnCopyNote.setOnClickListener { copyToClipboard("Nội dung", transferSyntax) }

        btnConfirmPaid.text = "ĐÃ THANH TOÁN XONG / ĐÓNG"
        btnConfirmPaid.setOnClickListener { dialog.dismiss() }
        btnCancelQrPayment.visibility = View.GONE
        btnQrDialogClose.setOnClickListener { dialog.dismiss() }

        // Tải ảnh mã QR động từ VietQR API
        Thread {
            try {
                val encodedSyntax = URLEncoder.encode(transferSyntax, "UTF-8")
                val encodedName = URLEncoder.encode(accountName, "UTF-8")
                val qrUrl = "https://img.vietqr.io/image/$bankCode-$accountNumber-compact2.png?amount=${order.total}&addInfo=$encodedSyntax&accountName=$encodedName"

                val url = URL(qrUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.doInput = true
                conn.connect()
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                runOnUiThread {
                    if (bitmap != null) {
                        ivQrCode.setImageBitmap(bitmap)
                        pbQrLoading.visibility = View.GONE
                        ivQrCode.visibility = View.VISIBLE
                    } else {
                        pbQrLoading.visibility = View.GONE
                        tvQrError.visibility = View.VISIBLE
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                runOnUiThread {
                    pbQrLoading.visibility = View.GONE
                    tvQrError.visibility = View.VISIBLE
                }
            }
        }.start()

        dialog.show()
    }

    private fun dialPhoneNumber(rawPhone: String, title: String = "") {
        val cleanPhone = rawPhone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(this, "Không có số điện thoại hợp lệ để gọi", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở ứng dụng gọi điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRestaurantPhone(restaurantName: String): String {
        return when {
            restaurantName.contains("Phở Tuyệt", ignoreCase = true) -> "0901234567"
            restaurantName.contains("KFC", ignoreCase = true) -> "19006886"
            restaurantName.contains("Bánh Mì", ignoreCase = true) -> "0912345678"
            restaurantName.contains("Cơm Tấm", ignoreCase = true) -> "0923456789"
            restaurantName.contains("Mì Quảng", ignoreCase = true) -> "0934567890"
            restaurantName.contains("Trà Sữa", ignoreCase = true) -> "0945678901"
            else -> "0867656271" // Hotline FoodHub kết nối quán
        }
    }
}
