package com.example.foodhub.ui.account

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.User
import java.util.Calendar

class UserInfoActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etGender: EditText
    private lateinit var etBirthday: EditText
    private lateinit var etJob: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etGender = findViewById(R.id.etGender)
        etBirthday = findViewById(R.id.etBirthday)
        etJob = findViewById(R.id.etJob)

        // Tên đăng nhập không cho sửa
        findViewById<EditText>(R.id.etUsername).setText(UserProfileManager.username)

        // Nạp dữ liệu hiện tại từ UserProfileManager vào các ô nhập
        loadCurrentProfile()

        // Bấm vào ô "Ngày sinh" mở lịch chọn ngày cho tiện, thay vì gõ tay
        etBirthday.setOnClickListener { showDatePicker() }

        // Save button
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener { saveProfile(btnSave) }
    }

    override fun onResume() {
        super.onResume()
        // Phòng khi thông tin bị thay đổi ở nơi khác trong lúc màn hình này đang mở
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        findViewById<TextView>(R.id.tvAvatarLetterInfo).text = UserProfileManager.avatarLetter
        findViewById<TextView>(R.id.tvAvatarNameInfo).text = UserProfileManager.name

        etName.setText(UserProfileManager.name)
        etPhone.setText(UserProfileManager.phone)
        etEmail.setText(UserProfileManager.email)
        etGender.setText(UserProfileManager.gender)
        etBirthday.setText(UserProfileManager.birthday)
        etJob.setText(UserProfileManager.job)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Nếu đã có ngày sinh dạng dd/MM/yyyy thì mở lịch đúng ngày đó
        val current = etBirthday.text.toString().trim()
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
            this,
            { _, year, month, dayOfMonth ->
                val text = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                etBirthday.setText(text)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveProfile(btnSave: Button) {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()

        if (name.isEmpty()) {
            etName.error = "Vui lòng nhập tên"
            etName.requestFocus()
            return
        }
        val digits = phone.filter { it.isDigit() }
        if (phone.isNotEmpty() && (digits.length < 9 || digits.length > 11)) {
            etPhone.error = "Số điện thoại không hợp lệ"
            etPhone.requestFocus()
            return
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email không hợp lệ"
            etEmail.requestFocus()
            return
        }

        UserProfileManager.update(
            name = name,
            phone = phone,
            email = email,
            gender = etGender.text.toString().trim(),
            birthday = etBirthday.text.toString().trim(),
            job = etJob.text.toString().trim()
        )

        // Cập nhật thông tin đồng bộ vào cơ sở dữ liệu SQLite
        val currentUserId = UserProfileManager.currentUserId
        if (currentUserId > 0) {
            val updatedUser = User(
                id = currentUserId,
                name = name,
                email = email,
                phone = phone,
                password = "",
                gender = etGender.text.toString().trim(),
                birthday = etBirthday.text.toString().trim(),
                job = etJob.text.toString().trim()
            )
            DatabaseHelper.getInstance(this).updateUserProfile(updatedUser)
        }

        // Cập nhật lại avatar/tên hiển thị ngay trên chính màn hình này
        findViewById<TextView>(R.id.tvAvatarLetterInfo).text = UserProfileManager.avatarLetter
        findViewById<TextView>(R.id.tvAvatarNameInfo).text = UserProfileManager.name

        Toast.makeText(this, "Đã lưu thông tin vào cơ sở dữ liệu", Toast.LENGTH_SHORT).show()

        btnSave.text = "✓ Đã lưu thành công!"
        btnSave.setBackgroundResource(R.drawable.bg_btn_green)
        btnSave.postDelayed({
            btnSave.text = "Lưu thông tin"
            btnSave.setBackgroundResource(R.drawable.bg_btn_red)
        }, 2000)
    }
}
