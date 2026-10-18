package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.SampleData
import java.io.File
import java.io.FileOutputStream

class AdminCategoryActivity : AppCompatActivity() {

    companion object {
        private const val TAB_CATEGORY = 0
        private const val TAB_RESTAURANT = 1
    }

    private lateinit var dbHelper: DatabaseHelper
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
            val savedPath = saveImageToInternalStorage(uri)
            if (savedPath != null) {
                pendingImageCallback?.invoke(savedPath)
            } else {
                Toast.makeText(this, "Không thể lưu ảnh từ thiết bị", Toast.LENGTH_SHORT).show()
            }
        }
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
                showAddCategoryDialog()
            } else {
                showAddRestaurantDialog()
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

                btnEdit.setOnClickListener { showEditCategoryDialog(cat) }
                btnDelete.setOnClickListener { confirmDeleteCategory(cat, count) }
            }

            llCategoryContainer.addView(itemView)
        }
    }

    /** Ánh xạ ảnh món ăn thực tế chuẩn đẹp cho từng danh mục mặc định */
    private fun resolveCategoryThumbnail(category: String): Int {
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

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_category_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatSubtitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogCatClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogCatName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCatCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogCatSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadCatImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadCatPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedCatPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeCatPhotoBadge)

        tvTitle.text = "Thêm danh mục món mới"
        tvSubtitle.text = "Điền tên danh mục và tải ảnh đại diện lên hệ thống"
        btnSave.text = "Thêm danh mục"

        var selectedImagePath: String? = null

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Exception) {}
            }
            ivUploadedPreview.visibility = View.GONE
            tvChangePhotoBadge.visibility = View.GONE
            llUploadPlaceholder.visibility = View.VISIBLE
        }

        cvUploadImage.setOnClickListener {
            pendingImageCallback = { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
            pickImageLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = dbHelper.insertCategory(name, selectedImagePath) != -1L
            if (ok) {
                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã thêm danh mục '$name'", Toast.LENGTH_SHORT).show()
                allFoods = dbHelper.getAllFoods()
                loadCategories()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Danh mục này đã tồn tại", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditCategoryDialog(oldName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_category_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatSubtitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogCatClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogCatName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCatCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogCatSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadCatImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadCatPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedCatPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeCatPhotoBadge)

        tvTitle.text = "Chỉnh sửa danh mục"
        tvSubtitle.text = "Cập nhật tên và đổi hình ảnh đại diện của danh mục"
        btnSave.text = "Lưu thay đổi"
        etName.setText(oldName)

        var selectedImagePath: String? = dbHelper.getCategoryImageUri(oldName)

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Exception) {}
            }
            // Nếu chưa có ảnh tải lên riêng thì hiển thị ảnh mẫu mặc định hiện tại
            val defaultRes = resolveCategoryThumbnail(oldName)
            if (defaultRes != 0) {
                ivUploadedPreview.setImageResource(defaultRes)
                ivUploadedPreview.visibility = View.VISIBLE
                tvChangePhotoBadge.visibility = View.VISIBLE
                llUploadPlaceholder.visibility = View.GONE
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        updateImagePreview(selectedImagePath)

        cvUploadImage.setOnClickListener {
            pendingImageCallback = { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
            pickImageLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = dbHelper.updateCategory(oldName, newName, selectedImagePath)
            if (ok) {
                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã cập nhật danh mục '$newName'", Toast.LENGTH_SHORT).show()
                allFoods = dbHelper.getAllFoods()
                loadCategories()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Không thể cập nhật danh mục", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun confirmDeleteCategory(cat: String, foodCount: Int) {
        val warning = if (foodCount > 0) {
            "\nLưu ý: Hiện có $foodCount món ăn thuộc danh mục này sẽ chuyển sang danh mục 'Khác'."
        } else ""

        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa danh mục")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '$cat'?$warning")
            .setPositiveButton("Xóa") { _, _ ->
                val ok = dbHelper.deleteCategory(cat)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã xóa danh mục '$cat'", Toast.LENGTH_SHORT).show()
                    allFoods = dbHelper.getAllFoods()
                    loadCategories()
                } else {
                    Toast.makeText(this, "Không thể xóa danh mục này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
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

            btnEdit.setOnClickListener { showEditRestaurantDialog(r) }
            btnDelete.setOnClickListener { confirmDeleteRestaurant(r, count) }

            llRestaurantContainer.addView(itemView)
        }
    }

    private fun showAddRestaurantDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_restaurant_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRestTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogRestClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogRestName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogRestDesc)
        val etTime = dialogView.findViewById<EditText>(R.id.etDialogRestTime)
        val etDist = dialogView.findViewById<EditText>(R.id.etDialogRestDist)
        val etBadge = dialogView.findViewById<EditText>(R.id.etDialogRestBadge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogRestCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogRestSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadRestImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadRestPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedRestPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeRestPhotoBadge)

        tvTitle.text = "Thêm nhà hàng mới"
        etTime.setText("20-30 phút")
        etDist.setText("1.5 km")
        etBadge.setText("Freeship đơn từ 50k")

        var selectedImagePath: String? = null

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    ivUploadedPreview.setImageBitmap(bitmap)
                    ivUploadedPreview.visibility = View.VISIBLE
                    tvChangePhotoBadge.visibility = View.VISIBLE
                    llUploadPlaceholder.visibility = View.GONE
                } catch (_: Exception) {}
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        cvUploadImage.setOnClickListener {
            pendingImageCallback = { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
            pickImageLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            var time = etTime.text.toString().trim().ifEmpty { "20-30 phút" }
            var dist = etDist.text.toString().trim().ifEmpty { "1.5 km" }
            val badge = etBadge.text.toString().trim()

            if (name.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nhà hàng và mô tả/khu vực", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newId = dbHelper.getNextRestaurantId()
            val newRestaurant = Restaurant(
                id = newId,
                name = name,
                desc = desc,
                rating = 5.0,
                time = time,
                dist = dist,
                badge = badge,
                bgColorStart = Color.parseColor("#E63946"),
                bgColorEnd = Color.parseColor("#C1121F"),
                imageRes = 0,
                imageUri = selectedImagePath
            )

            dbHelper.insertRestaurant(newRestaurant)
            SampleData.reloadFromDb(this)
            Toast.makeText(this, "Đã thêm nhà hàng '$name' thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadRestaurants()
        }

        dialog.show()
    }

    private fun showEditRestaurantDialog(restaurant: Restaurant) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_restaurant_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRestTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogRestClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogRestName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogRestDesc)
        val etTime = dialogView.findViewById<EditText>(R.id.etDialogRestTime)
        val etDist = dialogView.findViewById<EditText>(R.id.etDialogRestDist)
        val etBadge = dialogView.findViewById<EditText>(R.id.etDialogRestBadge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogRestCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogRestSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadRestImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadRestPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedRestPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeRestPhotoBadge)

        tvTitle.text = "Chỉnh sửa nhà hàng"
        etName.setText(restaurant.name)
        etDesc.setText(restaurant.desc)
        etTime.setText(restaurant.time)
        etDist.setText(restaurant.dist)
        etBadge.setText(restaurant.badge)

        var selectedImagePath: String? = restaurant.imageUri

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    ivUploadedPreview.setImageBitmap(bitmap)
                    ivUploadedPreview.visibility = View.VISIBLE
                    tvChangePhotoBadge.visibility = View.VISIBLE
                    llUploadPlaceholder.visibility = View.GONE
                    return
                } catch (_: Exception) {}
            }
            if (restaurant.imageRes != 0) {
                ivUploadedPreview.setImageResource(restaurant.imageRes)
                ivUploadedPreview.visibility = View.VISIBLE
                tvChangePhotoBadge.visibility = View.VISIBLE
                llUploadPlaceholder.visibility = View.GONE
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        updateImagePreview(selectedImagePath)

        cvUploadImage.setOnClickListener {
            pendingImageCallback = { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
            pickImageLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            var time = etTime.text.toString().trim().ifEmpty { "20-30 phút" }
            var dist = etDist.text.toString().trim().ifEmpty { "1.5 km" }
            val badge = etBadge.text.toString().trim()

            if (name.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nhà hàng và mô tả/khu vực", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedRestaurant = restaurant.copy(
                name = name,
                desc = desc,
                time = time,
                dist = dist,
                badge = badge,
                imageUri = selectedImagePath
            )

            dbHelper.updateRestaurant(updatedRestaurant)
            SampleData.reloadFromDb(this)
            Toast.makeText(this, "Đã cập nhật nhà hàng '$name' thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            loadRestaurants()
        }

        dialog.show()
    }

    private fun confirmDeleteRestaurant(r: Restaurant, foodCount: Int) {
        val warning = if (foodCount > 0) {
            "\nLưu ý: Hiện đang có $foodCount món ăn gắn với nhà hàng này."
        } else ""

        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa nhà hàng")
            .setMessage("Bạn có chắc chắn muốn xóa nhà hàng '${r.name}'?$warning")
            .setPositiveButton("Xóa") { _, _ ->
                val ok = dbHelper.deleteRestaurant(r.id)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã xóa nhà hàng '${r.name}'", Toast.LENGTH_SHORT).show()
                    loadRestaurants()
                } else {
                    Toast.makeText(this, "Không thể xóa nhà hàng này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
