package com.example.foodhub.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager

class SettingsActivity : AppCompatActivity() {

    private val prefs by lazy {
        getSharedPreferences("foodhub_settings_prefs", Context.MODE_PRIVATE)
    }

    private lateinit var swNotifOrder: SwitchCompat
    private lateinit var swNotifPromo: SwitchCompat
    private lateinit var swNotifSystem: SwitchCompat
    private lateinit var swDarkMode: SwitchCompat
    private lateinit var swLocation: SwitchCompat
    private lateinit var tvCurrentLanguage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        bindViews()
        setupSwitchTints()
        loadSavedSettings()
        setupListeners()
    }

    private fun bindViews() {
        swNotifOrder = findViewById(R.id.swNotifOrder)
        swNotifPromo = findViewById(R.id.swNotifPromo)
        swNotifSystem = findViewById(R.id.swNotifSystem)
        swDarkMode = findViewById(R.id.swDarkMode)
        swLocation = findViewById(R.id.swLocation)
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage)
    }

    /**
     * Tùy chỉnh màu sắc nút Switch đẹp mắt đúng như ảnh thiết kế:
     * - Bật (Checked): Xanh ngọc Teal (#14B8A6)
     * - Tắt (Unchecked): Xám (#D1D5DB / #E5E7EB)
     */
    private fun setupSwitchTints() {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val thumbColors = intArrayOf(
            Color.parseColor("#14B8A6"), // Teal
            Color.parseColor("#D1D5DB")  // Grey
        )
        val trackColors = intArrayOf(
            Color.parseColor("#99F6E4"), // Light Teal
            Color.parseColor("#E5E7EB")  // Light Grey
        )

        val thumbList = ColorStateList(states, thumbColors)
        val trackList = ColorStateList(states, trackColors)

        listOf(swNotifOrder, swNotifPromo, swNotifSystem, swDarkMode, swLocation).forEach { sw ->
            sw.thumbTintList = thumbList
            sw.trackTintList = trackList
        }
    }

    private fun loadSavedSettings() {
        swNotifOrder.isChecked = prefs.getBoolean("notif_order", true)
        swNotifPromo.isChecked = prefs.getBoolean("notif_promo", true)
        swNotifSystem.isChecked = prefs.getBoolean("notif_system", true)
        swDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
        swLocation.isChecked = prefs.getBoolean("location_enabled", false)

        val lang = prefs.getString("language", "Tiếng Việt") ?: "Tiếng Việt"
        tvCurrentLanguage.text = lang
    }

    private fun setupListeners() {
        // Switch listeners
        swNotifOrder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_order", isChecked).apply()
            val msg = if (isChecked) "Đã bật thông báo đơn hàng" else "Đã tắt thông báo đơn hàng"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        swNotifPromo.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_promo", isChecked).apply()
            val msg = if (isChecked) "Đã bật thông báo khuyến mãi" else "Đã tắt thông báo khuyến mãi"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        swNotifSystem.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_system", isChecked).apply()
            val msg = if (isChecked) "Đã bật thông báo hệ thống" else "Đã tắt thông báo hệ thống"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            val msg = if (isChecked) "Chế độ tối đang được thử nghiệm" else "Đã chuyển sang giao diện sáng"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        swLocation.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("location_enabled", isChecked).apply()
            val msg = if (isChecked) "Đã bật quyền vị trí để gợi ý quán gần bạn" else "Đã tắt quyền vị trí"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // Section Tài khoản & Bảo mật
        findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        findViewById<View>(R.id.btnTwoFactor).setOnClickListener {
            showTwoFactorDialog()
        }

        findViewById<View>(R.id.btnLanguage).setOnClickListener {
            showLanguageDialog()
        }

        // Section Khác
        findViewById<View>(R.id.btnRateApp).setOnClickListener {
            showRateAppDialog()
        }

        findViewById<View>(R.id.btnTerms).setOnClickListener {
            showTermsDialog()
        }

        findViewById<View>(R.id.btnAppVersion).setOnClickListener {
            Toast.makeText(this, "FoodHub phiên bản v1.0.0 (Mới nhất)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        val etOldPass = view.findViewById<EditText>(R.id.etOldPassword)
        val etNewPass = view.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPass = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val oldP = etOldPass.text.toString().trim()
            val newP = etNewPass.text.toString().trim()
            val confP = etConfirmPass.text.toString().trim()

            if (oldP.isEmpty() || newP.isEmpty() || confP.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ các trường", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newP.length < 6) {
                etNewPass.error = "Mật khẩu mới phải từ 6 ký tự"
                etNewPass.requestFocus()
                return@setOnClickListener
            }
            if (newP != confP) {
                etConfirmPass.error = "Mật khẩu xác nhận không khớp"
                etConfirmPass.requestFocus()
                return@setOnClickListener
            }

            val uid = UserProfileManager.currentUserId
            val db = DatabaseHelper.getInstance(this)
            val updated = db.updateUserPassword(uid, newP)
            if (updated) {
                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Có lỗi xảy ra, vui lòng thử lại sau.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showTwoFactorDialog() {
        val isEnabled = prefs.getBoolean("two_factor_enabled", false)
        val actionText = if (isEnabled) "Tắt xác thực" else "Kích hoạt ngay"

        AlertDialog.Builder(this)
            .setTitle("🛡️ Xác thực 2 lớp (2FA)")
            .setMessage(
                "Xác thực 2 lớp giúp bảo vệ tài khoản bằng mã OTP gửi về số điện thoại khi đăng nhập từ thiết bị lạ.\n\n" +
                "Trạng thái hiện tại: " + (if (isEnabled) "Đang BẬT ✅" else "Đang TẮT ❌")
            )
            .setPositiveButton(actionText) { _, _ ->
                val newState = !isEnabled
                prefs.edit().putBoolean("two_factor_enabled", newState).apply()
                val msg = if (newState) "Đã kích hoạt xác thực 2 lớp" else "Đã tắt xác thực 2 lớp"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Tiếng Việt (Mặc định)", "English (US)")
        val current = if (tvCurrentLanguage.text.toString().contains("English")) 1 else 0

        AlertDialog.Builder(this)
            .setTitle("🌐 Chọn ngôn ngữ")
            .setSingleChoiceItems(languages, current) { dialog, which ->
                val selectedLang = if (which == 0) "Tiếng Việt" else "English"
                tvCurrentLanguage.text = selectedLang
                prefs.edit().putString("language", selectedLang).apply()
                Toast.makeText(this, "Đã đổi ngôn ngữ sang: $selectedLang", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showRateAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("⭐ Đánh giá FoodHub")
            .setMessage("Bạn cảm thấy trải nghiệm đặt món cùng FoodHub như thế nào? Hãy dành chút thời gian đánh giá 5 sao nhé!")
            .setPositiveButton("Đánh giá 5 sao ⭐") { _, _ ->
                Toast.makeText(this, "Cảm ơn bạn rất nhiều vì đã ủng hộ 5 sao! ❤️", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Để sau", null)
            .show()
    }

    private fun showTermsDialog() {
        AlertDialog.Builder(this)
            .setTitle("📋 Điều khoản & Chính sách")
            .setMessage(
                "1. Cam kết an toàn thực phẩm và quyền lợi người dùng.\n\n" +
                "2. Bảo mật thông tin cá nhân và phương thức thanh toán tuyệt đối.\n\n" +
                "3. Thời gian giao hàng cam kết trong vòng 30 - 45 phút đối với các quán đối tác liên kết.\n\n" +
                "4. Mọi khiếu nại xin vui lòng liên hệ hotline: 1900 1234."
            )
            .setPositiveButton("Đã hiểu", null)
            .show()
    }
}
