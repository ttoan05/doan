package com.example.foodhub.ui.order

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.ReviewStore
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.Review
import com.example.foodhub.utils.FormatUtils
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Helper quản lý hộp thoại hiển thị mã QR thanh toán (VietQR/MoMo), đánh giá đơn hàng và gọi điện cho OrderDetailActivity.
 */
object OrderDetailDialogHelper {

    fun showQrPaymentDialog(activity: Activity, order: Order) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_vietqr_payment, null)
        val dialog = AlertDialog.Builder(activity)
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
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, "Đã sao chép $label: $text", Toast.LENGTH_SHORT).show()
        }

        btnCopyAccount.setOnClickListener { copyToClipboard("Số tài khoản", accountNumber) }
        btnCopyAmount.setOnClickListener { copyToClipboard("Số tiền", order.total.toString()) }
        btnCopyNote.setOnClickListener { copyToClipboard("Nội dung", transferSyntax) }

        btnConfirmPaid.text = "ĐÃ THANH TOÁN XONG / ĐÓNG"
        btnConfirmPaid.setOnClickListener { dialog.dismiss() }
        btnCancelQrPayment.visibility = View.GONE
        btnQrDialogClose.setOnClickListener { dialog.dismiss() }

        // Hiển thị ngay lập tức mã QR chuẩn nét offline (0ms latency, không sợ rớt mạng)
        try {
            val localQrBitmap = com.example.foodhub.utils.QrCodeGenerator.createPaymentQrBitmap(
                isMomo = isMomo,
                bankCode = bankCode,
                accountNumber = accountNumber,
                accountName = accountName,
                amount = order.total,
                transferNote = transferSyntax
            )
            ivQrCode.setImageBitmap(localQrBitmap)
            ivQrCode.visibility = View.VISIBLE
            pbQrLoading.visibility = View.GONE
            tvQrError.visibility = View.GONE
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        Thread {
            try {
                val cleanSyntax = transferSyntax.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim()
                val encodedSyntax = URLEncoder.encode(cleanSyntax, "UTF-8").replace("+", "%20")
                val encodedName = URLEncoder.encode(accountName, "UTF-8").replace("+", "%20")
                val qrUrl = "https://img.vietqr.io/image/970422-$accountNumber-compact2.png?amount=${order.total}&addInfo=$encodedSyntax&accountName=$encodedName"

                val url = URL(qrUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.doInput = true
                conn.connect()
                val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                if (bitmap != null) {
                    activity.runOnUiThread {
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

    fun showReviewDialog(activity: Activity, orderId: String, onReviewSubmitted: () -> Unit) {
        val order = OrderManager.findById(orderId) ?: DatabaseHelper.getInstance(activity).getOrderById(orderId) ?: return

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 28, 44, 20)
        }

        val tvTitle = TextView(activity).apply {
            text = "Bạn đánh giá món ăn từ ${order.restaurant} như thế nào?"
            textSize = 14f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(0, 0, 0, 16)
        }
        container.addView(tvTitle)

        var selectedStars = 5
        val starsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        val starViews = mutableListOf<TextView>()

        fun updateStarDisplay() {
            starViews.forEachIndexed { index, tv ->
                tv.text = if (index < selectedStars) "⭐" else "☆"
            }
        }

        for (i in 1..5) {
            val tvStar = TextView(activity).apply {
                text = "⭐"
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

        val etComment = EditText(activity).apply {
            hint = "Nhập cảm nhận của bạn về món ăn, hương vị, giao hàng... (không bắt buộc)"
            textSize = 14f
            minLines = 3
            setBackgroundResource(R.drawable.bg_field)
            setPadding(24, 20, 24, 20)
        }
        container.addView(etComment)

        // Mục đính kèm hình ảnh thực tế
        var selectedImagePath: String? = null
        val density = activity.resources.displayMetrics.density

        val imageSection = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 8)
        }

        val btnPickImage = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_field)
            setPadding(24, 24, 24, 24)
            isClickable = true
            isFocusable = true
        }

        val tvPickLabel = TextView(activity).apply {
            text = "📷 Thêm ảnh món ăn thực tế"
            textSize = 13.5f
            setTextColor(Color.parseColor("#374151"))
        }
        btnPickImage.addView(tvPickLabel)
        imageSection.addView(btnPickImage)

        val previewContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 8, 0, 0)
        }

        val ivPreview = ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (170 * density).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_field)
            clipToOutline = true
        }
        previewContainer.addView(ivPreview)

        val btnRemoveImage = TextView(activity).apply {
            text = "✕ Xóa ảnh này"
            textSize = 12.5f
            setTextColor(Color.parseColor("#EF4444"))
            setPadding(0, 8, 0, 4)
            isClickable = true
            isFocusable = true
        }
        previewContainer.addView(btnRemoveImage)
        imageSection.addView(previewContainer)
        container.addView(imageSection)

        btnPickImage.setOnClickListener {
            if (activity is OrderDetailActivity) {
                activity.pickReviewImage { path ->
                    selectedImagePath = path
                    val bm = BitmapFactory.decodeFile(path)
                    if (bm != null) {
                        ivPreview.setImageBitmap(bm)
                        previewContainer.visibility = View.VISIBLE
                        btnPickImage.visibility = View.GONE
                    }
                }
            } else {
                Toast.makeText(activity, "Không thể mở trình chọn ảnh", Toast.LENGTH_SHORT).show()
            }
        }

        btnRemoveImage.setOnClickListener {
            selectedImagePath = null
            previewContainer.visibility = View.GONE
            btnPickImage.visibility = View.VISIBLE
        }

        AlertDialog.Builder(activity)
            .setTitle("Đánh giá món ăn")
            .setView(container)
            .setPositiveButton("Gửi đánh giá") { _, _ ->
                val comment = etComment.text.toString().trim()
                ReviewStore.addReview(
                    activity,
                    Review(
                        orderId = order.id,
                        restaurant = order.restaurant,
                        stars = selectedStars,
                        comment = comment,
                        imageUri = selectedImagePath,
                        createdAt = System.currentTimeMillis()
                    )
                )
                val msg = if (selectedImagePath != null) {
                    "Cảm ơn bạn đã gửi đánh giá $selectedStars sao kèm ảnh thực tế! ❤️"
                } else {
                    "Cảm ơn bạn đã đánh giá $selectedStars sao! ❤️"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                onReviewSubmitted()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return com.example.foodhub.utils.ImageUtils.saveUriToInternalStorage(context, uri, "review")
    }

    fun dialPhoneNumber(context: Context, rawPhone: String) {
        val cleanPhone = rawPhone.replace(Regex("[^0-9+]"), "")
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Không có số điện thoại hợp lệ để gọi", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng gọi điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    fun getRestaurantPhone(restaurantName: String): String {
        return when {
            restaurantName.contains("Phở Tuyệt", ignoreCase = true) -> "0901234567"
            restaurantName.contains("KFC", ignoreCase = true) -> "19006886"
            restaurantName.contains("Bánh Mì", ignoreCase = true) -> "0912345678"
            restaurantName.contains("Cơm Tấm", ignoreCase = true) -> "0923456789"
            restaurantName.contains("Mì Quảng", ignoreCase = true) -> "0934567890"
            restaurantName.contains("Trà Sữa", ignoreCase = true) -> "0945678901"
            else -> "0867656271"
        }
    }
}
