package com.example.foodhub.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.foodhub.R
import com.example.foodhub.data.AddressStore
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.PaymentStore
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.VoucherStore
import com.example.foodhub.data.model.SavedAddress
import com.example.foodhub.ui.address.AddressActivity
import com.example.foodhub.ui.admin.AdminCategoryActivity
import com.example.foodhub.ui.admin.AdminFoodActivity
import com.example.foodhub.ui.admin.AdminOrderActivity
import com.example.foodhub.ui.auth.LoginActivity
import com.example.foodhub.ui.home.MainActivity
import com.example.foodhub.ui.settings.SettingsActivity
import com.example.foodhub.ui.voucher.VoucherActivity

class AccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phòng khi app bị hệ thống khởi động lại thẳng vào trang này
        FavoriteManager.init(this)

        setContentView(R.layout.activity_account)

        // Hiển thị tên/avatar/quyền admin hiện tại
        refreshUserHeader()
        refreshVoucherCount()

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
        // KHU VỰC QUẢN TRỊ ADMIN (Chỉ click được khi là Admin)
        // ==========================================
        findViewById<View>(R.id.btnAdminFoods).setOnClickListener {
            startActivity(Intent(this, AdminFoodActivity::class.java))
        }

        findViewById<View>(R.id.btnAdminCategories).setOnClickListener {
            startActivity(Intent(this, AdminCategoryActivity::class.java))
        }

        findViewById<View>(R.id.btnAdminOrders).setOnClickListener {
            startActivity(Intent(this, AdminOrderActivity::class.java))
        }

        // ==========================================
        // MENU TRANG TÔI
        // ==========================================

        // Ví Voucher
        findViewById<View>(R.id.btnVoucher).setOnClickListener {
            openVoucherWallet()
        }

        // Thanh toán
        findViewById<View>(R.id.btnPayment).setOnClickListener {
            openPaymentMethodsDialog()
        }

        // Địa chỉ
        findViewById<View>(R.id.btnAddress).setOnClickListener {
            openAddressBook()
        }

        // Trung tâm trợ giúp
        findViewById<View>(R.id.btnHelp).setOnClickListener {
            openHelpCenterDialog()
        }

        // Cài đặt
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            openSettingsDialog()
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

        findViewById<View>(R.id.navHome).setOnClickListener {
            openMainTab("home")
        }

        findViewById<View>(R.id.navOrders).setOnClickListener {
            openMainTab("orders")
        }

        findViewById<View>(R.id.navFavorite).setOnClickListener {
            openMainTab("likes")
        }

        findViewById<View>(R.id.navNotification).setOnClickListener {
            openMainTab("notifications")
        }

        findViewById<View>(R.id.navAccount).setOnClickListener {
            // Đang ở AccountActivity
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserHeader()
        refreshNotifBadge()
        refreshFavBadge()
        refreshVoucherCount()
        NotificationStore.onChanged = { refreshNotifBadge() }
        FavoriteManager.onChanged = { refreshFavBadge() }
    }

    override fun onPause() {
        super.onPause()
        NotificationStore.onChanged = null
        FavoriteManager.onChanged = null
    }

    private fun refreshVoucherCount() {
        val tvCount = findViewById<TextView>(R.id.tvVoucherCount) ?: return
        val count = VoucherStore.getTotalCount()
        tvCount.text = "$count voucher"
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

    private fun refreshUserHeader() {
        findViewById<TextView>(R.id.tvUserName).text = UserProfileManager.name
        findViewById<TextView>(R.id.tvAvatarLetter).text = UserProfileManager.avatarLetter

        val isAdmin = UserProfileManager.isAdmin
        findViewById<TextView>(R.id.tvUserRoleBadge).visibility = if (isAdmin) View.VISIBLE else View.GONE
        findViewById<View>(R.id.llAdminSection).visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun openUserInfo() {
        val intent = Intent(this, UserInfoActivity::class.java)
        startActivity(intent)
    }

    // ==========================================
    // VÍ VOUCHER
    // ==========================================
    private fun openVoucherWallet() {
        val intent = Intent(this, VoucherActivity::class.java)
        startActivity(intent)
    }

    // ==========================================
    // SỔ ĐỊA CHỈ GIAO HÀNG
    // ==========================================
    private fun openAddressBook() {
        val intent = Intent(this, AddressActivity::class.java)
        startActivity(intent)
    }

    // ==========================================
    // CÀI ĐẶT ỨNG DỤNG
    // ==========================================
    private fun openSettingsDialog() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    // ==========================================
    // PHƯƠNG THỨC THANH TOÁN
    // ==========================================
    private fun openPaymentMethodsDialog() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_payment_methods, null)
        bottomSheet.setContentView(view)

        // Làm trong suốt nền mặc định của bottom sheet để lộ bo góc mềm mại
        view.post {
            (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        }

        var selected = PaymentStore.getDefaultMethod(this)

        val cardCod = view.findViewById<View>(R.id.cardCod)
        val cardMomo = view.findViewById<View>(R.id.cardMomo)
        val cardBank = view.findViewById<View>(R.id.cardBank)
        val cardCard = view.findViewById<View>(R.id.cardCard)

        val radioCod = view.findViewById<ImageView>(R.id.radioCod)
        val radioMomo = view.findViewById<ImageView>(R.id.radioMomo)
        val radioBank = view.findViewById<ImageView>(R.id.radioBank)
        val radioCard = view.findViewById<ImageView>(R.id.radioCard)

        val tagCod = view.findViewById<TextView>(R.id.tagCod)
        val tagMomo = view.findViewById<TextView>(R.id.tagMomo)
        val tagBank = view.findViewById<TextView>(R.id.tagBank)
        val tagCard = view.findViewById<TextView>(R.id.tagCard)

        fun updateUi() {
            // COD
            val isCod = (selected == PaymentStore.METHOD_COD)
            cardCod.setBackgroundResource(if (isCod) R.drawable.bg_payment_card_selected else R.drawable.bg_payment_card_normal)
            radioCod.setImageResource(if (isCod) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
            tagCod.text = if (isCod) "Mặc định" else "Phổ biến"
            tagCod.setBackgroundResource(if (isCod) R.drawable.bg_tag_default else R.drawable.bg_tag_gray)
            tagCod.setTextColor(if (isCod) Color.parseColor("#03543F") else Color.parseColor("#475569"))

            // MOMO
            val isMomo = (selected == PaymentStore.METHOD_MOMO)
            cardMomo.setBackgroundResource(if (isMomo) R.drawable.bg_payment_card_selected else R.drawable.bg_payment_card_normal)
            radioMomo.setImageResource(if (isMomo) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
            tagMomo.text = if (isMomo) "Mặc định" else "Khuyên dùng"
            tagMomo.setBackgroundResource(if (isMomo) R.drawable.bg_tag_default else R.drawable.bg_tag_momo)
            tagMomo.setTextColor(if (isMomo) Color.parseColor("#03543F") else Color.parseColor("#9D174D"))

            // BANK
            val isBank = (selected == PaymentStore.METHOD_BANK)
            cardBank.setBackgroundResource(if (isBank) R.drawable.bg_payment_card_selected else R.drawable.bg_payment_card_normal)
            radioBank.setImageResource(if (isBank) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
            tagBank.text = if (isBank) "Mặc định" else "24/7 Miễn phí"
            tagBank.setBackgroundResource(if (isBank) R.drawable.bg_tag_default else R.drawable.bg_tag_bank)
            tagBank.setTextColor(if (isBank) Color.parseColor("#03543F") else Color.parseColor("#1E40AF"))

            // CARD
            val isCard = (selected == PaymentStore.METHOD_CARD)
            cardCard.setBackgroundResource(if (isCard) R.drawable.bg_payment_card_selected else R.drawable.bg_payment_card_normal)
            radioCard.setImageResource(if (isCard) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked)
            tagCard.text = if (isCard) "Mặc định" else "Bảo mật"
            tagCard.setBackgroundResource(if (isCard) R.drawable.bg_tag_default else R.drawable.bg_tag_card)
            tagCard.setTextColor(if (isCard) Color.parseColor("#03543F") else Color.parseColor("#6D28D9"))
        }

        updateUi()

        cardCod.setOnClickListener {
            selected = PaymentStore.METHOD_COD
            updateUi()
        }
        cardMomo.setOnClickListener {
            selected = PaymentStore.METHOD_MOMO
            updateUi()
        }
        cardBank.setOnClickListener {
            selected = PaymentStore.METHOD_BANK
            updateUi()
        }
        cardCard.setOnClickListener {
            selected = PaymentStore.METHOD_CARD
            updateUi()
        }

        view.findViewById<View>(R.id.btnClosePayment).setOnClickListener {
            bottomSheet.dismiss()
        }

        view.findViewById<Button>(R.id.btnSavePayment).setOnClickListener {
            PaymentStore.setDefaultMethod(this, selected)
            val name = when (selected) {
                PaymentStore.METHOD_MOMO -> "Ví điện tử MoMo"
                PaymentStore.METHOD_BANK -> "Chuyển khoản VietQR"
                PaymentStore.METHOD_CARD -> "Thẻ ATM / Visa / Master"
                else -> "Tiền mặt (COD)"
            }
            Toast.makeText(this, "Đã lưu $name làm phương thức thanh toán mặc định", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    // ==========================================
    // TRUNG TÂM TRỢ GIÚP & FAQ
    // ==========================================
    private fun openHelpCenterDialog() {
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }
        scrollView.addView(container)

        fun addFaq(q: String, a: String) {
            val tvQ = TextView(this).apply {
                text = q
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
                setPadding(0, 12, 0, 4)
            }
            val tvA = TextView(this).apply {
                text = a
                textSize = 13f
                setTextColor(Color.parseColor("#4B5563"))
                setLineSpacing(4f, 1f)
                setPadding(0, 0, 0, 8)
            }
            container.addView(tvQ)
            container.addView(tvA)
        }

        addFaq("1. Làm sao để áp dụng mã giảm giá?", "Tại trang Đặt hàng (Checkout), bạn bấm 'Chọn mã voucher' hoặc nhập mã (ví dụ: FIRST30) và bấm 'Áp dụng'. Số tiền sẽ được trừ trực tiếp vào tổng đơn.")
        addFaq("2. Đánh giá món ăn sau khi giao như thế nào?", "Sau khi đơn hàng chuyển sang trạng thái 'Đã giao', bạn mở Chi tiết đơn hàng và bấm 'Đánh giá món ăn ngay' để chấm 1-5 sao và viết nhận xét.")
        addFaq("3. Phí giao hàng được tính thế nào?", "Miễn phí giao hàng cho đơn từ 50.000đ; đơn dưới 50.000đ phí 15.000đ. Đơn gồm nhiều quán sẽ tính phí theo từng quán do tài xế lấy món độc lập.")
        addFaq("4. Hotline hỗ trợ:", "Tổng đài: 1900 1234 (8:00 - 22:00 hàng ngày)\nEmail CSKH: hotro@foodhub.vn")

        AlertDialog.Builder(this)
            .setTitle("❓ Trung tâm trợ giúp & FAQ")
            .setView(scrollView)
            .setPositiveButton("Đóng", null)
            .show()
    }

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
        overridePendingTransition(0, 0)
    }
}
