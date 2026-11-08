package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.foodhub.R
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.FoodSizeHelper
import com.example.foodhub.utils.SampleData
import java.io.File
import java.io.FileOutputStream

/**
 * Tách riêng toàn bộ logic Dialog Thêm / Sửa / Xóa món ăn và quản lý Size/Topping của AdminFoodActivity.
 * Giữ nguyên 100% logic giao diện và hành vi ban đầu.
 */
object AdminFoodDialogHelper {

    fun addSizeRow(
        activity: AdminFoodActivity,
        container: LinearLayout,
        name: String = "",
        priceDelta: Int = 0
    ) {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, activity.dpToPx(8))
            layoutParams = lp
        }

        val etSizeName = EditText(activity).apply {
            hint = "Tên size (vd: Size L)"
            setText(name)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, activity.dpToPx(40), 1.6f)
            lp.setMargins(0, 0, activity.dpToPx(6), 0)
            layoutParams = lp
            setPadding(activity.dpToPx(10), 0, activity.dpToPx(10), 0)
            isSingleLine = true
        }

        val etSizePrice = EditText(activity).apply {
            hint = "Giá thêm (+10k)"
            val priceStr = if (priceDelta > 0) priceDelta.toString() else "0"
            setText(priceStr)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, activity.dpToPx(40), 1.1f)
            lp.setMargins(0, 0, activity.dpToPx(6), 0)
            layoutParams = lp
            setPadding(activity.dpToPx(10), 0, activity.dpToPx(10), 0)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }

        val btnDelete = TextView(activity).apply {
            text = "✕"
            textSize = 15f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(activity.dpToPx(32), activity.dpToPx(32))
            isClickable = true
            isFocusable = true
            setOnClickListener { container.removeView(row) }
        }

        row.addView(etSizeName)
        row.addView(etSizePrice)
        row.addView(btnDelete)
        container.addView(row)
    }

    fun addToppingRow(
        activity: AdminFoodActivity,
        container: LinearLayout,
        name: String = "",
        price: Int = 0
    ) {
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 0, 0, activity.dpToPx(8))
            layoutParams = lp
        }

        val etToppingName = EditText(activity).apply {
            hint = "Tên topping"
            setText(name)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, activity.dpToPx(40), 1.6f)
            lp.setMargins(0, 0, activity.dpToPx(6), 0)
            layoutParams = lp
            setPadding(activity.dpToPx(10), 0, activity.dpToPx(10), 0)
            isSingleLine = true
        }

        val etToppingPrice = EditText(activity).apply {
            hint = "Giá (vd: 7000)"
            val priceStr = if (price > 0) price.toString() else "0"
            setText(priceStr)
            textSize = 13f
            setTextColor(Color.parseColor("#0F172A"))
            setBackgroundResource(R.drawable.bg_field)
            val lp = LinearLayout.LayoutParams(0, activity.dpToPx(40), 1.1f)
            lp.setMargins(0, 0, activity.dpToPx(6), 0)
            layoutParams = lp
            setPadding(activity.dpToPx(10), 0, activity.dpToPx(10), 0)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
        }

        val btnDelete = TextView(activity).apply {
            text = "✕"
            textSize = 15f
            setTextColor(Color.parseColor("#EF4444"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(activity.dpToPx(32), activity.dpToPx(32))
            isClickable = true
            isFocusable = true
            setOnClickListener { container.removeView(row) }
        }

        row.addView(etToppingName)
        row.addView(etToppingPrice)
        row.addView(btnDelete)
        container.addView(row)
    }

    fun collectSizesFromContainer(container: LinearLayout): List<FoodSizeHelper.AdminSize> {
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

    fun collectToppingsFromContainer(container: LinearLayout): List<FoodSizeHelper.AdminTopping> {
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

    fun showAddFoodDialog(
        activity: AdminFoodActivity,
        allCategories: List<String>,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_food_form, null)
        val dialog = AlertDialog.Builder(activity)
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
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        // Populate Categories
        val validCats = allCategories.filter { it != "Tất cả" }
        spCategory.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            validCats
        )

        // Pre-fill initial sizes & toppings
        val defaultSizes = FoodSizeHelper.getDefaultSizes("", "")
        defaultSizes.forEach { addSizeRow(activity, llSizes, it.name, it.priceDelta) }

        val defaultToppings = FoodSizeHelper.getDefaultToppings("", "")
        defaultToppings.forEach { addToppingRow(activity, llToppings, it.name, it.price) }

        btnAddSize.setOnClickListener { addSizeRow(activity, llSizes, "", 0) }
        btnAddTopping.setOnClickListener { addToppingRow(activity, llToppings, "", 0) }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            try {
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = spCategory.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: (if (validCats.isNotEmpty()) validCats[0] else "Cơm")

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(activity, "Vui lòng nhập đầy đủ thông tin món", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!price.endsWith("đ") && !price.endsWith("Đ")) price += "đ"

                val collectedSizes = collectSizesFromContainer(llSizes)
                val collectedToppings = collectToppingsFromContainer(llToppings)
                val sizesJson = FoodSizeHelper.sizesToJson(collectedSizes)
                val toppingsJson = FoodSizeHelper.toppingsToJson(collectedToppings)

                val newId = activity.dbHelper.getNextFoodId()
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

                val rowId = activity.dbHelper.insertFood(newFood)
                if (rowId <= 0) {
                    Toast.makeText(activity, "Không thể lưu món ăn vào cơ sở dữ liệu!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Tự động thêm quán ăn vào danh sách Nhà hàng nếu chưa có
                try {
                    val existingRests = activity.dbHelper.getAllRestaurants()
                    if (!existingRests.any { it.name.trim().equals(restaurant, ignoreCase = true) }) {
                        val newRestId = activity.dbHelper.getNextRestaurantId()
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
                        activity.dbHelper.insertRestaurant(newRest)
                    }
                } catch (_: Throwable) {}

                SampleData.reloadFromDb(activity)
                Toast.makeText(activity, "Đã thêm món '$name' thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSuccess()
            } catch (e: Throwable) {
                e.printStackTrace()
                Toast.makeText(activity, "Lỗi khi thêm món: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        val dm = activity.resources.displayMetrics
        dialog.window?.setLayout((dm.widthPixels * 0.94).toInt(), (dm.heightPixels * 0.88).toInt())
    }

    fun showEditFoodDialog(
        activity: AdminFoodActivity,
        food: Food,
        allCategories: List<String>,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_food_form, null)
        val dialog = AlertDialog.Builder(activity)
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
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        val validCats = allCategories.filter { it != "Tất cả" }
        val adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            validCats
        )
        spCategory.adapter = adapter
        val currentIdx = validCats.indexOf(food.category)
        if (currentIdx >= 0) spCategory.setSelection(currentIdx)

        // Populate existing sizes or defaults
        val parsedSizes = FoodSizeHelper.parseSizesJson(food.sizesJson)
        val initialSizes = if (parsedSizes.isNotEmpty()) {
            parsedSizes
        } else {
            FoodSizeHelper.getDefaultSizes(food.name, food.category)
        }
        initialSizes.forEach { addSizeRow(activity, llSizes, it.name, it.priceDelta) }

        // Populate existing toppings or defaults
        val parsedToppings = FoodSizeHelper.parseToppingsJson(food.toppingsJson)
        val initialToppings = if (parsedToppings.isNotEmpty()) {
            parsedToppings
        } else {
            FoodSizeHelper.getDefaultToppings(food.name, food.category)
        }
        initialToppings.forEach { addToppingRow(activity, llToppings, it.name, it.price) }

        btnAddSize.setOnClickListener { addSizeRow(activity, llSizes, "", 0) }
        btnAddTopping.setOnClickListener { addToppingRow(activity, llToppings, "", 0) }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            try {
                val name = etName.text.toString().trim()
                val restaurant = etRestaurant.text.toString().trim()
                var price = etPrice.text.toString().trim()
                val category = spCategory.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: food.category

                if (name.isEmpty() || restaurant.isEmpty() || price.isEmpty()) {
                    Toast.makeText(activity, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!price.endsWith("đ") && !price.endsWith("Đ")) price += "đ"

                val collectedSizes = collectSizesFromContainer(llSizes)
                val collectedToppings = collectToppingsFromContainer(llToppings)
                val sizesJson = FoodSizeHelper.sizesToJson(collectedSizes)
                val toppingsJson = FoodSizeHelper.toppingsToJson(collectedToppings)

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

                val ok = activity.dbHelper.updateFood(updatedFood)
                if (!ok) {
                    Toast.makeText(activity, "Không thể cập nhật món ăn vào cơ sở dữ liệu!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // Tự động thêm quán ăn vào danh sách Nhà hàng nếu chưa có
                try {
                    val existingRests = activity.dbHelper.getAllRestaurants()
                    if (!existingRests.any { it.name.trim().equals(restaurant, ignoreCase = true) }) {
                        val newRestId = activity.dbHelper.getNextRestaurantId()
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
                        activity.dbHelper.insertRestaurant(newRest)
                    }
                } catch (_: Throwable) {}

                SampleData.reloadFromDb(activity)
                Toast.makeText(activity, "Đã cập nhật món '$name' thành công!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                onSuccess()
            } catch (e: Throwable) {
                e.printStackTrace()
                Toast.makeText(activity, "Lỗi khi lưu món: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        val dm = activity.resources.displayMetrics
        dialog.window?.setLayout((dm.widthPixels * 0.94).toInt(), (dm.heightPixels * 0.88).toInt())
    }

    fun confirmDeleteFood(activity: AdminFoodActivity, food: Food, onSuccess: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Xác nhận xóa món")
            .setMessage("Bạn có chắc chắn muốn xóa món '${food.name}' khỏi thực đơn?\nThao tác này sẽ không thể hoàn tác.")
            .setPositiveButton("Xóa món") { _, _ ->
                val ok = activity.dbHelper.deleteFood(food.id)
                if (ok) {
                    SampleData.reloadFromDb(activity)
                    Toast.makeText(activity, "Đã xóa món '${food.name}' thành công", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(activity, "Không thể xóa món ăn này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    fun saveImageToInternalStorage(activity: AdminFoodActivity, uri: Uri): String? {
        return com.example.foodhub.utils.ImageUtils.saveUriToInternalStorage(activity, uri, "food")
    }
}
