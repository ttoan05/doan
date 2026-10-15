package com.example.foodhub.ui.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.utils.SampleData

class AdminCategoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var llCategoryContainer: LinearLayout
    private lateinit var tvCategoryCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_category)

        dbHelper = DatabaseHelper.getInstance(this)

        llCategoryContainer = findViewById(R.id.llCategoryContainer)
        tvCategoryCount     = findViewById(R.id.tvCategoryCount)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener { showAddCategoryDialog() }

        loadCategories()
    }

    private fun loadCategories() {
        llCategoryContainer.removeAllViews()

        val categories = dbHelper.getAllCategories()
        val allFoods = dbHelper.getAllFoods()

        tvCategoryCount.text = "Tổng số: ${categories.size} danh mục món ăn"

        categories.forEach { cat ->
            val isDefaultAll = cat.equals("Tất cả", ignoreCase = true)
            val count = if (isDefaultAll) allFoods.size else allFoods.count { it.category.equals(cat, ignoreCase = true) }

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(20, 18, 20, 18)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 12)
                layoutParams = params
            }

            val tvIcon = TextView(this).apply {
                text = if (isDefaultAll) "🌐" else "📁"
                textSize = 24f
                setPadding(0, 0, 16, 0)
            }
            card.addView(tvIcon)

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvName = TextView(this).apply {
                text = cat
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
            }

            val tvFoodCount = TextView(this).apply {
                text = if (isDefaultAll) "Bao gồm tất cả món ($count món)" else "Hiện có $count món ăn"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, 2, 0, 0)
            }

            infoCol.addView(tvName)
            infoCol.addView(tvFoodCount)
            card.addView(infoCol)

            if (isDefaultAll) {
                val tvBadge = TextView(this).apply {
                    text = "Mặc định"
                    textSize = 11f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#16A34A"))
                    setBackgroundResource(R.drawable.bg_cat_normal)
                    setPadding(16, 6, 16, 6)
                }
                card.addView(tvBadge)
            } else {
                val btnEdit = Button(this).apply {
                    text = "✏️ Sửa"
                    textSize = 12f
                    setBackgroundResource(R.drawable.bg_btn_blue)
                    setTextColor(Color.WHITE)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, 8, 0)
                    layoutParams = lp
                    setOnClickListener { showEditCategoryDialog(cat) }
                }

                val btnDelete = Button(this).apply {
                    text = "🗑️ Xóa"
                    textSize = 12f
                    setBackgroundResource(R.drawable.bg_btn_red_outline)
                    setTextColor(Color.parseColor("#E63946"))
                    setOnClickListener { confirmDeleteCategory(cat, count) }
                }

                card.addView(btnEdit)
                card.addView(btnDelete)
            }

            llCategoryContainer.addView(card)
        }
    }

    private fun showAddCategoryDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }

        val etName = EditText(this).apply {
            hint = "Nhập tên danh mục (vd: Trà sữa, Lẩu, Hải sản)"
            textSize = 14f
            setBackgroundResource(R.drawable.bg_field)
            setPadding(20, 20, 20, 20)
        }
        container.addView(etName)

        AlertDialog.Builder(this)
            .setTitle("➕ Thêm danh mục mới")
            .setView(container)
            .setPositiveButton("Thêm") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val ok = dbHelper.insertCategory(name) != -1L
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã thêm danh mục '$name' thành công", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Danh mục này đã tồn tại", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditCategoryDialog(oldName: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }

        val etName = EditText(this).apply {
            hint = "Tên danh mục mới"
            setText(oldName)
            textSize = 14f
            setBackgroundResource(R.drawable.bg_field)
            setPadding(20, 20, 20, 20)
        }
        container.addView(etName)

        AlertDialog.Builder(this)
            .setTitle("✏️ Đổi tên danh mục")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = etName.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val ok = dbHelper.updateCategory(oldName, newName)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã đổi tên thành '$newName'", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Không thể cập nhật danh mục", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDeleteCategory(cat: String, foodCount: Int) {
        val warning = if (foodCount > 0) {
            "\n⚠️ Lưu ý: Đang có $foodCount món ăn thuộc danh mục này."
        } else ""

        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa danh mục")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '$cat'?$warning")
            .setPositiveButton("Xóa") { _, _ ->
                val ok = dbHelper.deleteCategory(cat)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã xóa danh mục '$cat'", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Không thể xóa danh mục này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
