package com.example.foodhub.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.ui.account.AccountActivity
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.ui.product.ProductDetailActivity
import com.example.foodhub.ui.restaurant.RestaurantDetailActivity
import com.example.foodhub.utils.SampleData

class MainActivity : AppCompatActivity() {

    internal lateinit var contentContainer: LinearLayout
    private lateinit var tvToast: TextView
    private val handler = Handler(Looper.getMainLooper())

    // Tab label references
    private lateinit var labelHome: TextView
    private lateinit var labelOrders: TextView
    private lateinit var labelLikes: TextView
    private lateinit var labelNotifications: TextView
    private lateinit var labelAccount: TextView

    // Tab icon references
    private lateinit var iconHome: ImageView
    private lateinit var iconOrders: ImageView
    private lateinit var iconLikes: ImageView
    private lateinit var iconNotifications: ImageView
    private lateinit var iconAccount: ImageView

    // Badge giỏ hàng
    internal var cartBadge: TextView? = null

    // Badge số thông báo chưa đọc trên tab chuông & tim
    private lateinit var tvNotifBadge: TextView
    private lateinit var tvFavBadge: TextView

    // Adapter của trang Home
    internal var homeFoodAdapter: FoodAdapter? = null
    internal var homeRestaurantAdapter: RestaurantAdapter? = null

    // Sub-views chuyên trách cho từng Tab
    private lateinit var favoritesView: MainActivityFavoritesView
    private lateinit var notificationsView: MainActivityNotificationsView

    // Bộ lọc trạng thái đơn hàng
    private var selectedOrderFilter: OrderStatus? = null

    // Tab đang hiển thị
    private var currentTab: String = "home"
    private var isFirstLaunch: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nạp danh sách món ăn, yêu thích và dữ liệu đã lưu
        try {
            SampleData.reloadFromDb(this)
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
        iconHome           = findViewById(R.id.iconHome)
        iconOrders         = findViewById(R.id.iconOrders)
        iconLikes          = findViewById(R.id.iconLikes)
        iconNotifications  = findViewById(R.id.iconNotifications)
        iconAccount        = findViewById(R.id.iconAccount)
        tvNotifBadge       = findViewById(R.id.tvNotifBadge)
        tvFavBadge         = findViewById(R.id.tvFavBadge)

        favoritesView = MainActivityFavoritesView(this)
        notificationsView = MainActivityNotificationsView(this)

        // Tab click listeners
        findViewById<LinearLayout>(R.id.tabHome).setOnClickListener { selectTab("home") }
        findViewById<LinearLayout>(R.id.tabOrders).setOnClickListener { selectTab("orders") }
        findViewById<LinearLayout>(R.id.tabLikes).setOnClickListener { selectTab("likes") }
        findViewById<LinearLayout>(R.id.tabNotifications).setOnClickListener { selectTab("notifications") }
        findViewById<LinearLayout>(R.id.tabAccount).setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            overridePendingTransition(0, 0)
        }

        val selectedTab = intent.getStringExtra("selected_tab")
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
        SampleData.reloadFromDb(this)

        CartManager.ensureLoaded(this)
        OrderManager.ensureLoaded(this)
        NotificationStore.ensureLoaded(this)
        refreshCartBadge()

        if (isFirstLaunch) {
            isFirstLaunch = false
        } else {
            if (currentTab == "home") {
                showHomeContent()
            }
        }
        if (currentTab == "home") {
            homeFoodAdapter?.submit(SampleData.FOODS)
            homeRestaurantAdapter?.submit(SampleData.RESTAURANTS)
        }

        SampleData.onFoodsChanged = {
            runOnUiThread {
                if (currentTab == "home") {
                    showHomeContent()
                }
            }
        }

        if (currentTab == "orders") {
            showOrdersContent()
        }
        OrderManager.onChanged = {
            if (currentTab == "orders") showOrdersContent()
        }

        updateNotifBadge()
        if (currentTab == "notifications") notificationsView.onRefreshRequested?.invoke()
        NotificationStore.onChanged = {
            updateNotifBadge()
            if (currentTab == "notifications") notificationsView.onRefreshRequested?.invoke()
        }
        NotificationStore.onNewNotification = { n ->
            showToast("🔔 ${n.title}")
        }

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
        SampleData.onFoodsChanged = null
    }

    internal fun selectTab(tab: String) {
        currentTab = tab
        val orangeColor = getColor(R.color.red_primary)
        val greyColor   = Color.parseColor("#555555")

        listOf(labelHome, labelOrders, labelLikes, labelNotifications, labelAccount).forEach {
            it.setTextColor(greyColor)
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        iconHome.setImageResource(R.drawable.ic_nav_home_outline)
        iconOrders.setImageResource(R.drawable.ic_nav_orders_outline)
        iconLikes.setImageResource(R.drawable.ic_nav_likes_outline)
        iconNotifications.setImageResource(R.drawable.ic_nav_notif_outline)
        iconAccount.setImageResource(R.drawable.ic_nav_account_outline)

        when (tab) {
            "home" -> {
                labelHome.setTextColor(orangeColor)
                labelHome.setTypeface(null, android.graphics.Typeface.BOLD)
                iconHome.setImageResource(R.drawable.ic_nav_home_filled)
                SampleData.reloadFromDb(this)
                showHomeContent()
            }
            "orders" -> {
                labelOrders.setTextColor(orangeColor)
                labelOrders.setTypeface(null, android.graphics.Typeface.BOLD)
                iconOrders.setImageResource(R.drawable.ic_nav_orders_filled)
                showOrdersContent()
            }
            "likes" -> {
                labelLikes.setTextColor(orangeColor)
                labelLikes.setTypeface(null, android.graphics.Typeface.BOLD)
                iconLikes.setImageResource(R.drawable.ic_nav_likes_filled)
                favoritesView.showFavoritesContent()
            }
            "notifications" -> {
                labelNotifications.setTextColor(orangeColor)
                labelNotifications.setTypeface(null, android.graphics.Typeface.BOLD)
                iconNotifications.setImageResource(R.drawable.ic_nav_notif_filled)
                notificationsView.showNotificationsContent()
            }
            "account" -> {
                labelAccount.setTextColor(orangeColor)
                labelAccount.setTypeface(null, android.graphics.Typeface.BOLD)
                iconAccount.setImageResource(R.drawable.ic_nav_account_filled)
            }
        }
    }

    private fun showHomeContent() {
        MainActivityHomeView.showHomeContent(this)
    }

    private fun showOrdersContent() {
        MainActivityOrdersView.showOrdersContent(this, selectedOrderFilter) { newFilter ->
            selectedOrderFilter = newFilter
        }
    }

    internal fun onAddToCart(food: Food) {
        CartManager.addFood(food)
        refreshCartBadge()
        showToast("Đã thêm ${food.name} vào giỏ hàng 🛒")
    }

    internal fun refreshCartBadge() {
        val badge = cartBadge ?: return
        val count = CartManager.totalQuantity()
        badge.text = count.toString()
        badge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    internal fun addSectionHeader(title: String, actionText: String, onAction: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), dp(12))
            layoutParams = lp
        }
        val tvTitle = TextView(this).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.text_dark))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvAction = TextView(this).apply {
            text = actionText
            textSize = 13f
            setTextColor(getColor(R.color.red_primary))
            setOnClickListener { onAction() }
        }
        row.addView(tvTitle)
        row.addView(tvAction)
        contentContainer.addView(row)
    }

    internal fun openOrderDetail(orderId: String) {
        val intent = Intent(this, OrderDetailActivity::class.java)
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_ID, orderId)
        startActivity(intent)
    }

    private fun updateFavBadge() {
        val count = FavoriteManager.totalCount()
        tvFavBadge.text = if (count > 9) "9+" else count.toString()
        tvFavBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private fun refreshAfterFavoriteChange() {
        when (currentTab) {
            "home" -> {
                homeFoodAdapter?.notifyDataSetChanged()
                homeRestaurantAdapter?.notifyDataSetChanged()
            }
            "likes" -> {
                if (favoritesView.onRefreshRequested != null) {
                    favoritesView.onRefreshRequested?.invoke()
                } else {
                    favoritesView.showFavoritesContent()
                }
            }
        }
    }

    internal fun openFoodDetail(food: Food) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra(ProductDetailActivity.EXTRA_FOOD_ID, food.id)
        startActivity(intent)
    }

    internal fun openRestaurantDetail(restaurant: Restaurant) {
        val intent = Intent(this, RestaurantDetailActivity::class.java)
        intent.putExtra(RestaurantDetailActivity.EXTRA_RESTAURANT_ID, restaurant.id)
        startActivity(intent)
    }

    internal fun onFoodHeartChanged(food: Food, isFav: Boolean, oldTime: Long) {
        if (isFav) {
            showToast("Đã thêm ${food.name} vào Yêu thích ❤️")
        } else {
            showUndoSnackbar("Đã bỏ ${food.name} khỏi Yêu thích") {
                FavoriteManager.restoreFood(food.id, oldTime)
            }
        }
    }

    internal fun onRestaurantHeartChanged(r: Restaurant, isFav: Boolean, oldTime: Long) {
        if (isFav) {
            showToast("Đã thêm ${r.name} vào Yêu thích ❤️")
        } else {
            showUndoSnackbar("Đã bỏ ${r.name} khỏi Yêu thích") {
                FavoriteManager.restoreRestaurant(r.id, oldTime)
            }
        }
    }

    internal fun showUndoSnackbar(message: String, onUndo: () -> Unit) {
        Snackbar.make(findViewById<View>(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.bottomNav)
            .setAction("Hoàn tác") { onUndo() }
            .setActionTextColor(Color.parseColor("#FFB3BB"))
            .show()
    }

    private fun updateNotifBadge() {
        val count = NotificationStore.unreadCount()
        tvNotifBadge.text = if (count > 9) "9+" else count.toString()
        tvNotifBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    internal fun showToast(msg: String) {
        tvToast.text = msg
        tvToast.visibility = View.VISIBLE
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ tvToast.visibility = View.GONE }, 2500)
    }

    internal fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
