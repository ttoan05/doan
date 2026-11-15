package com.example.foodhub.ui.admin

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Voucher
import com.example.foodhub.data.model.VoucherType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AdminVoucherDialogHelper {

    private val dateFormatter = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale("vi", "VN")
    )

    // =========================================================
    // 1. HIỂN THỊ FORM THÊM VOUCHER
    // =========================================================

    fun showAddVoucherDialog(
        context: Context,
        onSaved: () -> Unit
    ) {
        showVoucherForm(
            context = context,
            voucher = null,
            onSaved = onSaved
        )
    }

    // =========================================================
    // 2. HIỂN THỊ FORM CHỈNH SỬA VOUCHER
    // =========================================================

    fun showEditVoucherDialog(
        context: Context,
        voucher: Voucher,
        onSaved: () -> Unit
    ) {
        showVoucherForm(
            context = context,
            voucher = voucher,
            onSaved = onSaved
        )
    }

    // =========================================================
    // 3. FORM CHUNG THÊM / CHỈNH SỬA
    // =========================================================

    private fun showVoucherForm(
        context: Context,
        voucher: Voucher?,
        onSaved: () -> Unit
    ) {
        val dbHelper = DatabaseHelper.getInstance(context)

        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_admin_voucher_form,
            null
        )

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvDialogSubtitle)
        val btnClose = view.findViewById<View>(R.id.btnDialogClose)

        if (voucher == null) {
            tvTitle.text = "Thêm voucher mới"
            tvSubtitle.text = "Thiết lập chương trình ưu đãi cho khách hàng"
        } else {
            tvTitle.text = "Chỉnh sửa voucher"
            tvSubtitle.text = "Cập nhật mã ưu đãi: ${voucher.code}"
        }

        // Ánh xạ các thành phần giao diện
        val etCode = view.findViewById<EditText>(R.id.etVoucherCode)
        val etTitle = view.findViewById<EditText>(R.id.etVoucherTitle)
        val etDescription = view.findViewById<EditText>(R.id.etVoucherDescription)
        val spType = view.findViewById<Spinner>(R.id.spVoucherType)
        val etValue = view.findViewById<EditText>(R.id.etVoucherValue)
        val etMinOrder = view.findViewById<EditText>(R.id.etVoucherMinOrder)
        val etMaxDiscount = view.findViewById<EditText>(R.id.etVoucherMaxDiscount)
        val etQuantity = view.findViewById<EditText>(R.id.etVoucherQuantity)
        val btnStartDate = view.findViewById<TextView>(R.id.btnVoucherStartDate)
        val btnEndDate = view.findViewById<TextView>(R.id.btnVoucherEndDate)
        val cbActive = view.findViewById<CheckBox>(R.id.cbVoucherActive)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelVoucher)
        val btnSave = view.findViewById<Button>(R.id.btnSaveVoucher)

        // =====================================================
        // 4. CẤU HÌNH LOẠI VOUCHER
        // =====================================================

        val voucherTypes = listOf(
            "Giảm theo phần trăm (%)",
            "Giảm số tiền cố định (VNĐ)",
            "Miễn phí vận chuyển"
        )

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            voucherTypes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spType.adapter = adapter

        // =====================================================
        // 5. KHỞI TẠO NGÀY BẮT ĐẦU / KẾT THÚC
        // =====================================================

        val startCalendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 30)
        }

        if (voucher != null) {
            if (voucher.startAt > 0L) {
                startCalendar.timeInMillis = voucher.startAt
            }

            if (voucher.endAt > 0L) {
                endCalendar.timeInMillis = voucher.endAt
            }
        }

        fun updateDateButtons() {
            btnStartDate.text = dateFormatter.format(startCalendar.time)
            btnEndDate.text = dateFormatter.format(endCalendar.time)
        }

        updateDateButtons()

        // Chọn ngày bắt đầu
        btnStartDate.setOnClickListener {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    startCalendar.set(year, month, day, 0, 0, 0)
                    startCalendar.set(Calendar.MILLISECOND, 0)
                    updateDateButtons()
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Chọn ngày kết thúc
        btnEndDate.setOnClickListener {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    endCalendar.set(year, month, day, 23, 59, 59)
                    endCalendar.set(Calendar.MILLISECOND, 999)
                    updateDateButtons()
                },
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // =====================================================
        // 6. ĐIỀN DỮ LIỆU KHI CHỈNH SỬA
        // =====================================================

        if (voucher != null) {
            etCode.setText(voucher.code)
            etTitle.setText(voucher.title)
            etDescription.setText(voucher.desc)

            etValue.setText(voucher.value.toString())
            etMinOrder.setText(voucher.minOrder.toString())
            etMaxDiscount.setText(voucher.maxDiscount.toString())
            etQuantity.setText(voucher.quantity.toString())

            spType.setSelection(
                when (voucher.type) {
                    VoucherType.PERCENT -> 0
                    VoucherType.AMOUNT -> 1
                    VoucherType.FREESHIP -> 2
                }
            )

            cbActive.isChecked = voucher.isActive
        } else {
            cbActive.isChecked = true
            spType.setSelection(0)
            etValue.setText("30")
            etMinOrder.setText("0")
            etMaxDiscount.setText("0")
            etQuantity.setText("100")
        }

        // =====================================================
        // 7. TẠO DIALOG HIỆN ĐẠI (BO GÓC TRONG SUỐT)
        // =====================================================

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // =====================================================
        // 8. XỬ LÝ LƯU VOUCHER
        // =====================================================

        btnSave.setOnClickListener {
            val code = etCode.text.toString()
                .trim()
                .uppercase(Locale.ROOT)

            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()

            val value = etValue.text.toString()
                .trim()
                .toIntOrNull()

            val minOrder = etMinOrder.text.toString()
                .trim()
                .toIntOrNull()

            val maxDiscount = etMaxDiscount.text.toString()
                .trim()
                .toIntOrNull()

            val quantity = etQuantity.text.toString()
                .trim()
                .toIntOrNull()

            val type = when (spType.selectedItemPosition) {
                0 -> VoucherType.PERCENT
                1 -> VoucherType.AMOUNT
                2 -> VoucherType.FREESHIP
                else -> VoucherType.PERCENT
            }

            // -------------------------------------------------
            // KIỂM TRA DỮ LIỆU
            // -------------------------------------------------

            if (code.isEmpty()) {
                etCode.error = "Vui lòng nhập mã voucher"
                etCode.requestFocus()
                return@setOnClickListener
            }

            if (!code.matches(Regex("[A-Z0-9_-]+"))) {
                etCode.error = "Mã chỉ được chứa chữ in hoa, số, dấu _ hoặc -"
                etCode.requestFocus()
                return@setOnClickListener
            }

            if (title.isEmpty()) {
                etTitle.error = "Vui lòng nhập tên chương trình"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                etDescription.error = "Vui lòng nhập mô tả"
                etDescription.requestFocus()
                return@setOnClickListener
            }

            if (value == null || value < 0) {
                etValue.error = "Giá trị giảm không hợp lệ"
                etValue.requestFocus()
                return@setOnClickListener
            }

            if (minOrder == null || minOrder < 0) {
                etMinOrder.error = "Đơn hàng tối thiểu không hợp lệ"
                etMinOrder.requestFocus()
                return@setOnClickListener
            }

            if (maxDiscount == null || maxDiscount < 0) {
                etMaxDiscount.error = "Mức giảm tối đa không hợp lệ"
                etMaxDiscount.requestFocus()
                return@setOnClickListener
            }

            if (quantity == null || quantity <= 0) {
                etQuantity.error = "Số lượng phải lớn hơn 0"
                etQuantity.requestFocus()
                return@setOnClickListener
            }

            if (type == VoucherType.PERCENT && value > 100) {
                etValue.error = "Phần trăm giảm không được vượt quá 100"
                etValue.requestFocus()
                return@setOnClickListener
            }

            if (type == VoucherType.FREESHIP && value != 0) {
                Toast.makeText(
                    context,
                    "Voucher miễn phí vận chuyển không cần nhập giá trị giảm",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (startCalendar.timeInMillis > endCalendar.timeInMillis) {
                Toast.makeText(
                    context,
                    "Ngày bắt đầu không được sau ngày kết thúc",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // -------------------------------------------------
            // KIỂM TRA MÃ VOUCHER TRÙNG
            // -------------------------------------------------

            val existingVoucher = dbHelper.getVoucherByCode(code)

            if (existingVoucher != null &&
                existingVoucher.id != (voucher?.id ?: 0)
            ) {
                etCode.error = "Mã voucher này đã tồn tại"
                etCode.requestFocus()
                return@setOnClickListener
            }

            // -------------------------------------------------
            // XỬ LÝ SỐ LƯỢNG CÒN LẠI
            // -------------------------------------------------

            val remainingQuantity: Int

            if (voucher == null) {
                remainingQuantity = quantity
            } else {
                val usedQuantity = voucher.quantity - voucher.remainingQuantity

                if (quantity < usedQuantity) {
                    etQuantity.error =
                        "Tổng số lượng không được nhỏ hơn số đã sử dụng ($usedQuantity)"
                    etQuantity.requestFocus()
                    return@setOnClickListener
                }

                remainingQuantity = quantity - usedQuantity
            }

            // -------------------------------------------------
            // TẠO ĐỐI TƯỢNG VOUCHER
            // -------------------------------------------------

            val newVoucher = Voucher(
                id = voucher?.id ?: 0,
                code = code,
                title = title,
                desc = description,
                type = type,
                value = if (type == VoucherType.FREESHIP) 0 else value,
                maxDiscount = if (type == VoucherType.PERCENT) {
                    maxDiscount
                } else {
                    0
                },
                minOrder = minOrder,
                emoji = voucher?.emoji ?: "🎟️",
                quantity = quantity,
                remainingQuantity = remainingQuantity,
                startAt = startCalendar.timeInMillis,
                endAt = endCalendar.timeInMillis,
                isActive = cbActive.isChecked
            )

            // -------------------------------------------------
            // LƯU VÀO SQLITE
            // -------------------------------------------------

            if (voucher == null) {
                val result = dbHelper.insertVoucher(newVoucher)

                if (result > 0) {
                    Toast.makeText(
                        context,
                        "Thêm voucher thành công!",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                    onSaved()
                } else {
                    Toast.makeText(
                        context,
                        "Thêm voucher thất bại. Hãy kiểm tra lại mã voucher!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val result = dbHelper.updateVoucher(newVoucher)

                if (result) {
                    Toast.makeText(
                        context,
                        "Cập nhật voucher thành công!",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                    onSaved()
                } else {
                    Toast.makeText(
                        context,
                        "Cập nhật voucher thất bại!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }
}
