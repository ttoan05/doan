package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.SampleData
import java.io.File

class AdminCategoryActivity : AppCompatActivity() {

    companion object {
        private const val TAB_CATEGORY = 0
        private const val TAB_RESTAURANT = 1
    }

    internal lateinit var dbHelper: DatabaseHelper
    private lateinit var tabCategory: TextView
    private lateinit var tabRestaurant: TextView
    private lateinit var tvCountSummary: TextView
    private lateinit var btnAddItem: TextView
    private lateinit var llCategoryContainer: LinearLayout
    private lateinit var llRestaurantContainer: LinearLayout

    private var currentTab = TAB_CATEGORY
    private var allFoods: List<Food> = emptyList()

    private var pendingImageCallback: ((String) -> Unit)? = null
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = AdminCategoryDialogHelper.saveImageToInternalStorage(this, uri)
            if (savedPath != null) {
                pendingImageCallback?.invoke(savedPath)
            } else {
                Toast.makeText(this, "Không thể lưu ảnh từ thiết bị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun pickImage(callback: (String) -> Unit) {
        pendingImageCallback = callback
        pickImageLauncher.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_category)

        dbHelper = DatabaseHelper.getInstance(this)

        tabCategory          = findViewById(R.id.tabCategory)
        tabRestaurant        = findViewById(R.id.tabRestaurant)
        tvCountSummary       = findViewById(R.id.tvCountSummary)
        btnAddItem           = findViewById(R.id.btnAddItem)
        llCategoryContainer  = findViewById(R.id.llCategoryContainer)
        llRestaurantContainer = findViewById(R.id.llRestaurantContainer)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        tabCategory.setOnClickListener { switchTab(TAB_CATEGORY) }
        tabRestaurant.setOnClickListener { switchTab(TAB_RESTAURANT) }

        btnAddItem.setOnClickListener {
            if (currentTab == TAB_CATEGORY) {
                AdminCategoryDialogHelper.showAddCategoryDialog(this) {
                    allFoods = dbHelper.getAllFoods()
                    loadCategories()
                }
            } else {
                AdminCategoryDialogHelper.showAddRestaurantDialog(this) {
                    allFoods = dbHelper.getAllFoods()
                    loadRestaurants()
                }
            }
        }

        switchTab(TAB_CATEGORY)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun switchTab(tab: Int) {
        currentTab = tab
        allFoods = dbHelper.getAllFoods()

        if (tab == TAB_CATEGORY) {
            tabCategory.setBackgroundResource(R.drawable.bg_tab_segment_selected)
            tabCategory.setTextColor(Color.parseColor("#0F172A"))
            tabCategory.setTypeface(null, Typeface.BOLD)
            tabCategory.elevation = dpToPx(2).toFloat()

            tabRestaurant.setBackgroundColor(Color.TRANSPARENT)
            tabRestaurant.setTextColor(Color.parseColor("#64748B"))
            tabRestaurant.setTypeface(null, Typeface.NORMAL)
            tabRestaurant.elevation = 0f

            btnAddItem.text = "+ Thêm danh mục"
            llCategoryContainer.visibility = View.VISIBLE
            llRestaurantContainer.visibility = View.GONE

            loadCategories()
        } else {
            tabRestaurant.setBackgroundResource(R.drawable.bg_tab_segment_selected)
            tabRestaurant.setTextColor(Color.parseColor("#0F172A"))
            tabRestaurant.setTypeface(null, Typeface.BOLD)
            tabRestaurant.elevation = dpToPx(2).toFloat()

            tabCategory.setBackgroundColor(Color.TRANSPARENT)
            tabCategory.setTextColor(Color.parseColor("#64748B"))
            tabCategory.setTypeface(null, Typeface.NORMAL)
            tabCategory.elevation = 0f

            btnAddItem.text = "+ Thêm nhà hàng"
            llCategoryContainer.visibility = View.GONE
            llRestaurantContainer.visibility = View.VISIBLE

            loadRestaurants()
        }
    }

    // =========================================================================
    // 1. QUẢN LÝ DANH MỤC MÓN ĂN (CATEGORIES)
    // =========================================================================

    private fun loadCategories() {
        llCategoryContainer.removeAllViews()

        val categories = dbHelper.getAllCategories()
        val categoryObjects = dbHelper.getAllCategoryObjects()
        val categoryMap = categoryObjects.associateBy { it.name.lowercase() }
        tvCountSummary.text = "Tổng cộng: ${categories.size} danh mục món ăn"

        categories.forEach { cat ->
            val isDefaultAll = cat.equals("Tất cả", ignoreCase = true)
            val count = if (isDefaultAll) allFoods.size else allFoods.count { it.category.equals(cat, ignoreCase = true) }

            val itemView = layoutInflater.inflate(R.layout.item_admin_category, llCategoryContainer, false)

            val ivThumb = itemView.findViewById<ImageView>(R.id.ivCategoryThumb)
            val tvName = itemView.findViewById<TextView>(R.id.tvCategoryName)
            val tvCount = itemView.findViewById<TextView>(R.id.tvCategoryFoodCount)
            val tvBadge = itemView.findViewById<TextView>(R.id.tvDefaultBadge)
            val llActions = itemView.findViewById<View>(R.id.llActions)
            val btnEdit = itemView.findViewById<View>(R.id.btnEditCategory)
            val btnDelete = itemView.findViewById<View>(R.id.btnDeleteCategory)

            // Hiển thị ảnh danh mục (ưu tiên ảnh admin đã tự upload từ máy)
            val uploadedImageUri = categoryMap[cat.lowercase()]?.imageUri
            var imageLoaded = false
            if (!uploadedImageUri.isNullOrEmpty()) {
                val file = File(uploadedImageUri)
                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            ivThumb.setImageBitmap(bitmap)
                            imageLoaded = true
                        }
                    } catch (_: Exception) {}
                }
            }
            if (!imageLoaded) {
                ivThumb.setImageResource(resolveCategoryThumbnail(cat))
            }

            tvName.text = cat
            tvCount.text = if (isDefaultAll) "Toàn bộ món ăn ($count món)" else "$count món trong thực đơn"

            if (isDefaultAll) {
                tvBadge.visibility = View.VISIBLE
                llActions.visibility = View.GONE
            } else {
                tvBadge.visibility = View.GONE
                llActions.visibility = View.VISIBLE

                btnEdit.setOnClickListener {
                    AdminCategoryDialogHelper.showEditCategoryDialog(this, cat) {
                        allFoods = dbHelper.getAllFoods()
                        loadCategories()
                    }
                }
                btnDelete.setOnClickListener {
                    AdminCategoryDialogHelper.confirmDeleteCategory(this, cat, count) {
                        allFoods = dbHelper.getAllFoods()
                        loadCategories()
                    }
                }
            }

            llCategoryContainer.addView(itemView)
        }
    }

    /** Ánh xạ ảnh món ăn thực tế chuẩn đẹp cho từng danh mục mặc định */
    internal fun resolveCategoryThumbnail(category: String): Int {
        val lower = category.lowercase()
        return when {
            lower.contains("cơm") -> R.drawable.img_com_tam
            lower.contains("phở") -> R.drawable.img_pho_bo
            lower.contains("bánh mì") -> R.drawable.img_banh_mi
            lower.contains("gà") -> R.drawable.img_ga_ran
            lower.contains("mì") -> R.drawable.img_mi_quang
            lower.contains("uống") || lower.contains("nước") || lower.contains("trà") -> R.drawable.img_nuoc_chanh
            else -> {
                val sampleFood = allFoods.firstOrNull { it.category.equals(category, ignoreCase = true) }
                if (sampleFood != null && sampleFood.imageRes != 0) {
                    sampleFood.imageRes
                } else {
                    R.drawable.img_com_tam
                }
            }
        }
    }

    // =========================================================================
    // 2. QUẢN LÝ NHÀ HÀNG / QUÁN ĂN (RESTAURANTS)
    // =========================================================================

    private fun loadRestaurants() {
        llRestaurantContainer.removeAllViews()

        val restaurants = dbHelper.getAllRestaurants()
        tvCountSummary.text = "Tổng cộng: ${restaurants.size} nhà hàng / quán ăn đang hoạt động"

        if (restaurants.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Chưa có nhà hàng nào. Bấm '+ Thêm nhà hàng' để tạo quán mới."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                setPadding(dpToPx(24), dpToPx(48), dpToPx(24), dpToPx(24))
            }
            llRestaurantContainer.addView(emptyTv)
            return
        }

        restaurants.forEach { r ->
            val itemView = layoutInflater.inflate(R.layout.item_admin_restaurant, llRestaurantContainer, false)

            val flBanner = itemView.findViewById<FrameLayout>(R.id.flRestaurantBanner)
            val ivBanner = itemView.findViewById<ImageView>(R.id.ivRestaurantBanner)
            val tvBadge = itemView.findViewById<TextView>(R.id.tvRestaurantBadge)
            val tvRating = itemView.findViewById<TextView>(R.id.tvRestaurantRating)
            val tvName = itemView.findViewById<TextView>(R.id.tvRestaurantName)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvRestaurantDesc)
            val tvTime = itemView.findViewById<TextView>(R.id.tvRestaurantTime)
            val tvDist = itemView.findViewById<TextView>(R.id.tvRestaurantDist)
            val tvFoodCount = itemView.findViewById<TextView>(R.id.tvRestaurantFoodCount)
            val btnEdit = itemView.findViewById<View>(R.id.btnEditRestaurant)
            val btnDelete = itemView.findViewById<View>(R.id.btnDeleteRestaurant)

            // Hiển thị ảnh banner thực tế
            SampleData.loadRestaurantImage(ivBanner, flBanner, r)

            tvName.text = r.name
            tvDesc.text = r.desc
            tvRating.text = r.rating.toString()
            tvTime.text = "🕒 ${r.time}"
            tvDist.text = "📍 ${r.dist}"

            if (r.badge.isNotEmpty()) {
                tvBadge.visibility = View.VISIBLE
                tvBadge.text = r.badge
            } else {
                tvBadge.visibility = View.GONE
            }

            val count = allFoods.count { it.restaurant.trim().equals(r.name.trim(), ignoreCase = true) }
            tvFoodCount.text = "$count món bán"

            btnEdit.setOnClickListener {
                AdminCategoryDialogHelper.showEditRestaurantDialog(this, r) {
                    allFoods = dbHelper.getAllFoods()
                    loadRestaurants()
                }
            }
            btnDelete.setOnClickListener {
                AdminCategoryDialogHelper.confirmDeleteRestaurant(this, r, count) {
                    allFoods = dbHelper.getAllFoods()
                    loadRestaurants()
                }
            }

            llRestaurantContainer.addView(itemView)
        }
    }
}
