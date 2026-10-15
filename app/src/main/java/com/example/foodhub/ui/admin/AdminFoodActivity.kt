package com.example.foodhub.ui.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.SampleData

class AdminFoodActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llFoodContainer: LinearLayout
    private lateinit var llCategoryChips: LinearLayout
    private lateinit var tvFoodCount: TextView
    private lateinit var etSearchFood: EditText

    private var allFoods: List<Food> = emptyList()
    private var allCategories: List<String> = emptyList()
    private var selectedCategory: String = "Tất cả"
    private var searchQuery: String = ""

    private val commonEmojis = listOf("🍲", "🍗", "🥖", "🍛", "🍝", "🥤", "🍜", "🍕", "🍔", "🍣", "🍰", "🧋")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_food)

        dbHelper = DatabaseHelper.getInstance(this)

        llFoodContainer = findViewById(R.id.llFoodContainer)
        llCategoryChips = findViewById(R.id.llCategoryChips)
        tvFoodCount     = findViewById(R.id.tvFoodCount)
        etSearchFood    = findViewById(R.id.etSearchFood)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddFood).setOnClickListener { showAddFoodDialog() }

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
            val btn = Button(this).apply {
                text = cat
                textSize = 12f
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#4B5563"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 16, 0)
                layoutParams = lp
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
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) || it.restaurant.lowercase().contains(q)
            }
        }

        tvFoodCount.text = "Tổng số: ${filtered.size} món ăn"

        if (filtered.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Không tìm thấy món ăn nào phù hợp.\nBấm '➕ Thêm món' để tạo món mới."
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 60, 0, 60)
            }
            llFoodContainer.addView(tvEmpty)
            return
        }

        filtered.forEach { food ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(20, 18, 20, 18)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            // Top row: Emoji/Ảnh, Tên món & Quán, Giá tiền
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // Food Emoji / Image
            val tvEmoji = TextView(this).apply {
                text = food.emoji
                textSize = 34f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(60, 60)
            }
            topRow.addView(tvEmoji)

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(16, 0, 12, 0)
            }

            val tvName = TextView(this).apply {
                text = food.name
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
            }

            val tvRestaurant = TextView(this).apply {
                text = "🏠 ${food.restaurant}  •  🏷️ ${food.category}"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 2, 0, 0)
            }

            val tvRating = TextView(this).apply {
                text = "⭐ ${food.rating} (${food.reviews} đánh giá)"
                textSize = 11f
                setTextColor(Color.parseColor("#F59E0B"))
                setPadding(0, 2, 0, 0)
            }

            infoCol.addView(tvName)
            infoCol.addView(tvRestaurant)
            infoCol.addView(tvRating)
            topRow.addView(infoCol)

            val tvPrice = TextView(this).apply {
                text = food.price
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#E63946"))
            }
            topRow.addView(tvPrice)

            card.addView(topRow)

            // Divider
            val div = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 12, 0, 12) }
                setBackgroundColor(Color.parseColor("#F3F4F6"))
            }
            card.addView(div)

            // Action row: Sửa món ăn / Xóa món ăn
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val btnEdit = Button(this).apply {
                text = "✏️ Sửa"
                textSize = 12f
                setBackgroundResource(R.drawable.bg_btn_blue)
                setTextColor(Color.WHITE)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 12, 0)
                layoutParams = lp
                setOnClickListener { showEditFoodDialog(food) }
            }

            val btnDelete = Button(this).apply {
                text = "🗑️ Xóa"
                textSize = 12f
                setBackgroundResource(R.drawable.bg_btn_red_outline)
                setTextColor(Color.parseColor("#E63946"))
                setOnClickListener { confirmDeleteFood(food) }
            }

            actionRow.addView(btnEdit)
            actionRow.addView(btnDelete)
            card.addView(actionRow)

            llFoodContainer.addView(card)
        }
    }

    private fun showAddFoodDialog() {
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }
        scrollView.addView(container)

        val etName = createField(container, "Tên món ăn (vd: Bún chả Hà Nội)", "")
        val etRestaurant = createField(container, "Tên nhà hàng (vd: Quán Phở Tuyệt)", "Quán Phở Tuyệt")
        val etPrice = createField(container, "Giá tiền (vd: 45.000đ)", "45.000đ")

        // Chọn Danh mục
        val tvCatLabel = TextView(this).apply {
            text = "Danh mục món:"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, 10, 0, 6)
        }
        container.addView(tvCatLabel)

        val validCats = allCategories.filter { it != "Tất cả" }
        val spCategory = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AdminFoodActivity,
                android.R.layout.simple_spinner_dropdown_item,
                validCats
            )
        }
        container.addView(spCategory)

        // Emoji biểu tượng
        val tvEmojiLabel = TextView(this).apply {
            text = "Biểu tượng Emoji món ăn:"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, 14, 0, 6)
        }
        container.addView(tvEmojiLabel)

        val etEmoji = createField(container, "Emoji (vd: 🍲)", "🍲")

        // Quick emoji chips
        val emojiRow = HorizontalScrollView(this).apply {
            val row = LinearLayout(this@AdminFoodActivity).apply { orientation = LinearLayout.HORIZONTAL }
            commonEmojis.forEach { em ->
                val btnEm = Button(this@AdminFoodActivity).apply {
                    text = em
                    textSize = 16f
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener { etEmoji.setText(em) }
                }
                row.addView(btnEm)
            }
            addView(row)
        }
        container.addView(emojiRow)

        AlertDialog.Builder(this)
            .setTitle("➕ Thêm món ăn mới")
            .setView(scrollView)
            .setPositiveButton("Thêm món") { _, _ ->
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = if (validCats.isNotEmpty()) spCategory.selectedItem.toString() else "Cơm"
                val emoji = etEmoji.text.toString().trim().ifEmpty { "🍲" }

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin món", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!price.endsWith("đ")) price += "đ"

                val newId = dbHelper.getNextFoodId()
                val newFood = Food(
                    id = newId,
                    name = name,
                    restaurant = restaurant,
                    rating = 4.8,
                    reviews = 20,
                    price = price,
                    category = category,
                    emoji = emoji,
                    bgColorStart = Color.parseColor("#F4A261"),
                    bgColorEnd = Color.parseColor("#E76F51"),
                    imageRes = 0
                )

                dbHelper.insertFood(newFood)
                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã thêm món '$name' thành công! 🎉", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditFoodDialog(food: Food) {
        val scrollView = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }
        scrollView.addView(container)

        val etName = createField(container, "Tên món ăn", food.name)
        val etRestaurant = createField(container, "Tên nhà hàng", food.restaurant)
        val etPrice = createField(container, "Giá tiền", food.price)

        val tvCatLabel = TextView(this).apply {
            text = "Danh mục món:"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, 10, 0, 6)
        }
        container.addView(tvCatLabel)

        val validCats = allCategories.filter { it != "Tất cả" }
        val spCategory = Spinner(this).apply {
            val adapter = ArrayAdapter(
                this@AdminFoodActivity,
                android.R.layout.simple_spinner_dropdown_item,
                validCats
            )
            this.adapter = adapter
            val currentIdx = validCats.indexOf(food.category)
            if (currentIdx >= 0) setSelection(currentIdx)
        }
        container.addView(spCategory)

        val tvEmojiLabel = TextView(this).apply {
            text = "Biểu tượng Emoji món ăn:"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, 14, 0, 6)
        }
        container.addView(tvEmojiLabel)

        val etEmoji = createField(container, "Emoji", food.emoji)

        // Quick emoji chips
        val emojiRow = HorizontalScrollView(this).apply {
            val row = LinearLayout(this@AdminFoodActivity).apply { orientation = LinearLayout.HORIZONTAL }
            commonEmojis.forEach { em ->
                val btnEm = Button(this@AdminFoodActivity).apply {
                    text = em
                    textSize = 16f
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener { etEmoji.setText(em) }
                }
                row.addView(btnEm)
            }
            addView(row)
        }
        container.addView(emojiRow)

        AlertDialog.Builder(this)
            .setTitle("✏️ Sửa món: ${food.name}")
            .setView(scrollView)
            .setPositiveButton("Lưu thay đổi") { _, _ ->
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = if (validCats.isNotEmpty()) spCategory.selectedItem.toString() else food.category
                val emoji = etEmoji.text.toString().trim().ifEmpty { food.emoji }

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!price.endsWith("đ")) price += "đ"

                val updatedFood = food.copy(
                    name = name,
                    restaurant = restaurant,
                    price = price,
                    category = category,
                    emoji = emoji
                )

                dbHelper.updateFood(updatedFood)
                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã cập nhật món '$name' thành công! ✨", Toast.LENGTH_SHORT).show()
                loadData()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDeleteFood(food: Food) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa món")
            .setMessage("Bạn có chắc chắn muốn xóa món '${food.name}' khỏi hệ thống?\nHành động này không thể hoàn tác.")
            .setPositiveButton("Xóa món ăn") { _, _ ->
                val ok = dbHelper.deleteFood(food.id)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã xóa món '${food.name}' thành công", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this, "Không thể xóa món ăn này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun createField(container: LinearLayout, hintText: String, defaultText: String): EditText {
        val et = EditText(this).apply {
            hint = hintText
            setText(defaultText)
            textSize = 14f
            setBackgroundResource(R.drawable.bg_field)
            setPadding(20, 20, 20, 20)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, 12)
            layoutParams = lp
        }
        container.addView(et)
        return et
    }
}
