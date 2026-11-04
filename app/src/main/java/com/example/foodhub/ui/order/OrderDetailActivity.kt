package com.example.foodhub.ui.order

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.ChatStore
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.ReviewStore
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.cart.CartActivity
import com.example.foodhub.utils.FormatUtils
import java.io.File

class OrderDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "order_id"
    }

    private var orderId: String = ""

    private val steps = listOf(
        "Đã đặt hàng",
        "Nhà hàng xác nhận",
        "Đang chuẩn bị",
        "Đang giao",
        "Đã giao"
    )

    private var pendingReviewImageCallback: ((String) -> Unit)? = null

    private val pickReviewImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = OrderDetailDialogHelper.saveImageToInternalStorage(this, uri)
            if (savedPath != null) {
                pendingReviewImageCallback?.invoke(savedPath)
            } else {
                Toast.makeText(this, "Không thể lưu ảnh từ thiết bị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pickReviewImage(callback: (String) -> Unit) {
        pendingReviewImageCallback = callback
        pickReviewImageLauncher.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnCancelOrder).setOnClickListener { confirmCancel() }
        findViewById<Button>(R.id.btnReorderDetail).setOnClickListener { reorder() }
        findViewById<Button>(R.id.btnWriteReview).setOnClickListener {
            OrderDetailDialogHelper.showReviewDialog(this, orderId) { bind() }
        }

        bind()
    }

    override fun onResume() {
        super.onResume()
        OrderManager.onChanged = { bind() }
        bind()
    }

    override fun onPause() {
        super.onPause()
        OrderManager.onChanged = null
    }

    private fun bind() {
        val order = OrderManager.findById(orderId) ?: DatabaseHelper.getInstance(this).getOrderById(orderId)
        if (order == null) {
            finish()
            return
        }

        val tvRestaurant = findViewById<TextView>(R.id.tvDetailRestaurant)
        tvRestaurant.text = order.restaurant
        val restaurantPhone = OrderDetailDialogHelper.getRestaurantPhone(order.restaurant)
        val onCallRestaurant = View.OnClickListener {
            OrderDetailDialogHelper.dialPhoneNumber(this, restaurantPhone)
        }
        tvRestaurant.setOnClickListener(onCallRestaurant)
        findViewById<View?>(R.id.btnCallRestaurant)?.setOnClickListener(onCallRestaurant)

        findViewById<TextView>(R.id.tvDetailCode).text = "Mã đơn hàng: ${order.code}"
        findViewById<TextView>(R.id.tvDetailDate).text =
            "Ngày đặt: ${FormatUtils.dateTime(order.createdAt)}"
        OrderUi.styleStatus(findViewById(R.id.tvDetailStatus), order.status)

        bindProgress(order)

        // Thẻ tài xế giao hàng (Shipper) & Nhắn tin
        val llShipper = findViewById<View>(R.id.llShipperCard)
        if (order.status == OrderStatus.CANCELLED) {
            llShipper.visibility = View.GONE
        } else {
            llShipper.visibility = View.VISIBLE
            val shipper = ChatStore.getShipperInfo(order.id)
            findViewById<TextView>(R.id.tvShipperDetailName).text = shipper.name
            findViewById<TextView>(R.id.tvShipperDetailVehicle).text = "${shipper.bikeInfo} • ${shipper.rating}"

            findViewById<View>(R.id.btnCallShipperDetail).setOnClickListener {
                OrderDetailDialogHelper.dialPhoneNumber(this, shipper.phone)
            }
            findViewById<View>(R.id.btnChatShipperDetail).setOnClickListener {
                val intent = Intent(this, OrderChatActivity::class.java).apply {
                    putExtra(OrderChatActivity.EXTRA_ORDER_ID, order.id)
                    putExtra(OrderChatActivity.EXTRA_ORDER_CODE, order.code)
                }
                startActivity(intent)
            }
        }

        bindItems(order)

        findViewById<TextView>(R.id.tvDetailSubtotal).text = FormatUtils.money(order.subtotal)
        findViewById<TextView>(R.id.tvDetailShipping).text =
            if (order.shippingFee == 0) "Miễn phí" else FormatUtils.money(order.shippingFee)

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
            OrderDetailDialogHelper.dialPhoneNumber(this, order.phone)
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
                OrderDetailDialogHelper.showQrPaymentDialog(this, order)
            }
        } else {
            btnDetailViewQr.visibility = View.GONE
        }

        val llNote = findViewById<LinearLayout>(R.id.llDetailOrderNote)
        val tvNote = findViewById<TextView>(R.id.tvDetailOrderNote)
        if (order.orderNote.isNotBlank()) {
            llNote.visibility = View.VISIBLE
            tvNote.text = order.orderNote
        } else {
            llNote.visibility = View.GONE
        }

        bindReviewSection(order)

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
        val ivReviewImage = findViewById<ImageView>(R.id.ivReviewImage)
        val btnWriteReview = findViewById<Button>(R.id.btnWriteReview)

        if (order.status != OrderStatus.DELIVERED) {
            llReview.visibility = View.GONE
            return
        }

        llReview.visibility = View.VISIBLE
        val existingReview = ReviewStore.getForOrder(this, order.id)

        if (existingReview != null) {
            tvStars.visibility = View.VISIBLE
            tvStars.text = "⭐".repeat(existingReview.stars) + " (${existingReview.stars}/5)"
            if (existingReview.comment.isNotBlank()) {
                tvComment.visibility = View.VISIBLE
                tvComment.text = "\"${existingReview.comment}\""
            } else {
                tvComment.visibility = View.GONE
            }

            val imgUri = existingReview.imageUri
            if (!imgUri.isNullOrEmpty()) {
                val file = File(imgUri)
                if (file.exists()) {
                    val bm = BitmapFactory.decodeFile(file.absolutePath)
                    if (bm != null) {
                        ivReviewImage.visibility = View.VISIBLE
                        ivReviewImage.setImageBitmap(bm)
                    } else {
                        ivReviewImage.visibility = View.GONE
                    }
                } else {
                    ivReviewImage.visibility = View.GONE
                }
            } else {
                ivReviewImage.visibility = View.GONE
            }

            btnWriteReview.visibility = View.GONE
        } else {
            tvStars.visibility = View.VISIBLE
            tvStars.text = "Bạn chưa đánh giá đơn hàng này"
            tvComment.visibility = View.GONE
            ivReviewImage.visibility = View.GONE
            btnWriteReview.visibility = View.VISIBLE
        }
    }

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

    private fun bindItems(order: Order) {
        val container = findViewById<LinearLayout>(R.id.llDetailItems)
        container.removeAllViews()
        order.items.forEach { item ->
            OrderUi.addItemLine(container, item)
        }
    }

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
}
