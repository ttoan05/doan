package com.example.foodhub.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.foodhub.R
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.User
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.ui.order.OrderUi
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Helper quản lý toàn bộ các Dialog phục vụ Quản lý khách hàng:
 * 1. Xem chi tiết khách hàng
 * 2. Thêm khách hàng mới
 * 3. Chỉnh sửa thông tin khách hàng
 * 4. Khóa / Mở khóa tài khoản
 * 5. Xem lịch sử đơn hàng của khách hàng
 */
object AdminCustomerDialogHelper {

    fun showCustomerDetailDialog(
        activity: AdminCustomerActivity,
        user: User,
        onRefresh: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_customer_detail, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDetailClose)
        val tvLetter = dialogView.findViewById<TextView>(R.id.tvDetailAvatarLetter)
        val ivPhoto = dialogView.findViewById<ImageView>(R.id.ivDetailAvatarPhoto)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDetailName)
        val tvCode = dialogView.findViewById<TextView>(R.id.tvDetailCode)
        val tvRoleBadge = dialogView.findViewById<TextView>(R.id.tvDetailRoleBadge)
        val tvStatusBadge = dialogView.findViewById<TextView>(R.id.tvDetailStatusBadge)

        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDetailEmail)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDetailPhone)
        val tvAddress = dialogView.findViewById<TextView>(R.id.tvDetailAddress)
        val tvGender = dialogView.findViewById<TextView>(R.id.tvDetailGender)
        val tvBirthday = dialogView.findViewById<TextView>(R.id.tvDetailBirthday)
        val tvJob = dialogView.findViewById<TextView>(R.id.tvDetailJob)
        val tvCreatedAt = dialogView.findViewById<TextView>(R.id.tvDetailCreatedAt)

        val btnViewOrders = dialogView.findViewById<View>(R.id.btnDetailViewOrders)
        val tvOrderCount = dialogView.findViewById<TextView>(R.id.tvDetailOrderCount)

        val btnToggleLock = dialogView.findViewById<Button>(R.id.btnDetailToggleLock)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnDetailEdit)

        // Bind data
        tvName.text = user.name
        val formattedCode = String.format("#KH%03d", user.id)
        tvCode.text = "Mã KH: $formattedCode"

        val isAdmin = user.role.equals("admin", ignoreCase = true)
        tvRoleBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE

        // Avatar
        renderAvatar(user, tvLetter, ivPhoto)

        // Status badge
        val isLocked = user.status.equals("LOCKED", ignoreCase = true)
        if (isLocked) {
            tvStatusBadge.text = "● Đã khóa"
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_red)
            tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
            btnToggleLock.text = " Mở khóa tài khoản"
            btnToggleLock.setTextColor(Color.parseColor("#16A34A"))
            btnToggleLock.setBackgroundResource(R.drawable.bg_badge_status_green)
        } else {
            tvStatusBadge.text = "● Đang hoạt động"
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_green)
            tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
            btnToggleLock.text = " Khóa tài khoản"
            btnToggleLock.setTextColor(Color.parseColor("#DC2626"))
            btnToggleLock.setBackgroundResource(R.drawable.bg_btn_action_delete)
        }

        tvEmail.text = user.email.ifBlank { "Chưa cập nhật" }
        tvPhone.text = user.phone.ifBlank { "Chưa cập nhật" }
        tvAddress.text = user.address.ifBlank { "Chưa cập nhật địa chỉ giao hàng" }
        tvGender.text = user.gender.ifBlank { "Chưa cập nhật" }
        tvBirthday.text = user.birthday.ifBlank { "Chưa cập nhật" }
        tvJob.text = user.job.ifBlank { "Chưa cập nhật" }
        tvCreatedAt.text = user.createdAt.ifBlank { "20/10/2026" }

        // Fetch order count
        val orders = activity.dbHelper.getOrdersForUser(user.id)
        tvOrderCount.text = "Khách hàng đã đặt ${orders.size} đơn hàng trên FoodHub"

        btnClose.setOnClickListener { dialog.dismiss() }

        btnViewOrders.setOnClickListener {
            showCustomerOrdersDialog(activity, user)
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditCustomerDialog(activity, user, onRefresh)
        }

        btnToggleLock.setOnClickListener {
            dialog.dismiss()
            showToggleStatusConfirm(activity, user, onRefresh)
        }

        dialog.show()
    }

    fun showAddCustomerDialog(
        activity: AdminCustomerActivity,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_customer_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvCustomerFormTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCustomerFormClose)
        val etName = dialogView.findViewById<EditText>(R.id.etCustomerFormName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etCustomerFormEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etCustomerFormPhone)
        val etPassword = dialogView.findViewById<EditText>(R.id.etCustomerFormPassword)
        val etAddress = dialogView.findViewById<EditText>(R.id.etCustomerFormAddress)
        val spGender = dialogView.findViewById<Spinner>(R.id.spCustomerFormGender)
        val etBirthday = dialogView.findViewById<EditText>(R.id.etCustomerFormBirthday)
        val spRole = dialogView.findViewById<Spinner>(R.id.spCustomerFormRole)
        val spStatus = dialogView.findViewById<Spinner>(R.id.spCustomerFormStatus)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCustomerFormCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnCustomerFormSave)

        tvTitle.text = "Thêm khách hàng mới"

        // Setup Spinners
        val genders = arrayOf("Nam", "Nữ", "Khác")
        val genderAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, genders)
        spGender.adapter = genderAdapter

        val roles = arrayOf("Khách hàng (user)", "Quản trị viên (admin)")
        val roleAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, roles)
        spRole.adapter = roleAdapter

        val statuses = arrayOf("Đang hoạt động (ACTIVE)", "Đã khóa (LOCKED)")
        val statusAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, statuses)
        spStatus.adapter = statusAdapter

        // Date Picker
        etBirthday.setOnClickListener {
            showDatePicker(activity, etBirthday)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val gender = spGender.selectedItem?.toString() ?: "Nam"
            val birthday = etBirthday.text.toString().trim()
            val role = if (spRole.selectedItemPosition == 1) "admin" else "user"
            val status = if (spStatus.selectedItemPosition == 1) "LOCKED" else "ACTIVE"

            // Validation
            if (name.isEmpty()) {
                etName.error = "Vui lòng nhập họ và tên"
                etName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Email không hợp lệ"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (activity.dbHelper.isEmailExists(email)) {
                etEmail.error = "Email này đã được đăng ký trong hệ thống!"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            val digits = phone.filter { it.isDigit() }
            if (phone.isEmpty() || digits.length < 9 || digits.length > 11) {
                etPhone.error = "Số điện thoại phải từ 9 đến 11 số"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Mật khẩu tối thiểu 6 ký tự"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val newUser = User(
                name = name,
                email = email,
                phone = phone,
                password = password,
                gender = gender,
                birthday = birthday,
                job = "",
                role = role,
                avatarUri = "",
                address = address,
                createdAt = nowStr,
                status = status
            )

            val newId = activity.dbHelper.registerUser(newUser)
            if (newId > 0) {
                Toast.makeText(activity, "Đã thêm khách hàng ${newUser.name} thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSuccess()
            } else {
                Toast.makeText(activity, "Không thể thêm khách hàng vào hệ thống!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun showEditCustomerDialog(
        activity: AdminCustomerActivity,
        user: User,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_customer_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvCustomerFormTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCustomerFormClose)
        val etName = dialogView.findViewById<EditText>(R.id.etCustomerFormName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etCustomerFormEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etCustomerFormPhone)
        val tvPasswordLabel = dialogView.findViewById<TextView>(R.id.tvCustomerFormPasswordLabel)
        val etPassword = dialogView.findViewById<EditText>(R.id.etCustomerFormPassword)
        val etAddress = dialogView.findViewById<EditText>(R.id.etCustomerFormAddress)
        val spGender = dialogView.findViewById<Spinner>(R.id.spCustomerFormGender)
        val etBirthday = dialogView.findViewById<EditText>(R.id.etCustomerFormBirthday)
        val spRole = dialogView.findViewById<Spinner>(R.id.spCustomerFormRole)
        val spStatus = dialogView.findViewById<Spinner>(R.id.spCustomerFormStatus)

        val btnCancel = dialogView.findViewById<Button>(R.id.btnCustomerFormCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnCustomerFormSave)

        tvTitle.text = "Chỉnh sửa thông tin khách hàng"
        tvPasswordLabel.text = "Mật khẩu mới (bỏ trống nếu không đổi)"
        etPassword.hint = "Để trống nếu giữ nguyên mật khẩu cũ"

        // Setup Spinners
        val genders = arrayOf("Nam", "Nữ", "Khác")
        val genderAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, genders)
        spGender.adapter = genderAdapter
        val genderIndex = genders.indexOfFirst { it.equals(user.gender, ignoreCase = true) }
        if (genderIndex >= 0) spGender.setSelection(genderIndex)

        val roles = arrayOf("Khách hàng (user)", "Quản trị viên (admin)")
        val roleAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, roles)
        spRole.adapter = roleAdapter
        spRole.setSelection(if (user.role.equals("admin", ignoreCase = true)) 1 else 0)

        val statuses = arrayOf("Đang hoạt động (ACTIVE)", "Đã khóa (LOCKED)")
        val statusAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, statuses)
        spStatus.adapter = statusAdapter
        spStatus.setSelection(if (user.status.equals("LOCKED", ignoreCase = true)) 1 else 0)

        // Pre-fill fields
        etName.setText(user.name)
        etEmail.setText(user.email)
        etPhone.setText(user.phone)
        etAddress.setText(user.address)
        etBirthday.setText(user.birthday)

        etBirthday.setOnClickListener {
            showDatePicker(activity, etBirthday)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim().lowercase()
            val phone = etPhone.text.toString().trim()
            val newPass = etPassword.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val gender = spGender.selectedItem?.toString() ?: "Nam"
            val birthday = etBirthday.text.toString().trim()
            val role = if (spRole.selectedItemPosition == 1) "admin" else "user"
            val status = if (spStatus.selectedItemPosition == 1) "LOCKED" else "ACTIVE"

            if (name.isEmpty()) {
                etName.error = "Vui lòng nhập họ và tên"
                etName.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Email không hợp lệ"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // Check if email changed and already belongs to another user
            if (!email.equals(user.email, ignoreCase = true) && activity.dbHelper.isEmailExists(email)) {
                etEmail.error = "Email này đã thuộc về tài khoản khác!"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            val digits = phone.filter { it.isDigit() }
            if (phone.isEmpty() || digits.length < 9 || digits.length > 11) {
                etPhone.error = "Số điện thoại không hợp lệ"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            if (newPass.isNotEmpty() && newPass.length < 6) {
                etPassword.error = "Mật khẩu mới phải tối thiểu 6 ký tự"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Cannot lock currently logged in admin
            if (user.id == UserProfileManager.currentUserId && status == "LOCKED") {
                Toast.makeText(activity, "Không thể khóa tài khoản của chính bạn đang đăng nhập!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val finalPassword = if (newPass.isNotEmpty()) newPass else user.password

            val updated = user.copy(
                name = name,
                email = email,
                phone = phone,
                password = finalPassword,
                gender = gender,
                birthday = birthday,
                role = role,
                address = address,
                status = status
            )

            val success = activity.dbHelper.updateCustomerByAdmin(updated)
            if (success) {
                // If updated current logged-in user, refresh UserProfileManager
                if (user.id == UserProfileManager.currentUserId) {
                    UserProfileManager.setUser(updated)
                }
                Toast.makeText(activity, "Đã cập nhật thông tin khách hàng thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSuccess()
            } else {
                Toast.makeText(activity, "Lỗi cập nhật thông tin vào cơ sở dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun showToggleStatusConfirm(
        activity: AdminCustomerActivity,
        user: User,
        onRefresh: () -> Unit
    ) {
        if (user.id == UserProfileManager.currentUserId) {
            Toast.makeText(activity, "Không thể khóa tài khoản của chính bạn đang đăng nhập!", Toast.LENGTH_LONG).show()
            return
        }

        val isLocked = user.status.equals("LOCKED", ignoreCase = true)
        val title = if (isLocked) "Mở khóa tài khoản" else "Khóa tài khoản khách hàng"
        val message = if (isLocked) {
            "Bạn có chắc muốn MỞ KHÓA cho tài khoản ${user.name} (${user.email})?\nKhách hàng sẽ có thể đăng nhập và đặt hàng bình thường."
        } else {
            "Bạn có chắc muốn KHÓA tài khoản ${user.name} (${user.email})?\nKhách hàng này sẽ bị chặn đăng nhập vào hệ thống FoodHub."
        }
        val positiveBtnText = if (isLocked) "Mở khóa ngay" else "Khóa tài khoản"

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveBtnText) { d, _ ->
                d.dismiss()
                val newStatus = if (isLocked) "ACTIVE" else "LOCKED"
                val ok = activity.dbHelper.updateUserStatus(user.id, newStatus)
                if (ok) {
                    val msg = if (isLocked) "Đã mở khóa tài khoản ${user.name}" else "Đã khóa tài khoản ${user.name}"
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                    onRefresh()
                } else {
                    Toast.makeText(activity, "Lỗi khi cập nhật trạng thái!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun showDeleteCustomerConfirm(
        activity: AdminCustomerActivity,
        user: User,
        onRefresh: () -> Unit
    ) {
        if (user.id == UserProfileManager.currentUserId) {
            Toast.makeText(activity, "Không thể xóa tài khoản của chính bạn đang đăng nhập!", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(activity)
            .setTitle("Xóa tài khoản khách hàng")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản ${user.name} (${user.email})?\nThao tác này sẽ xóa vĩnh viễn dữ liệu tài khoản khỏi SQLite.")
            .setPositiveButton("Xóa vĩnh viễn") { d, _ ->
                d.dismiss()
                val ok = activity.dbHelper.deleteCustomer(user.id)
                if (ok) {
                    Toast.makeText(activity, "Đã xóa tài khoản ${user.name}", Toast.LENGTH_SHORT).show()
                    onRefresh()
                } else {
                    Toast.makeText(activity, "Không thể xóa tài khoản này!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun showCustomerOrdersDialog(
        activity: AdminCustomerActivity,
        user: User
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_customer_orders, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvOrderHistoryTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvOrderHistorySubtitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnOrderHistoryClose)
        val btnDismiss = dialogView.findViewById<Button>(R.id.btnOrderHistoryDismiss)
        val llContainer = dialogView.findViewById<LinearLayout>(R.id.llCustomerOrdersContainer)
        val llEmpty = dialogView.findViewById<LinearLayout>(R.id.llCustomerOrdersEmpty)

        tvTitle.text = "Lịch sử đơn hàng: ${user.name}"
        val orders = activity.dbHelper.getOrdersForUser(user.id)
        val formattedCode = String.format("#KH%03d", user.id)
        tvSubtitle.text = "Mã KH: $formattedCode • Tổng số: ${orders.size} đơn hàng"

        btnClose.setOnClickListener { dialog.dismiss() }
        btnDismiss.setOnClickListener { dialog.dismiss() }

        if (orders.isEmpty()) {
            llEmpty.visibility = View.VISIBLE
            llContainer.visibility = View.GONE
        } else {
            llEmpty.visibility = View.GONE
            llContainer.visibility = View.VISIBLE
            llContainer.removeAllViews()

            val inflater = activity.layoutInflater
            orders.forEach { order ->
                val itemView = inflater.inflate(R.layout.item_admin_order, llContainer, false)

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

                val cleanOrderCode = if (order.code.startsWith("#")) order.code else "#${order.code}"
                tvCode.text = "Mã đơn: $cleanOrderCode"
                OrderUi.styleStatus(tvStatus, order.status)
                tvMeta.text = "${FormatUtils.dateTime(order.createdAt)}  •  ${order.restaurant}"

                tvCustomer.text = "Người nhận: ${order.receiverName} • ${order.phone}"
                tvAddress.text = "Địa chỉ: ${order.address}"

                val itemsSummary = order.items.joinToString(", ") { "${it.name} (x${it.quantity})" }
                tvItems.text = "Món (${order.totalQuantity}): $itemsSummary"

                if (order.orderNote.isNotBlank()) {
                    llNote.visibility = View.VISIBLE
                    tvNote.text = "Ghi chú: ${order.orderNote}"
                } else {
                    llNote.visibility = View.GONE
                }

                tvPayment.text = order.paymentMethod
                tvTotal.text = FormatUtils.money(order.total)

                btnDetail.setOnClickListener {
                    val intent = Intent(activity, OrderDetailActivity::class.java).apply {
                        putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id)
                    }
                    activity.startActivity(intent)
                }

                btnUpdate.visibility = View.GONE // Quick read-only inside customer dialog

                llContainer.addView(itemView)
            }
        }

        dialog.show()
    }

    private fun renderAvatar(user: User, tvLetter: TextView, ivPhoto: ImageView) {
        if (user.avatarUri.isNotEmpty()) {
            val file = File(user.avatarUri)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val circular = ImageUtils.getCircularBitmap(bitmap)
                    ivPhoto.setImageBitmap(circular)
                    ivPhoto.visibility = View.VISIBLE
                    tvLetter.visibility = View.GONE
                    return
                }
            }
        }
        val letter = user.name.trim().firstOrNull()?.uppercase() ?: "U"
        tvLetter.text = letter
        tvLetter.visibility = View.VISIBLE
        ivPhoto.visibility = View.GONE
    }

    private fun showDatePicker(activity: AdminCustomerActivity, etTarget: EditText) {
        val calendar = Calendar.getInstance()
        val current = etTarget.text.toString().trim()
        val parts = current.split("/")
        if (parts.size == 3) {
            val d = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val y = parts[2].toIntOrNull()
            if (d != null && m != null && y != null) {
                calendar.set(y, m - 1, d)
            }
        }

        DatePickerDialog(
            activity,
            { _, year, month, dayOfMonth ->
                val text = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                etTarget.setText(text)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
