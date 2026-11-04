package com.example.foodhub.ui.admin

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.SampleData

class AdminFoodActivity : AppCompatActivity() {

    internal lateinit var dbHelper: DatabaseHelper
    private lateinit var llFoodContainer: LinearLayout
    private lateinit var llCategoryChips: LinearLayout
    private lateinit var tvFoodCount: TextView
    private lateinit var etSearchFood: EditText

    private var allFoods: List<Food> = emptyList()
    private var allCategories: List<String> = emptyList()
    private var selectedCategory: String = "Tất cả"
    private var searchQuery: String = ""

    private var pendingImageCallback: ((String) -> Unit)? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = AdminFoodDialogHelper.saveImageToInternalStorage(this, uri)
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
        setContentView(R.layout.activity_admin_food)

        dbHelper = DatabaseHelper.getInstance(this)

        llFoodContainer = findViewById(R.id.llFoodContainer)
        llCategoryChips = findViewById(R.id.llCategoryChips)
        tvFoodCount     = findViewById(R.id.tvFoodCount)
        etSearchFood    = findViewById(R.id.etSearchFood)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddFood).setOnClickListener {
            AdminFoodDialogHelper.showAddFoodDialog(this, allCategories) {
                loadData()
            }
        }

        etSearchFood.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            renderFoodList()
        }

        loadData()
    }

    private fun loadData() {
        allCategories = dbHelper.getAllCategories()
        allFoods = dbHelper.getAllFoods()
        renderCategoryChips()
        renderFoodList()
    }

    private fun renderCategoryChips() {
        llCategoryChips.removeAllViews()
        allCategories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val btn = TextView(this).apply {
                text = cat
                textSize = 13f
                gravity = Gravity.CENTER
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) R.drawable.bg_cat_active else R.drawable.bg_chip_normal)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#475569"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size).coerceAtMost(dpToPx(34))
                )
                lp.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = lp
                setPadding(dpToPx(14), 0, dpToPx(14), 0)
                setOnClickListener {
                    selectedCategory = cat
                    renderCategoryChips()
                    renderFoodList()
                }
            }
            llCategoryChips.addView(btn)
        }
    }

    private fun renderFoodList() {
        llFoodContainer.removeAllViews()

        var filtered = allFoods
        if (selectedCategory != "Tất cả") {
            filtered = filtered.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                com.example.foodhub.utils.FormatUtils.matchesSearch(it.name, searchQuery) ||
                com.example.foodhub.utils.FormatUtils.matchesSearch(it.restaurant, searchQuery)
            }
        }

        tvFoodCount.text = "Tổng số: ${filtered.size} món ăn"

        if (filtered.isEmpty()) {
            val emptyCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(48), dpToPx(24), dpToPx(48))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = lp
            }

            val tvEmptyIcon = TextView(this).apply {
                text = "🍽️"
                textSize = 40f
                gravity = Gravity.CENTER
            }
            val tvEmpty = TextView(this).apply {
                text = "Không tìm thấy món ăn nào phù hợp\nBấm '+ Thêm món' để tạo món mới"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, dpToPx(8), 0, 0)
            }
            emptyCard.addView(tvEmptyIcon)
            emptyCard.addView(tvEmpty)
            llFoodContainer.addView(emptyCard)
            return
        }

        filtered.forEach { food ->
            val itemView = layoutInflater.inflate(R.layout.item_admin_food, llFoodContainer, false)

            val ivFoodImage = itemView.findViewById<ImageView>(R.id.ivFoodImage)
            val tvEmojiFallback = itemView.findViewById<TextView>(R.id.tvFoodEmojiFallback)

            // Bind image (custom uploaded photo or drawable or emoji)
            SampleData.loadFoodImage(ivFoodImage, tvEmojiFallback, food)

            // Bind food texts
            itemView.findViewById<TextView>(R.id.tvFoodName).text = food.name
            itemView.findViewById<TextView>(R.id.tvPrice).text = food.price
            itemView.findViewById<TextView>(R.id.tvCategory).text = food.category
            itemView.findViewById<TextView>(R.id.tvRestaurant).text = food.restaurant
            itemView.findViewById<TextView>(R.id.tvRating).text = food.rating.toString()
            itemView.findViewById<TextView>(R.id.tvReviews).text = "(${food.reviews})"

            // Bind status badge & switch (Còn hàng / Tạm hết)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val switchAvailable = itemView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchAvailable)

            fun applyStatusBadge(isAvail: Boolean) {
                if (isAvail) {
                    tvStatusBadge.text = "● Còn hàng"
                    tvStatusBadge.setTextColor(Color.parseColor("#16A34A"))
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_green)
                    itemView.alpha = 1.0f
                } else {
                    tvStatusBadge.text = "● Tạm hết"
                    tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_status_red)
                    itemView.alpha = 0.65f
                }
            }

            switchAvailable.setOnCheckedChangeListener(null)
            switchAvailable.isChecked = food.isAvailable
            applyStatusBadge(food.isAvailable)

            switchAvailable.setOnCheckedChangeListener { _, isChecked ->
                val ok = dbHelper.updateFoodAvailability(food.id, isChecked)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    applyStatusBadge(isChecked)
                    val statusText = if (isChecked) "Còn hàng" else "Tạm hết"
                    Toast.makeText(this, "${food.name}: Đã chuyển sang '$statusText'", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this, "Không thể cập nhật trạng thái món ăn", Toast.LENGTH_SHORT).show()
                    switchAvailable.isChecked = !isChecked
                }
            }

            // Action buttons
            itemView.findViewById<View>(R.id.btnEdit).setOnClickListener {
                AdminFoodDialogHelper.showEditFoodDialog(this, food, allCategories) {
                    loadData()
                }
            }
            itemView.findViewById<View>(R.id.btnDelete).setOnClickListener {
                AdminFoodDialogHelper.confirmDeleteFood(this, food) {
                    loadData()
                }
            }

            llFoodContainer.addView(itemView)
        }
    }

    internal fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
