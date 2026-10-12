package com.example.foodhub.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.User

class SignUpActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbAgree: CheckBox
    private lateinit var tvNameError: TextView
    private lateinit var tvEmailError: TextView
    private lateinit var tvPhoneError: TextView
    private lateinit var tvPasswordError: TextView
    private lateinit var tvAgreeError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etName        = findViewById(R.id.etName)
        etEmail       = findViewById(R.id.etEmail)
        etPhone       = findViewById(R.id.etPhone)
        etPassword    = findViewById(R.id.etPassword)
        cbAgree       = findViewById(R.id.cbAgree)
        tvNameError   = findViewById(R.id.tvNameError)
        tvEmailError  = findViewById(R.id.tvEmailError)
        tvPhoneError  = findViewById(R.id.tvPhoneError)
        tvPasswordError = findViewById(R.id.tvPasswordError)
        tvAgreeError  = findViewById(R.id.tvAgreeError)

        // Tối ưu hóa chọn và sao chép toàn bộ email (chạm giữ hoặc chạm đúp sẽ bôi đen toàn bộ email)
        setupEmailSelection(etEmail)

        findViewById<Button>(R.id.btnRegister).setOnClickListener { handleRegister() }
        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { finish() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEmailSelection(et: EditText) {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (et.text.isNotEmpty()) {
                    et.selectAll()
                    return true
                }
                return false
            }
        })
        et.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        et.setOnLongClickListener {
            if (et.text.isNotEmpty()) {
                et.selectAll()
            }
            false
        }
    }

    private fun handleRegister() {
        val name     = etName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val phone    = etPhone.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val agreed   = cbAgree.isChecked
        var hasError = false

        if (name.isEmpty()) {
            showError(etName, tvNameError, "Vui lòng nhập họ và tên")
            hasError = true
        } else {
            hideError(etName, tvNameError)
        }

        when {
            email.isEmpty() -> {
                showError(etEmail, tvEmailError, "Vui lòng nhập email")
                hasError = true
            }
            !email.contains("@") -> {
                showError(etEmail, tvEmailError, "Email bắt buộc phải có chữ @")
                hasError = true
            }
            !email.contains(".com") -> {
                showError(etEmail, tvEmailError, "Email bắt buộc phải có chữ .com")
                hasError = true
            }
            email.indexOf("@") == 0 || email.indexOf("@") >= email.lastIndexOf(".com") -> {
                showError(etEmail, tvEmailError, "Định dạng email không hợp lệ (ví dụ: example@gmail.com)")
                hasError = true
            }
            else -> hideError(etEmail, tvEmailError)
        }

        if (phone.isEmpty()) {
            showError(etPhone, tvPhoneError, "Vui lòng nhập số điện thoại")
            hasError = true
        } else {
            hideError(etPhone, tvPhoneError)
        }

        if (password.length < 6) {
            showError(etPassword, tvPasswordError, "Mật khẩu tối thiểu 6 ký tự")
            hasError = true
        } else {
            hideError(etPassword, tvPasswordError)
        }

        if (!agreed) {
            tvAgreeError.text = "Vui lòng đồng ý điều khoản dịch vụ"
            tvAgreeError.visibility = View.VISIBLE
            hasError = true
        } else {
            tvAgreeError.visibility = View.GONE
        }

        if (hasError) return

        val dbHelper = DatabaseHelper.getInstance(this)

        // 1. Kiểm tra tài khoản đã tồn tại trong SQLite chưa
        if (dbHelper.isEmailExists(email)) {
            showError(etEmail, tvEmailError, "Email này đã được đăng ký, vui lòng đăng nhập!")
            etEmail.requestFocus()
            return
        }

        // 2. Lưu người dùng mới vào CSDL SQLite
        val newUser = User(
            name = name,
            email = email,
            phone = phone,
            password = password
        )

        val newId = dbHelper.registerUser(newUser)

        if (newId > 0) {
            Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập với tài khoản vừa tạo.", Toast.LENGTH_LONG).show()

            // Gửi email về LoginActivity để điền sẵn cho người dùng
            val resultIntent = Intent().apply {
                putExtra("registered_email", email)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Đăng ký thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(field: EditText, errorView: TextView, msg: String) {
        field.setBackgroundResource(R.drawable.bg_field_error)
        errorView.text = msg
        errorView.visibility = View.VISIBLE
    }

    private fun hideError(field: EditText, errorView: TextView) {
        field.setBackgroundResource(R.drawable.bg_field)
        errorView.visibility = View.GONE
    }
}
