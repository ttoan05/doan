package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.User
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.ImageUtils
import java.io.File

/**
 * Màn hình Quản lý khách hàng dành cho Quản trị viên (Admin):
 * STT 1: Xem danh sách khách hàng
 * STT 2: Xem chi tiết khách hàng
 * STT 3: Thêm khách hàng mới
 * STT 4: Chỉnh sửa thông tin khách hàng
 * STT 5: Tìm kiếm khách hàng (tên, email, SĐT)
 * STT 6: Khóa / Mở khóa tài khoản
 * STT 7: Xem lịch sử đơn hàng của khách hàng
 */
class AdminCustomerActivity : AppCompatActivity() {

    internal lateinit var dbHelper: DatabaseHelper
    private lateinit var llCustomerContainer: LinearLayout
    private lateinit var llFilterChips: LinearLayout
    private lateinit var tvCustomerCount: TextView
    private lateinit var etSearchCustomer: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvEmptyMessage: TextView

    private var allCustomers: List<User> = emptyList()
    private var selectedFilter: String = "Tất cả"
    private var searchQuery: String = ""

    private val filters = listOf("Tất cả", "Đang hoạt động", "Đã khóa")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_customer)

        dbHelper = DatabaseHelper.getInstance(this)

        // Phân quyền bảo vệ: Chỉ Admin mới được truy cập
        if (!UserProfileManager.isAdmin) {
            Toast.makeText(this, "Chỉ Quản trị viên mới có quyền truy cập!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        llCustomerContainer = findViewById(R.id.llCustomerContainer)
        llFilterChips       = findViewById(R.id.llFilterChips)
        tvCustomerCount     = findViewById(R.id.tvCustomerCount)
        etSearchCustomer    = findViewById(R.id.etSearchCustomer)
        btnClearSearch      = findViewById(R.id.btnClearSearch)
        llEmptyState        = findViewById(R.id.llEmptyState)
        tvEmptyMessage      = findViewById(R.id.tvEmptyMessage)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnAddCustomer).setOnClickListener {
            AdminCustomerDialogHelper.showAddCustomerDialog(this) {
                loadData()
            }
        }

        findViewById<View>(R.id.btnRefresh).setOnClickListener {
            Toast.makeText(this, "Đã làm mới danh sách", Toast.LENGTH_SHORT).show()
            loadData()
        }

        etSearchCustomer.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
            renderCustomerList()
        }

        btnClearSearch.setOnClickListener {
            etSearchCustomer.setText("")
        }

        renderFilterChips()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        allCustomers = dbHelper.getAllCustomers()
        renderFilterChips()
        renderCustomerList()
    }

    private fun renderFilterChips() {
        llFilterChips.removeAllViews()

        val activeCount = allCustomers.count { !it.status.equals("LOCKED", ignoreCase = true) }
        val lockedCount = allCustomers.count { it.status.equals("LOCKED", ignoreCase = true) }

        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            val countText = when (filter) {
                "Tất cả" -> "${allCustomers.size}"
                "Đang hoạt động" -> "$activeCount"
                "Đã khóa" -> "$lockedCount"
                else -> ""
            }

            val chip = TextView(this).apply {
                text = "$filter ($countText)"
                textSize = 12.5f
                gravity = Gravity.CENTER
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) R.drawable.bg_cat_active else R.drawable.bg_chip_normal)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#475569"))

                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(34)
                )
                lp.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = lp
                setPadding(dpToPx(14), 0, dpToPx(14), 0)

                setOnClickListener {
                    if (selectedFilter != filter) {
                        selectedFilter = filter
                        renderFilterChips()
                        renderCustomerList()
                    }
                }
            }
            llFilterChips.addView(chip)
        }
    }

    private fun renderCustomerList() {
        llCustomerContainer.removeAllViews()

        // 1. Filter by status chip
        var filtered = when (selectedFilter) {
            "Đang hoạt động" -> allCustomers.filter { !it.status.equals("LOCKED", ignoreCase = true) }
            "Đã khóa" -> allCustomers.filter { it.status.equals("LOCKED", ignoreCase = true) }
            else -> allCustomers
        }

        // 2. Filter by search query (name, email, phone)
        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase()
            filtered = filtered.filter { user ->
                user.name.lowercase().contains(q) ||
                user.email.lowercase().contains(q) ||
                user.phone.contains(q) ||
                String.format("#KH%03d", user.id).lowercase().contains(q) ||
                user.address.lowercase().contains(q)
            }
        }

        // Summary text
        tvCustomerCount.text = "Hiển thị ${filtered.size} / ${allCustomers.size} khách hàng"

        if (filtered.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            tvEmptyMessage.text = if (searchQuery.isNotEmpty()) {
                "Không tìm thấy khách hàng nào khớp với \"$searchQuery\""
            } else {
                "Chưa có khách hàng nào trong mục này"
            }
            return
        }

        llEmptyState.visibility = View.GONE
        val inflater = layoutInflater

        filtered.forEach { user ->
            val cardView = inflater.inflate(R.layout.item_admin_customer, llCustomerContainer, false)

            val tvLetter = cardView.findViewById<TextView>(R.id.tvCustomerAvatarLetter)
            val ivPhoto = cardView.findViewById<ImageView>(R.id.ivCustomerAvatarPhoto)
            val tvName = cardView.findViewById<TextView>(R.id.tvCustomerName)
            val tvCode = cardView.findViewById<TextView>(R.id.tvCustomerCode)
            val tvRoleBadge = cardView.findViewById<TextView>(R.id.tvCustomerRoleBadge)
            val tvStatusBadge = cardView.findViewById<TextView>(R.id.tvCustomerStatusBadge)
            val tvPhone = cardView.findViewById<TextView>(R.id.tvCustomerPhone)
            val tvEmail = cardView.findViewById<TextView>(R.id.tvCustomerEmail)
            val tvAddress = cardView.findViewById<TextView>(R.id.tvCustomerAddress)

            val btnViewOrders = cardView.findViewById<View>(R.id.btnViewOrders)
            val btnDetail = cardView.findViewById<View>(R.id.btnCustomerDetail)
            val btnEdit = cardView.findViewById<View>(R.id.btnCustomerEdit)
            val btnToggleLock = cardView.findViewById<TextView>(R.id.btnCustomerToggleLock)

            // Bind values
            tvName.text = user.name
            val formattedCode = String.format("#KH%03d", user.id)
            tvCode.text = "Mã KH: $formattedCode"

            val isAdmin = user.role.equals("admin", ignoreCase = true)
            tvRoleBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE

            // Avatar
            renderAvatar(user, tvLetter, ivPhoto)

            // Status Badge
            val isLocked = user.status.equals("LOCKED", ignoreCase = true)
            if (isLocked) {
                tvStatusBadge.text = "● Đã khóa"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_red)
                tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))

                btnToggleLock.text = " Mở"
                btnToggleLock.setTextColor(Color.parseColor("#16A34A"))
            } else {
                tvStatusBadge.text = "● Hoạt động"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_green)
                tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))

                btnToggleLock.text = " Khóa"
                btnToggleLock.setTextColor(Color.parseColor("#DC2626"))
            }

            tvPhone.text = user.phone.ifBlank { "Chưa cập nhật SĐT" }
            tvEmail.text = user.email.ifBlank { "Chưa cập nhật email" }
            tvAddress.text = user.address.ifBlank { "Chưa cập nhật địa chỉ" }

            // Card click -> Xem chi tiết
            cardView.setOnClickListener {
                AdminCustomerDialogHelper.showCustomerDetailDialog(this, user) {
                    loadData()
                }
            }

            btnDetail.setOnClickListener {
                AdminCustomerDialogHelper.showCustomerDetailDialog(this, user) {
                    loadData()
                }
            }

            btnViewOrders.setOnClickListener {
                AdminCustomerDialogHelper.showCustomerOrdersDialog(this, user)
            }

            btnEdit.setOnClickListener {
                AdminCustomerDialogHelper.showEditCustomerDialog(this, user) {
                    loadData()
                }
            }

            btnToggleLock.setOnClickListener {
                AdminCustomerDialogHelper.showToggleStatusConfirm(this, user) {
                    loadData()
                }
            }

            llCustomerContainer.addView(cardView)
        }
    }

    private fun renderAvatar(user: User, tvLetter: TextView, ivPhoto: ImageView) {
        if (user.avatarUri.isNotEmpty()) {
            val circular = ImageUtils.getCircularAvatar(user.avatarUri, 160)
            if (circular != null) {
                ivPhoto.setImageBitmap(circular)
                ivPhoto.visibility = View.VISIBLE
                tvLetter.visibility = View.GONE
                return
            }
        }
        val letter = user.name.trim().firstOrNull()?.uppercase() ?: "U"
        tvLetter.text = letter
        tvLetter.visibility = View.VISIBLE
        ivPhoto.visibility = View.GONE
    }

    internal fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
