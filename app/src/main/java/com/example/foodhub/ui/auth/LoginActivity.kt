package com.example.foodhub.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.ui.home.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var tvEmailError: TextView
    private lateinit var tvPasswordError: TextView

    // Nhận lại email sau khi người dùng đăng ký thành công ở SignUpActivity
    private val registerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val registeredEmail = result.data?.getStringExtra("registered_email")
            if (!registeredEmail.isNullOrEmpty()) {
                etEmail.setText(registeredEmail)
                etPassword.setText("")
                etPassword.requestFocus()
                hideError(etEmail, tvEmailError)
                hideError(etPassword, tvPasswordError)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail         = findViewById(R.id.etEmail)
        etPassword      = findViewById(R.id.etPassword)
        cbRemember      = findViewById(R.id.cbRemember)
        tvEmailError    = findViewById(R.id.tvEmailError)
        tvPasswordError = findViewById(R.id.tvPasswordError)

        // Khôi phục tài khoản nếu trước đó đã tích "Ghi nhớ tôi"
        loadRememberedAccount()

        // Tối ưu hóa chọn và sao chép toàn bộ email (chạm giữ hoặc chạm đúp sẽ bôi đen trọn vẹn email)
        setupEmailSelection(etEmail)

        findViewById<Button>(R.id.btnLogin).setOnClickListener { handleLogin() }

        findViewById<TextView>(R.id.tvGoRegister).setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            registerLauncher.launch(intent)
        }

        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            Toast.makeText(
                this,
                "Tài khoản mẫu: truong@email.com / MK: 123456\nHoặc bạn hãy bấm 'Đăng kí ngay' để tạo tài khoản mới.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadRememberedAccount() {
        val sharedPref = getSharedPreferences("foodhub_auth_prefs", Context.MODE_PRIVATE)
        val isRemembered = sharedPref.getBoolean("remember_me", false)
        val savedEmail = sharedPref.getString("saved_email", "")

        if (isRemembered && !savedEmail.isNullOrEmpty()) {
            cbRemember.isChecked = true
            etEmail.setText(savedEmail)
            etPassword.requestFocus()
        }
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

    private fun handleLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        var hasError = false

        // Kiểm tra định dạng email
        when {
            email.isEmpty() -> {
                showError(etEmail, tvEmailError, "Vui lòng nhập email hoặc số điện thoại")
                hasError = true
            }
            !email.contains("@") && email.length < 9 -> {
                showError(etEmail, tvEmailError, "Email hoặc số điện thoại không hợp lệ")
                hasError = true
            }
            else -> hideError(etEmail, tvEmailError)
        }

        // Kiểm tra mật khẩu
        if (password.isEmpty()) {
            showError(etPassword, tvPasswordError, "Vui lòng nhập mật khẩu")
            hasError = true
        } else {
            hideError(etPassword, tvPasswordError)
        }

        if (hasError) return

        val dbHelper = DatabaseHelper.getInstance(this)

        // ==============================================================
        // KIỂM TRA TÀI KHOẢN VÀ MẬT KHẨU TRONG CƠ SỞ DỮ LIỆU SQLITE
        // ==============================================================
        val user = dbHelper.checkUser(email, password)

        if (user != null) {
            // Đúng tài khoản và đúng mật khẩu trong SQLite -> Cho phép đăng nhập
            UserProfileManager.setUser(user)

            // Nạp dữ liệu giỏ hàng, đơn hàng và thông báo thực tế từ SQLite theo user_id của tài khoản này
            CartManager.init(this, user.id)
            OrderManager.init(this, user.id)
            NotificationStore.init(this, user.id)

            // Lưu trạng thái "Ghi nhớ tôi"
            val sharedPref = getSharedPreferences("foodhub_auth_prefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                if (cbRemember.isChecked) {
                    putBoolean("remember_me", true)
                    putString("saved_email", email)
                } else {
                    putBoolean("remember_me", false)
                    remove("saved_email")
                }
                apply()
            }

            Toast.makeText(this, "Đăng nhập thành công! Chào mừng ${user.name}", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // Sai mật khẩu hoặc tài khoản chưa đăng ký -> TUYỆT ĐỐI KHÔNG CHO VÀO
            if (!dbHelper.isEmailExists(email)) {
                showError(etEmail, tvEmailError, "Tài khoản chưa được đăng ký trong hệ thống!")
                etEmail.requestFocus()
            } else {
                showError(etPassword, tvPasswordError, "Mật khẩu không chính xác! Vui lòng thử lại.")
                etPassword.requestFocus()
            }
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
