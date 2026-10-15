package com.example.foodhub.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.VoucherStore
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.FavSort
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.ui.account.AccountActivity
import com.example.foodhub.ui.cart.CartActivity
import com.example.foodhub.ui.notification.NotificationAdapter
import com.example.foodhub.ui.notification.SwipeToDeleteCallback
import com.example.foodhub.ui.order.OrderAdapter
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.ui.product.ProductDetailActivity
import com.example.foodhub.ui.restaurant.RestaurantDetailActivity
import com.example.foodhub.ui.voucher.VoucherActivity
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

class MainActivity : AppCompatActivity() {

    private lateinit var contentContainer: LinearLayout
    private lateinit var tvToast: TextView
    private val handler = Handler(Looper.getMainLooper())

    // Tab label references
    private lateinit var labelHome: TextView
    private lateinit var labelOrders: TextView
    private lateinit var labelLikes: TextView
    private lateinit var labelNotifications: TextView
    private lateinit var labelAccount: TextView

    // Badge giỏ hàng: giữ tham chiếu ở cấp Activity để có thể cập nhật lại
    // khi quay về từ CartActivity / CheckoutActivity (onResume).
    private var cartBadge: TextView? = null

    // Badge số thông báo chưa đọc trên tab chuông
    private lateinit var tvNotifBadge: TextView

    // Badge số mục yêu thích trên tab tim
    private lateinit var tvFavBadge: TextView

    // Adapter của trang Home, giữ lại để cập nhật lại icon tim khi có thay đổi từ nơi khác
    private var homeFoodAdapter: FoodAdapter? = null
    private var homeRestaurantAdapter: RestaurantAdapter? = null

    // Hàm làm mới danh sách khi đang ở tab Thông báo (gán trong showNotificationsContent)
    private var notifRefresh: (() -> Unit)? = null

    // Bộ lọc loại thông báo đang chọn; null = "Tất cả"
    private var selectedNotifFilter: NotificationType? = null

    // Tab đang hiển thị, dùng để biết có cần refresh khi onResume không.
    private var currentTab: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nạp danh sách yêu thích và dữ liệu đã lưu
        try {
            FavoriteManager.init(this)
            CartManager.ensureLoaded(this)
            OrderManager.ensureLoaded(this)
            NotificationStore.ensureLoaded(this)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContentView(R.layout.activity_main)

        contentContainer = findViewById(R.id.contentContainer)
        tvToast = findViewById(R.id.tvToast)

        labelHome          = findViewById(R.id.labelHome)
        labelOrders        = findViewById(R.id.labelOrders)
        labelLikes         = findViewById(R.id.labelLikes)
        labelNotifications = findViewById(R.id.labelNotifications)
        labelAccount       = findViewById(R.id.labelAccount)
        tvNotifBadge       = findViewById(R.id.tvNotifBadge)
        tvFavBadge         = findViewById(R.id.tvFavBadge)

        // Tab click listeners
        findViewById<LinearLayout>(R.id.tabHome).setOnClickListener { selectTab("home") }
        findViewById<LinearLayout>(R.id.tabOrders).setOnClickListener { selectTab("orders") }
        findViewById<LinearLayout>(R.id.tabLikes).setOnClickListener { selectTab("likes") }
        findViewById<LinearLayout>(R.id.tabNotifications).setOnClickListener { selectTab("notifications") }
        findViewById<LinearLayout>(R.id.tabAccount).setOnClickListener {
            startActivity(
                Intent(this, AccountActivity::class.java)
            )

            overridePendingTransition(0, 0)
        }

        val selectedTab = intent.getStringExtra("selected_tab")
        // Start on requested tab, or Home if none given
        selectTab(selectedTab ?: "home")
        updateNotifBadge()
        updateFavBadge()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val selectedTab = intent?.getStringExtra("selected_tab")

        if (selectedTab != null) {
            selectedOrderFilter = null
            selectTab(selectedTab)
        }
    }

    override fun onResume() {
        super.onResume()
        CartManager.ensureLoaded(this)
        OrderManager.ensureLoaded(this)
        NotificationStore.ensureLoaded(this)
        // Cập nhật lại giỏ hàng (có thể đã thay đổi ở CartActivity/CheckoutActivity)
        refreshCartBadge()
        // Đơn hàng có thể vừa được tạo/hủy/tự chuyển trạng thái -> refresh nếu đang ở tab Đơn hàng
        if (currentTab == "orders") {
            showOrdersContent()
        }
        OrderManager.onChanged = {
            if (currentTab == "orders") showOrdersContent()
        }

        // Thông báo: cập nhật badge + danh sách (có thể có thông báo mới khi đang ở màn khác)
        updateNotifBadge()
        if (currentTab == "notifications") notifRefresh?.invoke()
        NotificationStore.onChanged = {
            updateNotifBadge()
            if (currentTab == "notifications") notifRefresh?.invoke()
        }
        // Có thông báo mới lúc đang mở app -> hiện banner nhỏ ở trên cùng
        NotificationStore.onNewNotification = { n ->
            showToast("🔔 ${n.title}")
        }

        // Yêu thích: cập nhật badge + icon tim + danh sách (có thể vừa thả tim ở trang chi tiết)
        updateFavBadge()
        refreshAfterFavoriteChange()
        FavoriteManager.onChanged = {
            updateFavBadge()
            refreshAfterFavoriteChange()
        }
    }

    override fun onPause() {
        super.onPause()
        OrderManager.onChanged = null
        NotificationStore.onChanged = null
        NotificationStore.onNewNotification = null
        FavoriteManager.onChanged = null
    }

    private fun selectTab(tab: String) {
        currentTab = tab
        val redColor  = getColor(R.color.red_primary)
        val greyColor = getColor(R.color.text_grey)

        // Reset all labels to grey
        listOf(labelHome, labelOrders, labelLikes, labelNotifications, labelAccount).forEach {
            it.setTextColor(greyColor)
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        when (tab) {
            "home" -> {
                labelHome.setTextColor(redColor)
                labelHome.setTypeface(null, android.graphics.Typeface.BOLD)
                showHomeContent()
            }
            "orders" -> {
                labelOrders.setTextColor(redColor)
                labelOrders.setTypeface(null, android.graphics.Typeface.BOLD)
                showOrdersContent()
            }
            "likes" -> {
                labelLikes.setTextColor(redColor)
                labelLikes.setTypeface(null, android.graphics.Typeface.BOLD)
                showFavoritesContent()
            }
            "notifications" -> {
                labelNotifications.setTextColor(redColor)
                labelNotifications.setTypeface(null, android.graphics.Typeface.BOLD)
                showNotificationsContent()
            }
        }
    }

    // ─── Home Content ─────────────────────────────────
    private fun showHomeContent() {
        contentContainer.removeAllViews()

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }
        val tvLogo = TextView(this).apply { text = "🏠"; textSize = 22f }
        val tvTitle = TextView(this).apply {
            text = "FoodHub"; textSize = 20f
            setTextColor(getColor(R.color.red_primary))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        // ==========================================
// GIỎ HÀNG + CHẤM SỐ LƯỢNG
// ==========================================

        val cartContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        }

// Hình giỏ hàng
        val tvCart = TextView(this).apply {
            text = "🛒"
            textSize = 25f
            gravity = Gravity.CENTER

            layoutParams = FrameLayout.LayoutParams(
                dp(55),
                dp(55)
            )

            setOnClickListener {
                startActivity(Intent(this@MainActivity, CartActivity::class.java))
            }
        }

// Chấm đỏ + số lượng
        val cartBadgeView = TextView(this).apply {

            text = CartManager.totalQuantity().toString()

            textSize = 10f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)

            gravity = Gravity.CENTER

            // Nền đỏ hình tròn
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E5394F"))
            }

            layoutParams = FrameLayout.LayoutParams(
                dp(22),
                dp(22)
            ).apply {

                gravity = Gravity.TOP or Gravity.END

                // Đẩy badge lên phía trên bên phải
                topMargin = dp(2)
                rightMargin = dp(0)
            }

            // Chưa có sản phẩm thì ẩn
            visibility =
                if (CartManager.totalQuantity() > 0) View.VISIBLE
                else View.GONE
        }
        cartBadge = cartBadgeView

        cartContainer.addView(tvCart)
        cartContainer.addView(cartBadgeView)

        header.addView(tvLogo)
        header.addView(tvTitle)
        header.addView(cartContainer)
        contentContainer.addView(header)

        // ==========================================
        // Ô TÌM KIẾM (món ăn, nhà hàng)
        // ==========================================
        // foodAdapter / restaurantAdapter được khai báo ở đây (trước khi tạo)
        // để cả ô tìm kiếm lẫn các nút danh mục phía dưới đều gọi lọc được.
        var foodAdapter: FoodAdapter? = null
        var restaurantAdapter: RestaurantAdapter? = null
        var searchQuery = ""
        var selectedCat = "Tất cả"

        // Dòng hiển thị khi tìm không ra kết quả nào, tạo trước và thêm vào
        // cuối các section bên dưới; mặc định ẩn.
        val tvSearchEmpty = TextView(this).apply {
            text = "Không tìm thấy món ăn hoặc nhà hàng phù hợp"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_grey))
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(24), dp(16), dp(16))
            layoutParams = lp
        }

        fun applyFoodFilter() {
            var list = SampleData.FOODS
            if (selectedCat != "Tất cả") {
                list = list.filter { it.category == selectedCat }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) || it.restaurant.lowercase().contains(q)
                }
            }
            foodAdapter?.submit(list)
        }

        fun applyRestaurantFilter() {
            var list = SampleData.RESTAURANTS
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) || it.desc.lowercase().contains(q)
                }
            }
            restaurantAdapter?.submit(list)
        }

        fun updateSearchEmptyState() {
            val noResult = searchQuery.isNotBlank() &&
                    (foodAdapter?.currentCount() ?: 0) == 0 &&
                    (restaurantAdapter?.currentCount() ?: 0) == 0
            tvSearchEmpty.visibility = if (noResult) View.VISIBLE else View.GONE
        }

        fun onSearchQueryChanged(text: String) {
            searchQuery = text
            applyFoodFilter()
            applyRestaurantFilter()
            updateSearchEmptyState()
        }

        val searchBar = EditText(this).apply {
            hint = "🔍  Tìm kiếm món ăn, nhà hàng..."
            textSize = 14f
            setTextColor(getColor(R.color.text_dark))
            setHintTextColor(getColor(R.color.text_grey))
            setBackgroundResource(R.drawable.bg_search)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), dp(12), dp(16), dp(12))
            layoutParams = lp

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    onSearchQueryChanged(s?.toString().orEmpty())
                }
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // Ẩn bàn phím khi người dùng bấm nút tìm kiếm trên bàn phím
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(windowToken, 0)
                    true
                } else {
                    false
                }
            }
        }
        contentContainer.addView(searchBar)

        // Promo Banner - Chỉ hiển thị cho tài khoản chưa bấm DÙNG NGAY và chưa sử dụng FIRST30
        if (!VoucherStore.isWelcomeBannerDismissed(this)) {
            val promoBanner = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_promo)
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(dp(16), 0, dp(16), dp(16))
                layoutParams = lp
                setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            val promoText = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val promoTitle = TextView(this).apply {
                text = "🎉 Giảm 30% cho đơn đầu tiên"
                textSize = 13f; setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(getColor(R.color.red_primary))
            }
            val promoSub = TextView(this).apply {
                text = "Nhập mã: FIRST30"; textSize = 12f
                setTextColor(getColor(R.color.red_primary))
            }
            promoText.addView(promoTitle); promoText.addView(promoSub)
            val promoBtn = Button(this).apply {
                text = "DÙNG NGAY"
                textSize = 13f
                setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_btn_red)
                setOnClickListener {
                    VoucherStore.markWelcomeBannerDismissed(this@MainActivity)
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Voucher Code", "FIRST30")
                        clipboard.setPrimaryClip(clip)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    showToast("Đã nhận mã FIRST30! Mở Ví Voucher của bạn")
                    promoBanner.visibility = View.GONE
                    contentContainer.removeView(promoBanner)
                    startActivity(Intent(this@MainActivity, VoucherActivity::class.java))
                }
            }
            promoBanner.addView(promoText); promoBanner.addView(promoBtn)
            contentContainer.addView(promoBanner)
        }

        // Categories
        val catScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dp(16)); layoutParams = lp
        }
        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        val catButtons = mutableListOf<Button>()

        fun updateCatButtons() {
            catButtons.forEachIndexed { i, btn ->
                val isActive = SampleData.CATEGORIES[i] == selectedCat
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
        }

        SampleData.CATEGORIES.forEach { cat ->
            val btn = Button(this).apply {
                text = cat; textSize = 14f
                setBackgroundResource(if (cat == selectedCat) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                setTextColor(if (cat == selectedCat) Color.WHITE else getColor(R.color.text_grey2))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, dp(8), 0); layoutParams = lp
                setOnClickListener {
                    selectedCat = cat
                    updateCatButtons()
                    // Lọc lại danh sách món ăn theo danh mục vừa chọn
                    applyFoodFilter()
                    updateSearchEmptyState()
                }
            }
            catButtons.add(btn); catRow.addView(btn)
        }
        catScroll.addView(catRow); contentContainer.addView(catScroll)

        // Section: Món ngon đề xuất
        addSectionHeader("Món ngon đề xuất", "Xem tất cả") { showToast("Xem tất cả món") }
        val foodRecycler = RecyclerView(this).apply {

            layoutManager = GridLayoutManager(
                this@MainActivity,
                2
            )

            adapter = FoodAdapter(
                initialFoods = SampleData.FOODS,
                onItemClick = { food -> openFoodDetail(food) },
                onFavoriteChanged = { food, isFav, oldTime -> onFoodHeartChanged(food, isFav, oldTime) },
                onAddToCart = { food -> onAddToCart(food) }
            ).also { foodAdapter = it; homeFoodAdapter = it }

            isNestedScrollingEnabled = false

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            lp.setMargins(
                dp(10),
                0,
                dp(10),
                dp(24)
            )

            layoutParams = lp
        }

        contentContainer.addView(foodRecycler)

        // Section: Nhà hàng nổi bật
        addSectionHeader("Nhà hàng nổi bật", "Xem thêm") { showToast("Xem thêm nhà hàng") }
        val restaurantRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = RestaurantAdapter(
                initialRestaurants = SampleData.RESTAURANTS,
                onFavoriteChanged = { r, isFav, oldTime -> onRestaurantHeartChanged(r, isFav, oldTime) },
                onViewMenu = { r -> openRestaurantDetail(r) }
            ).also { restaurantAdapter = it; homeRestaurantAdapter = it }
            isNestedScrollingEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), dp(16)); layoutParams = lp
        }
        contentContainer.addView(restaurantRecycler)
        contentContainer.addView(tvSearchEmpty)
    }

    private fun onAddToCart(food: Food) {
        CartManager.addFood(food)
        refreshCartBadge()

        showToast(
            "Đã thêm ${food.name} vào giỏ hàng 🛒"
        )
    }

    private fun refreshCartBadge() {
        val badge = cartBadge ?: return
        val count = CartManager.totalQuantity()
        badge.text = count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun addSectionHeader(title: String, actionText: String, onAction: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), dp(12)); layoutParams = lp
        }
        val tvTitle = TextView(this).apply {
            text = title; textSize = 16f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvAction = TextView(this).apply {
            text = actionText; textSize = 13f
            setTextColor(getColor(R.color.red_primary))
            setOnClickListener { onAction() }
        }
        row.addView(tvTitle); row.addView(tvAction)
        contentContainer.addView(row)
    }

    // ─── Orders Content (Trang Đơn hàng) ───────────────
    // Bộ lọc trạng thái hiện đang chọn cho tab Đơn hàng.
    private var selectedOrderFilter: OrderStatus? = null // null = "Tất cả"

    private fun showOrdersContent() {
        contentContainer.removeAllViews()
        cartBadge = null // header của trang Home không còn hiển thị ở đây

        // Header kèm chú thích lưu trong 3 ngày
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(12))
        }
        val tvTitle = TextView(this).apply {
            text = "Đơn hàng"; textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_dark))
        }
        val tvSub = TextView(this).apply {
            text = "Lưu lịch sử các đơn hàng trong 3 ngày gần nhất"
            textSize = 12f
            setTextColor(getColor(R.color.text_grey))
            setPadding(0, dp(2), 0, 0)
        }
        header.addView(tvTitle)
        header.addView(tvSub)
        contentContainer.addView(header)

        // Tabs trạng thái: Tất cả / Đang xử lý / Đang giao / Lịch sử / Đã hủy
        val filters: List<Pair<String, OrderStatus?>> = listOf(
            "Tất cả" to null,
            "Đang xử lý" to OrderStatus.PROCESSING,
            "Đang giao" to OrderStatus.SHIPPING,
            "Lịch sử" to OrderStatus.DELIVERED,
            "Đã hủy" to OrderStatus.CANCELLED
        )

        val filterScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dp(12)); layoutParams = lp
        }
        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), 0)
        }

        val orderRecycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            isNestedScrollingEnabled = false
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), dp(16)); layoutParams = lp
        }
        val orderAdapter = OrderAdapter(
            onDetail = { order -> openOrderDetail(order.id) },
            onReorder = { order ->
                CartManager.addItems(order.items)
                refreshCartBadge()
                showToast("Đã thêm ${order.totalQuantity} món vào giỏ hàng")
            }
        )
        orderRecycler.adapter = orderAdapter

        val emptyView = TextView(this).apply {
            text = "Chưa có đơn hàng nào"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_grey))
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(60)
            layoutParams = lp
        }

        val filterButtons = mutableListOf<Button>()

        fun refreshOrderList() {
            val list = OrderManager.getOrders(selectedOrderFilter)
            orderAdapter.submit(list)
            emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            orderRecycler.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            emptyView.text = when (selectedOrderFilter) {
                null -> "Bạn chưa có đơn hàng nào trong 3 ngày qua"
                OrderStatus.DELIVERED -> "Chưa có đơn hàng nào trong lịch sử (3 ngày qua)"
                OrderStatus.PROCESSING -> "Không có đơn hàng nào đang xử lý"
                OrderStatus.SHIPPING -> "Không có đơn hàng nào đang giao"
                OrderStatus.CANCELLED -> "Không có đơn hàng nào đã hủy"
            }

            filterButtons.forEachIndexed { i, btn ->
                val (label, status) = filters[i]
                val count = OrderManager.countOrders(status)
                btn.text = if (count > 0) "$label ($count)" else label
                val isActive = status == selectedOrderFilter
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
        }

        filters.forEach { (label, status) ->
            val btn = Button(this).apply {
                text = label; textSize = 13f
                isAllCaps = false
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, dp(8), 0); layoutParams = lp
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener {
                    selectedOrderFilter = status
                    refreshOrderList()
                }
            }
            filterButtons.add(btn); filterRow.addView(btn)
        }
        filterScroll.addView(filterRow)
        contentContainer.addView(filterScroll)
        contentContainer.addView(orderRecycler)
        contentContainer.addView(emptyView)

        refreshOrderList()
    }

    private fun openOrderDetail(orderId: String) {
        val intent = Intent(this, OrderDetailActivity::class.java)
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId)
        startActivity(intent)
    }

    // ─── Favorites Content (Trang Yêu thích) ───────────
    private var favTab = "foods"                 // "foods" | "restaurants"
    private var favSearch = ""
    private var favCategory = "Tất cả"
    private var favSort = FavSort.NEWEST
    private var favRefresh: (() -> Unit)? = null

    // Kiểu sắp xếp hợp lệ cho từng tab
    private val foodSorts = listOf(FavSort.NEWEST, FavSort.PRICE_ASC, FavSort.PRICE_DESC, FavSort.RATING)
    private val restaurantSorts = listOf(FavSort.NEWEST, FavSort.RATING, FavSort.NAME)

    private fun updateFavBadge() {
        val count = FavoriteManager.totalCount()
        tvFavBadge.text = if (count > 9) "9+" else count.toString()
        tvFavBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    /** Có thay đổi yêu thích -> làm mới màn hình đang hiển thị (Home hoặc Yêu thích). */
    private fun refreshAfterFavoriteChange() {
        when (currentTab) {
            "home" -> {
                homeFoodAdapter?.notifyDataSetChanged()
                homeRestaurantAdapter?.notifyDataSetChanged()
            }
            "likes" -> {
                if (favRefresh != null) {
                    favRefresh?.invoke()
                } else {
                    showFavoritesContent()
                }
            }
        }
    }

    private fun openFoodDetail(food: Food) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra(ProductDetailActivity.EXTRA_FOOD_ID, food.id)
        startActivity(intent)
    }

    private fun openRestaurantDetail(restaurant: Restaurant) {
        val intent = Intent(this, RestaurantDetailActivity::class.java)
        intent.putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_ID, restaurant.id)
        startActivity(intent)
    }

    private fun onFoodHeartChanged(food: Food, isFav: Boolean, oldTime: Long) {
        if (isFav) {
            showToast("Đã thêm ${food.name} vào Yêu thích ❤️")
        } else {
            showUndoSnackbar("Đã bỏ ${food.name} khỏi Yêu thích") {
                FavoriteManager.restoreFood(food.id, oldTime)
            }
        }
    }

    private fun onRestaurantHeartChanged(r: Restaurant, isFav: Boolean, oldTime: Long) {
        if (isFav) {
            showToast("Đã thêm ${r.name} vào Yêu thích ❤️")
        } else {
            showUndoSnackbar("Đã bỏ ${r.name} khỏi Yêu thích") {
                FavoriteManager.restoreRestaurant(r.id, oldTime)
            }
        }
    }

    /** Thanh thông báo phía dưới có nút "Hoàn tác". */
    private fun showUndoSnackbar(message: String, onUndo: () -> Unit) {
        Snackbar.make(findViewById<View>(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.bottomNav)
            .setAction("Hoàn tác") { onUndo() }
            .setActionTextColor(Color.parseColor("#FFB3BB"))
            .show()
    }

    private fun sortFavFoods(list: List<Food>): List<Food> = when (favSort) {
        FavSort.PRICE_ASC -> list.sortedBy { FormatUtils.parsePrice(it.price) }
        FavSort.PRICE_DESC -> list.sortedByDescending { FormatUtils.parsePrice(it.price) }
        FavSort.RATING -> list.sortedByDescending { it.rating }
        else -> list.sortedByDescending { FavoriteManager.foodAddedAt(it.id) }
    }

    private fun sortFavRestaurants(list: List<Restaurant>): List<Restaurant> = when (favSort) {
        FavSort.RATING -> list.sortedByDescending { it.rating }
        FavSort.NAME -> list.sortedBy { it.name }
        else -> list.sortedByDescending { FavoriteManager.restaurantAddedAt(it.id) }
    }

    /** Gợi ý món chưa thích: ưu tiên cùng danh mục với món đã thích, còn lại lấy món đánh giá cao. */
    private fun suggestFoods(favFoods: List<Food>): List<Food> {
        val notFav = SampleData.FOODS.filter { !FavoriteManager.isFood(it.id) }
        val favCategories = favFoods.map { it.category }.toSet()
        val sameCategory = notFav.filter { it.category in favCategories }
        val others = notFav.filter { it !in sameCategory }.sortedByDescending { it.rating }
        return (sameCategory + others).take(4)
    }

    private fun showFavoritesContent() {
        contentContainer.removeAllViews()
        cartBadge = null // header của trang Home không còn hiển thị ở đây
        layoutInflater.inflate(R.layout.layout_favorites, contentContainer, true)

        val tvSummary = findViewById<TextView>(R.id.tvFavSummary)
        val btnClear = findViewById<TextView>(R.id.btnFavClear)
        val etSearch = findViewById<EditText>(R.id.etFavSearch)
        val tabFoods = findViewById<View>(R.id.tabFavFoods)
        val tabRestaurants = findViewById<View>(R.id.tabFavRestaurants)
        val tvTabFoods = findViewById<TextView>(R.id.tvTabFavFoods)
        val tvTabRestaurants = findViewById<TextView>(R.id.tvTabFavRestaurants)
        val vTabFoods = findViewById<View>(R.id.vTabFavFoods)
        val vTabRestaurants = findViewById<View>(R.id.vTabFavRestaurants)
        val scrollCats = findViewById<View>(R.id.scrollFavCats)
        val llCats = findViewById<LinearLayout>(R.id.llFavCats)
        val llToolbar = findViewById<View>(R.id.llFavToolbar)
        val btnSort = findViewById<TextView>(R.id.btnFavSort)
        val btnAddAll = findViewById<TextView>(R.id.btnFavAddAll)
        val rvFoods = findViewById<RecyclerView>(R.id.rvFavFoods)
        val rvRestaurants = findViewById<RecyclerView>(R.id.rvFavRestaurants)
        val tvNoResult = findViewById<View>(R.id.tvFavNoResult)
        val llEmpty = findViewById<View>(R.id.llFavEmpty)
        val tvEmptyTitle = findViewById<TextView>(R.id.tvFavEmptyTitle)
        val tvEmptyDesc = findViewById<TextView>(R.id.tvFavEmptyDesc)
        val btnExplore = findViewById<Button>(R.id.btnFavExplore)
        val tvSuggestTitle = findViewById<TextView>(R.id.tvFavSuggestTitle)
        val rvSuggest = findViewById<RecyclerView>(R.id.rvFavSuggest)

        val red = getColor(R.color.red_primary)
        val grey = getColor(R.color.text_grey)

        // ── Adapter ──
        val foodAdapter = FoodAdapter(
            initialFoods = emptyList(),
            onItemClick = { food -> openFoodDetail(food) },
            onFavoriteChanged = { food, isFav, oldTime -> onFoodHeartChanged(food, isFav, oldTime) },
            onAddToCart = { food -> onAddToCart(food) }
        )
        val suggestAdapter = FoodAdapter(
            initialFoods = emptyList(),
            onItemClick = { food -> openFoodDetail(food) },
            onFavoriteChanged = { food, isFav, oldTime -> onFoodHeartChanged(food, isFav, oldTime) },
            onAddToCart = { food -> onAddToCart(food) }
        )
        val restaurantAdapter = RestaurantAdapter(
            initialRestaurants = emptyList(),
            onFavoriteChanged = { r, isFav, oldTime -> onRestaurantHeartChanged(r, isFav, oldTime) },
            onViewMenu = { r -> openRestaurantDetail(r) }
        )
        rvFoods.layoutManager = GridLayoutManager(this, 2)
        rvFoods.adapter = foodAdapter
        rvSuggest.layoutManager = GridLayoutManager(this, 2)
        rvSuggest.adapter = suggestAdapter
        rvRestaurants.layoutManager = LinearLayoutManager(this)
        rvRestaurants.adapter = restaurantAdapter

        val catButtons = mutableListOf<Button>()
        var shownFoods: List<Food> = emptyList()

        fun refresh() {
            val isFoods = favTab == "foods"
            val q = favSearch.trim().lowercase()

            // Đổi tab mà kiểu sắp xếp hiện tại không có ở tab kia thì quay về "Mới thêm"
            val sorts = if (isFoods) foodSorts else restaurantSorts
            if (favSort !in sorts) favSort = FavSort.NEWEST

            val allFoods = FavoriteManager.favoriteFoods()
            val allRestaurants = FavoriteManager.favoriteRestaurants()

            // Lọc + sắp xếp món ăn
            var foods = allFoods
            if (favCategory != "Tất cả") foods = foods.filter { it.category == favCategory }
            if (q.isNotEmpty()) {
                foods = foods.filter {
                    it.name.lowercase().contains(q) || it.restaurant.lowercase().contains(q)
                }
            }
            foods = sortFavFoods(foods)
            shownFoods = foods

            // Lọc + sắp xếp nhà hàng
            var restaurants = allRestaurants
            if (q.isNotEmpty()) {
                restaurants = restaurants.filter {
                    it.name.lowercase().contains(q) || it.desc.lowercase().contains(q)
                }
            }
            restaurants = sortFavRestaurants(restaurants)

            foodAdapter.submit(foods)
            restaurantAdapter.submit(restaurants)

            val total = if (isFoods) allFoods.size else allRestaurants.size
            val shown = if (isFoods) foods.size else restaurants.size

            // Tiêu đề + tab con
            tvSummary.text = "${allFoods.size} món • ${allRestaurants.size} nhà hàng"
            tvTabFoods.text = "Món ăn (${allFoods.size})"
            tvTabRestaurants.text = "Nhà hàng (${allRestaurants.size})"
            tvTabFoods.setTextColor(if (isFoods) red else grey)
            tvTabRestaurants.setTextColor(if (isFoods) grey else red)
            tvTabFoods.setTypeface(null, if (isFoods) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            tvTabRestaurants.setTypeface(null, if (isFoods) android.graphics.Typeface.NORMAL else android.graphics.Typeface.BOLD)
            vTabFoods.setBackgroundColor(if (isFoods) red else Color.TRANSPARENT)
            vTabRestaurants.setBackgroundColor(if (isFoods) Color.TRANSPARENT else red)

            // Hiện/ẩn từng phần
            etSearch.visibility =
                if (allFoods.isEmpty() && allRestaurants.isEmpty()) View.GONE else View.VISIBLE
            btnClear.visibility = if (total > 0) View.VISIBLE else View.GONE
            scrollCats.visibility = if (isFoods && allFoods.isNotEmpty()) View.VISIBLE else View.GONE
            llToolbar.visibility = if (total > 0) View.VISIBLE else View.GONE
            btnAddAll.visibility = if (isFoods) View.VISIBLE else View.GONE
            btnSort.text = "⇅  ${favSort.label}"
            rvFoods.visibility = if (isFoods && foods.isNotEmpty()) View.VISIBLE else View.GONE
            rvRestaurants.visibility = if (!isFoods && restaurants.isNotEmpty()) View.VISIBLE else View.GONE
            tvNoResult.visibility = if (total > 0 && shown == 0) View.VISIBLE else View.GONE
            llEmpty.visibility = if (total == 0) View.VISIBLE else View.GONE
            if (isFoods) {
                tvEmptyTitle.text = "Chưa có món yêu thích"
                tvEmptyDesc.text = "Bấm vào biểu tượng trái tim trên món ăn để lưu lại nhé!"
            } else {
                tvEmptyTitle.text = "Chưa có nhà hàng yêu thích"
                tvEmptyDesc.text = "Bấm vào biểu tượng trái tim trên nhà hàng để lưu lại nhé!"
            }

            // Các nút danh mục
            catButtons.forEachIndexed { i, btn ->
                val isActive = SampleData.CATEGORIES[i] == favCategory
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            // Gợi ý món: chỉ hiện ở tab Món ăn khi không đang tìm/lọc
            val suggestions =
                if (isFoods && q.isEmpty() && favCategory == "Tất cả") suggestFoods(allFoods)
                else emptyList()
            suggestAdapter.submit(suggestions)
            tvSuggestTitle.text =
                if (allFoods.isEmpty()) "Món ngon đề xuất" else "Có thể bạn cũng thích"
            tvSuggestTitle.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
            rvSuggest.visibility = if (suggestions.isEmpty()) View.GONE else View.VISIBLE
        }

        // ── Nút danh mục ──
        SampleData.CATEGORIES.forEach { cat ->
            val btn = Button(this).apply {
                text = cat; textSize = 14f
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, dp(8), 0); layoutParams = lp
                setOnClickListener {
                    favCategory = cat
                    refresh()
                }
            }
            catButtons.add(btn); llCats.addView(btn)
        }

        // ── Tab con ──
        tabFoods.setOnClickListener { favTab = "foods"; refresh() }
        tabRestaurants.setOnClickListener { favTab = "restaurants"; refresh() }

        // ── Tìm kiếm ──
        etSearch.setText(favSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                favSearch = s?.toString().orEmpty()
                refresh()
            }
        })
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        // ── Sắp xếp ──
        btnSort.setOnClickListener {
            val sorts = if (favTab == "foods") foodSorts else restaurantSorts
            AlertDialog.Builder(this)
                .setTitle("Sắp xếp theo")
                .setSingleChoiceItems(
                    sorts.map { it.label }.toTypedArray(),
                    sorts.indexOf(favSort)
                ) { dialog, which ->
                    favSort = sorts[which]
                    dialog.dismiss()
                    refresh()
                }
                .show()
        }

        // ── Thêm tất cả vào giỏ (các món đang hiển thị) ──
        btnAddAll.setOnClickListener {
            if (shownFoods.isEmpty()) {
                showToast("Không có món nào để thêm")
            } else {
                shownFoods.forEach { CartManager.addFood(it) }
                showToast("Đã thêm ${shownFoods.size} món vào giỏ hàng 🛒")
            }
        }

        // ── Xóa hết (có xác nhận, và có thể hoàn tác) ──
        btnClear.setOnClickListener {
            val isFoods = favTab == "foods"
            val count = if (isFoods) FavoriteManager.foodCount() else FavoriteManager.restaurantCount()
            if (count == 0) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Xóa yêu thích")
                .setMessage(
                    if (isFoods) "Bạn muốn xóa tất cả $count món khỏi Yêu thích?"
                    else "Bạn muốn xóa tất cả $count nhà hàng khỏi Yêu thích?"
                )
                .setPositiveButton("Xóa hết") { _, _ ->
                    if (isFoods) {
                        val backup = FavoriteManager.clearFoods()
                        showUndoSnackbar("Đã xóa $count món khỏi Yêu thích") {
                            FavoriteManager.restoreFoods(backup)
                        }
                    } else {
                        val backup = FavoriteManager.clearRestaurants()
                        showUndoSnackbar("Đã xóa $count nhà hàng khỏi Yêu thích") {
                            FavoriteManager.restoreRestaurants(backup)
                        }
                    }
                }
                .setNegativeButton("Không", null)
                .show()
        }

        // ── Trạng thái rỗng: về Home khám phá món ──
        btnExplore.setOnClickListener { selectTab("home") }

        favRefresh = { refresh() }
        refresh()
    }

    // ─── Notifications Content (Trang Thông báo) ───────
    private fun updateNotifBadge() {
        val count = NotificationStore.unreadCount()
        tvNotifBadge.text = if (count > 9) "9+" else count.toString()
        tvNotifBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun showNotificationsContent() {
        contentContainer.removeAllViews()
        cartBadge = null // header của trang Home không còn hiển thị ở đây
        layoutInflater.inflate(R.layout.layout_notifications, contentContainer, true)

        val tvSummary = findViewById<TextView>(R.id.tvNotifSummary)
        val btnMarkAll = findViewById<TextView>(R.id.btnMarkAllRead)
        val btnDeleteAll = findViewById<TextView>(R.id.btnDeleteAll)
        val llFilters = findViewById<LinearLayout>(R.id.llNotifFilters)
        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        val llEmpty = findViewById<View>(R.id.llNotifEmpty)
        val tvEmpty = findViewById<TextView>(R.id.tvNotifEmpty)

        // Tất cả / Đơn hàng / Khuyến mãi / Hệ thống
        val filters: List<Pair<String, NotificationType?>> = listOf(
            "Tất cả" to null,
            "Đơn hàng" to NotificationType.ORDER,
            "Khuyến mãi" to NotificationType.PROMO,
            "Hệ thống" to NotificationType.SYSTEM
        )

        val adapter = NotificationAdapter(
            onClick = { n -> onNotificationClick(n) },
            onLongClick = { n -> confirmDeleteNotification(n) }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.isNestedScrollingEnabled = false
        rv.adapter = adapter

        // Vuốt ngang để xóa
        ItemTouchHelper(
            SwipeToDeleteCallback(
                canSwipe = { pos -> !adapter.isHeader(pos) },
                onDelete = { pos ->
                    val n = adapter.notificationAt(pos)
                    if (n != null) {
                        NotificationStore.delete(n.id)
                        showToast("Đã xóa thông báo")
                    }
                }
            )
        ).attachToRecyclerView(rv)

        val filterButtons = mutableListOf<Button>()

        fun refresh() {
            val list = NotificationStore.getAll(selectedNotifFilter)
            adapter.submit(list)
            llEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            tvEmpty.text = when (selectedNotifFilter) {
                null -> "Bạn chưa có thông báo nào"
                else -> "Chưa có thông báo ${selectedNotifFilter!!.label.lowercase()}"
            }

            // Các nút lọc: hiện số chưa đọc của từng loại
            filterButtons.forEachIndexed { i, btn ->
                val (label, type) = filters[i]
                val unread = NotificationStore.unreadCount(type)
                btn.text = if (unread > 0) "$label ($unread)" else label
                val isActive = type == selectedNotifFilter
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            val totalUnread = NotificationStore.unreadCount(null)
            tvSummary.text =
                if (totalUnread > 0) "Bạn có $totalUnread thông báo chưa đọc"
                else "Bạn đã đọc hết thông báo"

            // "Đọc tất cả" chuyển xám khi mục đang xem không còn thông báo chưa đọc
            val hasUnread = NotificationStore.unreadCount(selectedNotifFilter) > 0
            btnMarkAll.setTextColor(getColor(if (hasUnread) R.color.red_primary else R.color.text_grey))
        }

        filters.forEach { (label, type) ->
            val btn = Button(this).apply {
                text = label; textSize = 13f
                isAllCaps = false
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, dp(8), 0); layoutParams = lp
                setPadding(dp(16), dp(8), dp(16), dp(8))
                setOnClickListener {
                    selectedNotifFilter = type
                    refresh()
                }
            }
            filterButtons.add(btn); llFilters.addView(btn)
        }

        btnMarkAll.setOnClickListener {
            if (NotificationStore.unreadCount(selectedNotifFilter) == 0) {
                showToast("Không có thông báo chưa đọc")
            } else {
                NotificationStore.markAllRead(selectedNotifFilter)
                showToast("Đã đánh dấu tất cả là đã đọc")
            }
        }

        btnDeleteAll.setOnClickListener {
            if (NotificationStore.getAll(selectedNotifFilter).isEmpty()) {
                showToast("Không có thông báo để xóa")
            } else {
                confirmDeleteAllNotifications()
            }
        }

        notifRefresh = { refresh() }
        refresh()
    }

    private fun onNotificationClick(n: AppNotification) {
        NotificationStore.markRead(n.id)

        val orderId = n.orderId
        when {
            // Thông báo đơn hàng -> mở chi tiết đơn (nếu đơn còn tồn tại)
            n.type == NotificationType.ORDER && orderId != null &&
                    OrderManager.findById(orderId) != null -> openOrderDetail(orderId)
            n.type == NotificationType.PROMO -> showPromoDialog(n)
            else -> showNotificationDialog(n)
        }
    }

    private fun showPromoDialog(n: AppNotification) {
        val code = n.promoCode
        val builder = AlertDialog.Builder(this)
            .setTitle(n.title)
            .setMessage(n.message)
            .setNegativeButton("Đóng", null)

        if (code != null) {
            builder.setPositiveButton("Sao chép mã") { _, _ ->
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("promo_code", code))
                showToast("Đã sao chép mã $code")
            }
            builder.setNeutralButton("Đặt món ngay") { _, _ -> selectTab("home") }
        } else {
            builder.setPositiveButton("Đặt món ngay") { _, _ -> selectTab("home") }
        }
        builder.show()
    }

    private fun showNotificationDialog(n: AppNotification) {
        AlertDialog.Builder(this)
            .setTitle(n.title)
            .setMessage(n.message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun confirmDeleteNotification(n: AppNotification) {
        AlertDialog.Builder(this)
            .setTitle("Xóa thông báo")
            .setMessage("Bạn muốn xóa thông báo này?")
            .setPositiveButton("Xóa") { _, _ ->
                NotificationStore.delete(n.id)
                showToast("Đã xóa thông báo")
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun confirmDeleteAllNotifications() {
        val filter = selectedNotifFilter
        val msg =
            if (filter == null) "Bạn muốn xóa tất cả thông báo?"
            else "Bạn muốn xóa tất cả thông báo ${filter.label.lowercase()}?"

        AlertDialog.Builder(this)
            .setTitle("Xóa thông báo")
            .setMessage(msg)
            .setPositiveButton("Xóa hết") { _, _ ->
                NotificationStore.deleteAll(filter)
                showToast("Đã xóa thông báo")
            }
            .setNegativeButton("Không", null)
            .show()
    }

    // ─── Placeholder tabs ──────────────────────────────
    private fun showPlaceholder(icon: String, msg: String) {
        contentContainer.removeAllViews()
        cartBadge = null
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dp(40), dp(60), dp(40), dp(40))
        }
        val iconView = TextView(this).apply { text = icon; textSize = 48f; gravity = Gravity.CENTER }
        val msgView  = TextView(this).apply {
            text = msg; textSize = 15f; gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_grey))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(12); layoutParams = lp
        }
        wrapper.addView(iconView); wrapper.addView(msgView)
        contentContainer.addView(wrapper)
    }

    // ─── Toast ─────────────────────────────────────────
    private fun showToast(msg: String) {
        tvToast.text = msg
        tvToast.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ tvToast.visibility = View.GONE }, 2500)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
