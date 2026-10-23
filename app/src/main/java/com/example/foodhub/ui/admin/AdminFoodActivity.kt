package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.FoodSizeHelper
import com.example.foodhub.utils.SampleData
import java.io.File
import java.io.FileOutputStream

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
        setContentView(R.layout.activity_admin_food)

        dbHelper = DatabaseHelper.getInstance(this)

        llFoodContainer = findViewById(R.id.llFoodContainer)
        llCategoryChips = findViewById(R.id.llCategoryChips)
        tvFoodCount     = findViewById(R.id.tvFoodCount)
        etSearchFood    = findViewById(R.id.etSearchFood)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddFood).setOnClickListener { showAddFoodDialog() }

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
            val q = searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) || it.restaurant.lowercase().contains(q)
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
                showEditFoodDialog(food)
            }
            itemView.findViewById<View>(R.id.btnDelete).setOnClickListener {
                confirmDeleteFood(food)
            }

            llFoodContainer.addView(itemView)
        }
    }

    private fun addSizeRow(
        container: LinearLayout,
        name: String = "",
        priceDelta: Int = 0
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, dpToPx(8))
            layoutParams = lp
        }

        val etSizeName = EditText(this).apply {
            hint = "Tên size (vd: Size L)"
            setText(name)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, dpToPx(40), 1.6f)
            lp.setMargins(0, 0, dpToPx(6), 0)
            layoutParams = lp
            setPadding(dpToPx(10), 0, dpToPx(10), 0)
            isSingleLine = true
        }

        val etSizePrice = EditText(this).apply {
            hint = "Giá thêm (+10k)"
            val priceStr = if (priceDelta > 0) priceDelta.toString() else "0"
            setText(priceStr)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, dpToPx(40), 1.1f)
            lp.setMargins(0, 0, dpToPx(6), 0)
            layoutParams = lp
            setPadding(dpToPx(10), 0, dpToPx(10), 0)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }

        val btnDelete = TextView(this).apply {
            text = "✕"
            textSize = 15f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            isClickable = true
            isFocusable = true
            setOnClickListener { container.removeView(row) }
        }

        row.addView(etSizeName)
        row.addView(etSizePrice)
        row.addView(btnDelete)
        container.addView(row)
    }

    private fun addToppingRow(
        container: LinearLayout,
        name: String = "",
        price: Int = 0
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, dpToPx(8))
            layoutParams = lp
        }

        val etToppingName = EditText(this).apply {
            hint = "Tên topping"
            setText(name)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, dpToPx(40), 1.6f)
            lp.setMargins(0, 0, dpToPx(6), 0)
            layoutParams = lp
            setPadding(dpToPx(10), 0, dpToPx(10), 0)
            isSingleLine = true
        }

        val etToppingPrice = EditText(this).apply {
            hint = "Giá (vd: 7000)"
            val priceStr = if (price > 0) price.toString() else "0"
            setText(priceStr)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, dpToPx(40), 1.1f)
            lp.setMargins(0, 0, dpToPx(6), 0)
            layoutParams = lp
            setPadding(dpToPx(10), 0, dpToPx(10), 0)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }

        val btnDelete = TextView(this).apply {
            text = "✕"
            textSize = 15f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            isClickable = true
            isFocusable = true
            setOnClickListener { container.removeView(row) }
        }

        row.addView(etToppingName)
        row.addView(etToppingPrice)
        row.addView(btnDelete)
        container.addView(row)
    }

    private fun collectSizesFromContainer(container: LinearLayout): List<FoodSizeHelper.AdminSize> {
        val list = mutableListOf<FoodSizeHelper.AdminSize>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            val etName = row.getChildAt(0) as? EditText ?: continue
            val etPrice = row.getChildAt(1) as? EditText ?: continue
            val name = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().replace(Regex("[^0-9]"), "")
            val price = priceStr.toIntOrNull() ?: 0
            if (name.isNotEmpty()) {
                list.add(FoodSizeHelper.AdminSize(name, price))
            }
        }
        return list
    }

    private fun collectToppingsFromContainer(container: LinearLayout): List<FoodSizeHelper.AdminTopping> {
        val list = mutableListOf<FoodSizeHelper.AdminTopping>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            val etName = row.getChildAt(0) as? EditText ?: continue
            val etPrice = row.getChildAt(1) as? EditText ?: continue
            val name = etName.text.toString().trim()
            val priceStr = etPrice.text.toString().replace(Regex("[^0-9]"), "")
            val price = priceStr.toIntOrNull() ?: 0
            if (name.isNotEmpty()) {
                list.add(FoodSizeHelper.AdminTopping(name, price))
            }
        }
        return list
    }

    private fun showAddFoodDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_food_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogFoodName)
        val etRestaurant = dialogView.findViewById<EditText>(R.id.etDialogRestaurant)
        val etPrice = dialogView.findViewById<EditText>(R.id.etDialogPrice)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spDialogCategory)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangePhotoBadge)

        val llSizes = dialogView.findViewById<LinearLayout>(R.id.llAdminSizesContainer)
        val btnAddSize = dialogView.findViewById<View>(R.id.btnAdminAddSize)
        val llToppings = dialogView.findViewById<LinearLayout>(R.id.llAdminToppingsContainer)
        val btnAddTopping = dialogView.findViewById<View>(R.id.btnAdminAddTopping)

        tvTitle.text = "Thêm món ăn mới"
        etRestaurant.setText("Quán Phở Tuyệt")
        etPrice.setText("45.000đ")

        var selectedImagePath: String? = null

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = SampleData.decodeSampledBitmap(path, 400, 300)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Throwable) {}
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

        // Populate Categories
        val validCats = allCategories.filter { it != "Tất cả" }
        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            validCats
        )

        // Pre-fill initial sizes & toppings
        val defaultSizes = FoodSizeHelper.getDefaultSizes("", "")
        defaultSizes.forEach { addSizeRow(llSizes, it.name, it.priceDelta) }

        val defaultToppings = FoodSizeHelper.getDefaultToppings("", "")
        defaultToppings.forEach { addToppingRow(llToppings, it.name, it.price) }

        btnAddSize.setOnClickListener { addSizeRow(llSizes, "", 0) }
        btnAddTopping.setOnClickListener { addToppingRow(llToppings, "", 0) }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            try {
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = spCategory.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: (if (validCats.isNotEmpty()) validCats[0] else "Cơm")

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin món", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!price.endsWith("đ") && !price.endsWith("Đ")) price += "đ"

                val collectedSizes = collectSizesFromContainer(llSizes)
                val collectedToppings = collectToppingsFromContainer(llToppings)
                val sizesJson = if (collectedSizes.isNotEmpty()) FoodSizeHelper.sizesToJson(collectedSizes) else null
                val toppingsJson = if (collectedToppings.isNotEmpty()) FoodSizeHelper.toppingsToJson(collectedToppings) else null

                val newId = dbHelper.getNextFoodId()
                val resolvedImg = SampleData.resolveFoodImage(newId, name, 0, category)
                val newFood = Food(
                    id = newId,
                    name = name,
                    restaurant = restaurant,
                    rating = 4.8,
                    reviews = 20,
                    price = price,
                    category = category,
                    emoji = "🍲",
                    bgColorStart = Color.parseColor("#F4A261"),
                    bgColorEnd = Color.parseColor("#E76F51"),
                    imageRes = resolvedImg,
                    imageUri = selectedImagePath,
                    sizesJson = sizesJson,
                    toppingsJson = toppingsJson
                )

                val rowId = dbHelper.insertFood(newFood)
                if (rowId <= 0) {
                    Toast.makeText(this, "Không thể lưu món ăn vào cơ sở dữ liệu!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Tự động thêm quán ăn vào danh sách Nhà hàng nếu chưa có
                try {
                    val existingRests = dbHelper.getAllRestaurants()
                    if (!existingRests.any { it.name.trim().equals(restaurant, ignoreCase = true) }) {
                        val newRestId = dbHelper.getNextRestaurantId()
                        val newRest = Restaurant(
                            id = newRestId,
                            name = restaurant,
                            desc = "$category • Đang phục vụ",
                            rating = 4.8,
                            time = "20-30 phút",
                            dist = "1.2 km",
                            badge = "Món mới",
                            bgColorStart = Color.parseColor("#F4A261"),
                            bgColorEnd = Color.parseColor("#E76F51"),
                            imageRes = resolvedImg,
                            imageUri = selectedImagePath
                        )
                        dbHelper.insertRestaurant(newRest)
                    }
                } catch (_: Throwable) {}

                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã thêm món '$name' thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadData()
            } catch (e: Throwable) {
                e.printStackTrace()
                Toast.makeText(this, "Lỗi khi thêm món: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        val dm = resources.displayMetrics
        dialog.window?.setLayout((dm.widthPixels * 0.94).toInt(), (dm.heightPixels * 0.88).toInt())
    }

    private fun showEditFoodDialog(food: Food) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_admin_food_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogFoodName)
        val etRestaurant = dialogView.findViewById<EditText>(R.id.etDialogRestaurant)
        val etPrice = dialogView.findViewById<EditText>(R.id.etDialogPrice)
        val spCategory = dialogView.findViewById<Spinner>(R.id.spDialogCategory)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangePhotoBadge)

        val llSizes = dialogView.findViewById<LinearLayout>(R.id.llAdminSizesContainer)
        val btnAddSize = dialogView.findViewById<View>(R.id.btnAdminAddSize)
        val llToppings = dialogView.findViewById<LinearLayout>(R.id.llAdminToppingsContainer)
        val btnAddTopping = dialogView.findViewById<View>(R.id.btnAdminAddTopping)

        tvTitle.text = "Chỉnh sửa món ăn"
        etName.setText(food.name)
        etRestaurant.setText(food.restaurant)
        etPrice.setText(food.price)

        var selectedImagePath: String? = food.imageUri

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = SampleData.decodeSampledBitmap(path, 400, 300)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Throwable) {}
            }
            val res = SampleData.resolveFoodImage(food.id, food.name, food.imageRes, food.category)
            if (res != 0) {
                ivUploadedPreview.setImageResource(res)
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

        val validCats = allCategories.filter { it != "Tất cả" }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            validCats
        )
        spCategory.adapter = adapter
        val currentIdx = validCats.indexOf(food.category)
        if (currentIdx >= 0) spCategory.setSelection(currentIdx)

        // Populate existing sizes or defaults
        val initialSizes = if (!food.sizesJson.isNullOrBlank()) {
            FoodSizeHelper.parseSizesJson(food.sizesJson)
        } else {
            FoodSizeHelper.getDefaultSizes(food.name, food.category)
        }
        initialSizes.forEach { addSizeRow(llSizes, it.name, it.priceDelta) }

        // Populate existing toppings or defaults
        val initialToppings = if (!food.toppingsJson.isNullOrBlank()) {
            FoodSizeHelper.parseToppingsJson(food.toppingsJson)
        } else {
            FoodSizeHelper.getDefaultToppings(food.name, food.category)
        }
        initialToppings.forEach { addToppingRow(llToppings, it.name, it.price) }

        btnAddSize.setOnClickListener { addSizeRow(llSizes, "", 0) }
        btnAddTopping.setOnClickListener { addToppingRow(llToppings, "", 0) }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            try {
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = spCategory.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: food.category

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!price.endsWith("đ") && !price.endsWith("Đ")) price += "đ"

                val collectedSizes = collectSizesFromContainer(llSizes)
                val collectedToppings = collectToppingsFromContainer(llToppings)
                val sizesJson = if (collectedSizes.isNotEmpty()) FoodSizeHelper.sizesToJson(collectedSizes) else null
                val toppingsJson = if (collectedToppings.isNotEmpty()) FoodSizeHelper.toppingsToJson(collectedToppings) else null

                val resolvedImg = SampleData.resolveFoodImage(food.id, name, food.imageRes, category)
                val updatedFood = food.copy(
                    name = name,
                    restaurant = restaurant,
                    price = price,
                    category = category,
                    imageRes = resolvedImg,
                    imageUri = selectedImagePath,
                    sizesJson = sizesJson,
                    toppingsJson = toppingsJson
                )

                val ok = dbHelper.updateFood(updatedFood)
                if (!ok) {
                    Toast.makeText(this, "Không thể cập nhật món ăn vào cơ sở dữ liệu!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Tự động thêm quán ăn vào danh sách Nhà hàng nếu chưa có
                try {
                    val existingRests = dbHelper.getAllRestaurants()
                    if (!existingRests.any { it.name.trim().equals(restaurant, ignoreCase = true) }) {
                        val newRestId = dbHelper.getNextRestaurantId()
                        val newRest = Restaurant(
                            id = newRestId,
                            name = restaurant,
                            desc = "$category • Đang phục vụ",
                            rating = 4.8,
                            time = "20-30 phút",
                            dist = "1.2 km",
                            badge = "Món mới",
                            bgColorStart = Color.parseColor("#F4A261"),
                            bgColorEnd = Color.parseColor("#E76F51"),
                            imageRes = resolvedImg,
                            imageUri = selectedImagePath
                        )
                        dbHelper.insertRestaurant(newRest)
                    }
                } catch (_: Throwable) {}

                SampleData.reloadFromDb(this)
                Toast.makeText(this, "Đã cập nhật món '$name' thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadData()
            } catch (e: Throwable) {
                e.printStackTrace()
                Toast.makeText(this, "Lỗi khi lưu món: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        val dm = resources.displayMetrics
        dialog.window?.setLayout((dm.widthPixels * 0.94).toInt(), (dm.heightPixels * 0.88).toInt())
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val fileName = "food_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            val inputStream = contentResolver.openInputStream(uri)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            var sampleSize = 1
            val maxDimension = 1200
            val maxOut = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            while (maxOut / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val secondStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream?.close()

            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                file.absolutePath
            } else {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun confirmDeleteFood(food: Food) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa món")
            .setMessage("Bạn có chắc chắn muốn xóa món '${food.name}' khỏi thực đơn?\nThao tác này sẽ không thể hoàn tác.")
            .setPositiveButton("Xóa món") { _, _ ->
                val ok = dbHelper.deleteFood(food.id)
                if (ok) {
                    SampleData.reloadFromDb(this)
                    Toast.makeText(this, "Đã xóa món '${food.name}' thành công", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this, "Không thể xóa món ăn này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
