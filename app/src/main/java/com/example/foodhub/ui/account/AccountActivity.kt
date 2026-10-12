package com.example.foodhub.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.ui.home.MainActivity
import com.example.foodhub.ui.auth.LoginActivity

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phòng khi app bị hệ thống khởi động lại thẳng vào trang này
        FavoriteManager.init(this)

        setContentView(R.layout.activity_account)

        // Hiển thị tên/avatar hiện tại (có thể đã được đổi ở trang Thông tin người dùng)
        refreshUserHeader()

        // ==========================================
        // THÔNG TIN CÁ NHÂN
        // ==========================================

        // Bấm avatar
        findViewById<FrameLayout>(R.id.flAvatar).setOnClickListener {
            openUserInfo()
        }

        // Bấm tên người dùng
        findViewById<TextView>(R.id.tvUserName).setOnClickListener {
            openUserInfo()
        }


        // ==========================================
        // MENU TRANG TÔI
        // ==========================================

        // Ví Voucher
        findViewById<View>(R.id.btnVoucher).setOnClickListener {

            Toast.makeText(
                this,
                "Ví Voucher - đang phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Thanh toán
        findViewById<View>(R.id.btnPayment).setOnClickListener {

            Toast.makeText(
                this,
                "Thanh toán - đang phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Địa chỉ
        findViewById<View>(R.id.btnAddress).setOnClickListener {

            Toast.makeText(
                this,
                "Địa chỉ - đang phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Trung tâm trợ giúp
        findViewById<View>(R.id.btnHelp).setOnClickListener {

            Toast.makeText(
                this,
                "Trung tâm trợ giúp - đang phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Cài đặt
        findViewById<View>(R.id.btnSettings).setOnClickListener {

            Toast.makeText(
                this,
                "Cài đặt - đang phát triển",
                Toast.LENGTH_SHORT
            ).show()
        }


        // ==========================================
        // ĐĂNG XUẤT
        // ==========================================

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // Xóa phiên đăng nhập người dùng và dọn bộ nhớ tạm giỏ hàng/đơn hàng/thông báo
            UserProfileManager.clear()
            CartManager.clearMemory()
            OrderManager.clearMemory()
            NotificationStore.clearMemory()

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }


        // ==========================================
        // THANH ĐIỀU HƯỚNG DƯỚI
        // ==========================================

        // ------------------------------------------
        // HOME
        // ------------------------------------------

        findViewById<View>(R.id.navHome).setOnClickListener {

            openMainTab("home")
        }


        // ------------------------------------------
        // ĐƠN HÀNG
        // ------------------------------------------

        findViewById<View>(R.id.navOrders).setOnClickListener {

            openMainTab("orders")
        }


        // ------------------------------------------
        // YÊU THÍCH
        // ------------------------------------------

        findViewById<View>(R.id.navFavorite).setOnClickListener {

            openMainTab("likes")
        }


        // ------------------------------------------
        // THÔNG BÁO
        // ------------------------------------------

        findViewById<View>(R.id.navNotification).setOnClickListener {

            openMainTab("notifications")
        }


        // ------------------------------------------
        // TÔI
        // ------------------------------------------

        // Đang ở AccountActivity nên không cần chuyển
        findViewById<View>(R.id.navAccount).setOnClickListener {

            // Không làm gì
        }
    }


    override fun onResume() {
        super.onResume()
        // Quay lại từ trang "Thông tin người dùng" sau khi đổi tên -> cập nhật lại header
        refreshUserHeader()
        // Đơn hàng vẫn tự chuyển trạng thái khi đang ở trang Tôi -> badge chuông cập nhật theo
        refreshNotifBadge()
        refreshFavBadge()
        NotificationStore.onChanged = { refreshNotifBadge() }
        FavoriteManager.onChanged = { refreshFavBadge() }
    }

    override fun onPause() {
        super.onPause()
        NotificationStore.onChanged = null
        FavoriteManager.onChanged = null
    }

    private fun refreshFavBadge() {
        val badge = findViewById<TextView>(R.id.tvFavBadge)
        val count = FavoriteManager.totalCount()
        badge.text = if (count > 9) "9+" else count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun refreshNotifBadge() {
        val badge = findViewById<TextView>(R.id.tvNotifBadge)
        val count = NotificationStore.unreadCount()
        badge.text = if (count > 9) "9+" else count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    // ==========================================
    // ĐỒNG BỘ TÊN / AVATAR VỚI THÔNG TIN ĐÃ LƯU
    // ==========================================

    private fun refreshUserHeader() {
        findViewById<TextView>(R.id.tvUserName).text = UserProfileManager.name
        findViewById<TextView>(R.id.tvAvatarLetter).text = UserProfileManager.avatarLetter
    }

    // ==========================================
    // MỞ TRANG THÔNG TIN CÁ NHÂN
    // ==========================================

    private fun openUserInfo() {

        val intent = Intent(
            this,
            UserInfoActivity::class.java
        )

        startActivity(intent)
    }


    // ==========================================
    // CHUYỂN TỪ ACCOUNT SANG MAINACTIVITY
    // ==========================================

    private fun openMainTab(tab: String) {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.putExtra("selected_tab", tab)

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        // Tắt hiệu ứng chuyển Activity
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}
