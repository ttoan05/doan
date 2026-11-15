package com.example.foodhub.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.Voucher
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminVoucherActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var llVoucherContainer: LinearLayout
    private lateinit var tvVoucherCount: TextView
    private lateinit var tvVoucherEmpty: TextView
    private lateinit var etSearchVoucher: EditText

    private var allVouchers: List<Voucher> = emptyList()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_voucher)

        dbHelper = DatabaseHelper.getInstance(this)

        // Chỉ Admin mới được truy cập
        if (!UserProfileManager.isAdmin) {
            Toast.makeText(
                this,
                "Chỉ Quản trị viên mới có quyền truy cập!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        // Ánh xạ View
        llVoucherContainer = findViewById(R.id.llVoucherContainer)
        tvVoucherCount = findViewById(R.id.tvVoucherCount)
        tvVoucherEmpty = findViewById(R.id.tvVoucherEmpty)
        etSearchVoucher = findViewById(R.id.etSearchVoucher)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnAddVoucher).setOnClickListener {
            AdminVoucherDialogHelper.showAddVoucherDialog(this) {
                loadData()
            }
        }

        // Tìm kiếm voucher
        etSearchVoucher.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            renderVoucherList()
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()

        // Tránh truy cập View trước khi kiểm tra quyền Admin
        if (::llVoucherContainer.isInitialized) {
            loadData()
        }
    }

    private fun loadData() {
        allVouchers = dbHelper.getAllVouchers()
        renderVoucherList()
    }

    private fun renderVoucherList() {
        llVoucherContainer.removeAllViews()

        // Lọc voucher theo mã, tên và mô tả
        val filtered = if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()

            allVouchers.filter { voucher ->
                voucher.code.lowercase().contains(query) ||
                        voucher.title.lowercase().contains(query) ||
                        voucher.desc.lowercase().contains(query)
            }
        } else {
            allVouchers
        }

        tvVoucherCount.text =
            "Hiển thị ${filtered.size} / ${allVouchers.size} voucher"

        if (filtered.isEmpty()) {
            tvVoucherEmpty.visibility = View.VISIBLE
            tvVoucherEmpty.text = if (searchQuery.isNotEmpty()) {
                "Không tìm thấy voucher phù hợp với \"$searchQuery\""
            } else {
                "Chưa có voucher nào. Hãy thêm voucher mới!"
            }
            return
        }

        tvVoucherEmpty.visibility = View.GONE

        val inflater = LayoutInflater.from(this)

        filtered.forEach { voucher ->
            val itemView = inflater.inflate(
                R.layout.item_admin_voucher,
                llVoucherContainer,
                false
            )

            val tvEmoji =
                itemView.findViewById<TextView>(R.id.tvVoucherEmoji)

            val tvTitle =
                itemView.findViewById<TextView>(R.id.tvVoucherTitle)

            val tvStatus =
                itemView.findViewById<TextView>(R.id.tvVoucherStatus)

            val tvCode =
                itemView.findViewById<TextView>(R.id.tvVoucherCode)

            val tvDescription =
                itemView.findViewById<TextView>(R.id.tvVoucherDescription)

            val tvDiscount =
                itemView.findViewById<TextView>(R.id.tvVoucherDiscount)

            val tvCondition =
                itemView.findViewById<TextView>(R.id.tvVoucherCondition)

            val tvQuantity =
                itemView.findViewById<TextView>(R.id.tvVoucherQuantity)

            val tvTime =
                itemView.findViewById<TextView>(R.id.tvVoucherTime)

            val btnToggle =
                itemView.findViewById<TextView>(R.id.btnToggleVoucher)

            val btnEdit =
                itemView.findViewById<View>(R.id.btnEditVoucher)

            val btnDelete =
                itemView.findViewById<View>(R.id.btnDeleteVoucher)

            // Hiển thị thông tin
            tvEmoji?.text = if (voucher.emoji.isNotBlank()) voucher.emoji else "🎟️"
            tvTitle.text = voucher.title
            tvCode.text = "Mã: ${voucher.code}"
            tvDescription.text = voucher.desc

            tvDiscount.text = getDiscountText(voucher)

            tvCondition.text =
                "Đơn tối thiểu: ${formatMoney(voucher.minOrder)}"

            val usedCount = dbHelper.getVoucherUsageCount(voucher.id)
            tvQuantity.text =
                "Đã có $usedCount tài khoản sử dụng"

            tvTime.text =
                "Thời gian: ${formatDate(voucher.startAt)} - ${formatDate(voucher.endAt)}"

            // Trạng thái badge và nút bật/tắt đồng bộ giao diện
            if (voucher.isActive) {
                tvStatus.text = "● Đang hoạt động"
                tvStatus.setTextColor(Color.parseColor("#16A34A"))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_status_green)

                btnToggle.text = "Tạm tắt"
                btnToggle.setTextColor(Color.parseColor("#475569"))
                btnToggle.setBackgroundResource(R.drawable.bg_btn_cancel)
            } else {
                tvStatus.text = "● Đã tắt"
                tvStatus.setTextColor(Color.parseColor("#DC2626"))
                tvStatus.setBackgroundResource(R.drawable.bg_badge_status_red)

                btnToggle.text = "Kích hoạt"
                btnToggle.setTextColor(Color.parseColor("#16A34A"))
                btnToggle.setBackgroundResource(R.drawable.bg_badge_status_green)
            }

            // Bật / tắt voucher
            btnToggle.setOnClickListener {
                val newStatus = !voucher.isActive

                val success = dbHelper.updateVoucherStatus(
                    voucher.id,
                    newStatus
                )

                if (success) {
                    Toast.makeText(
                        this,
                        if (newStatus) "Đã bật voucher" else "Đã tắt voucher",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
                } else {
                    Toast.makeText(
                        this,
                        "Không thể cập nhật trạng thái voucher",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Chỉnh sửa voucher
            btnEdit.setOnClickListener {
                AdminVoucherDialogHelper.showEditVoucherDialog(
                    this,
                    voucher
                ) {
                    loadData()
                }
            }

            // Xóa voucher
            btnDelete.setOnClickListener {
                showDeleteConfirm(voucher)
            }

            llVoucherContainer.addView(itemView)
        }
    }

    private fun getDiscountText(voucher: Voucher): String {
        return when (voucher.type) {
            com.example.foodhub.data.model.VoucherType.PERCENT ->
                "Giảm ${voucher.value}% - Tối đa ${formatMoney(voucher.maxDiscount)}"

            com.example.foodhub.data.model.VoucherType.AMOUNT ->
                "Giảm ${formatMoney(voucher.value)}"

            com.example.foodhub.data.model.VoucherType.FREESHIP ->
                "Miễn phí vận chuyển"
        }
    }

    private fun showDeleteConfirm(voucher: Voucher) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa voucher")
            .setMessage(
                "Bạn có chắc chắn muốn xóa voucher \"${voucher.code}\" không? Thao tác này không thể hoàn tác."
            )
            .setNegativeButton("Hủy bỏ", null)
            .setPositiveButton("Xóa") { _, _ ->
                val success = dbHelper.deleteVoucher(voucher.id)

                if (success) {
                    Toast.makeText(
                        this,
                        "Đã xóa voucher thành công",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadData()
                } else {
                    Toast.makeText(
                        this,
                        "Xóa voucher thất bại",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun formatMoney(amount: Int): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(amount)}đ"
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Chưa thiết lập"

        val formatter = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale("vi", "VN")
        )

        return formatter.format(Date(timestamp))
    }
}
