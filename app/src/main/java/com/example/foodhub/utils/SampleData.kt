package com.example.foodhub.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import java.io.File

object SampleData {

    private val _categories = mutableListOf("Tất cả", "Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống")
    val CATEGORIES: List<String>
        get() = _categories

    val DEFAULT_FOODS = listOf(
        Food(1, "Phở bò đặc biệt",   "Quán Phở Tuyệt",   4.8, 156, "45.000đ", "Phở",      "🍲", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_pho_bo),
        Food(2, "Gà rán giòn cay",   "KFC Việt Nam",      4.9, 234, "55.000đ", "Gà rán",   "🍗", Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_ga_ran),
        Food(3, "Bánh mì thập cẩm",  "Bánh mì Sài Gòn",  4.7, 189, "25.000đ", "Bánh mì",  "🥖", Color.parseColor("#F4A261"), Color.parseColor("#F9C74F"), R.drawable.img_banh_mi),
        Food(4, "Cơm tấm sườn nướng","Cơm tấm Quỳnh",    4.6, 112, "35.000đ", "Cơm",      "🍛", Color.parseColor("#E63946"), Color.parseColor("#C1121F"), R.drawable.img_com_tam),
        Food(5, "Mì Quảng tôm thịt", "Mì Quảng Huế",     4.8,  98, "40.000đ", "Mì",       "🍝", Color.parseColor("#F4A261"), Color.parseColor("#E9C46A"), R.drawable.img_mi_quang),
        Food(6, "Nước chanh đá",     "Quán nước mía",     4.5,  45, "10.000đ", "Đồ uống",  "🥤", Color.parseColor("#457B9D"), Color.parseColor("#1D3557"), R.drawable.img_nuoc_chanh)
    )

    private val _foods = mutableListOf<Food>().apply { addAll(DEFAULT_FOODS) }
    val FOODS: List<Food>
        get() = _foods

    val DEFAULT_RESTAURANTS = listOf(
        Restaurant(1, "Quán Phở Tuyệt",  "Phở, Cơm, Mì • Quận 1",             4.8, "30-45 phút", "1.2 km", "Freeship đơn từ 50k", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_res_pho),
        Restaurant(2, "KFC Việt Nam",    "Gà rán, Thức ăn nhanh • Quận 3",     4.9, "20-30 phút", "2.5 km", "Ưu đãi 20% combo",    Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_res_kfc),
        Restaurant(3, "Bánh mì Sài Gòn","Bánh mì, Nước giải khát • Quận 10",  4.7, "15-25 phút", "0.8 km", "Bán chạy nhất",       Color.parseColor("#F9C74F"), Color.parseColor("#F4A261"), R.drawable.img_res_banh_mi)
    )

    private val _restaurants = mutableListOf<Restaurant>().apply { addAll(DEFAULT_RESTAURANTS) }
    val RESTAURANTS: List<Restaurant>
        get() = _restaurants

    /**
     * Tự động ánh xạ ảnh drawable chuẩn cho món ăn theo ID, tên món hoặc danh mục.
     */
    fun resolveFoodImage(id: Int, name: String, storedRes: Int = 0, category: String = ""): Int {
        val def = DEFAULT_FOODS.find { it.id == id || it.name.trim().equals(name.trim(), ignoreCase = true) }
        if (def != null && def.imageRes != 0) return def.imageRes
        val lower = name.lowercase()
        val catLower = category.lowercase()
        return when {
            lower.contains("phở") || catLower.contains("phở") -> R.drawable.img_pho_bo
            lower.contains("gà") || catLower.contains("gà") -> R.drawable.img_ga_ran
            lower.contains("bánh mì") || catLower.contains("bánh mì") -> R.drawable.img_banh_mi
            lower.contains("cơm") || catLower.contains("cơm") -> R.drawable.img_com_tam
            lower.contains("mì quảng") || lower.contains("mì") || catLower.contains("mì") -> R.drawable.img_mi_quang
            lower.contains("chanh") || lower.contains("nước") || lower.contains("trà") || lower.contains("sữa") || catLower.contains("uống") -> R.drawable.img_nuoc_chanh
            storedRes != 0 -> storedRes
            else -> R.drawable.img_com_tam
        }
    }

    /**
     * Tự động ánh xạ ảnh drawable chuẩn cho nhà hàng theo ID hoặc tên quán.
     */
    fun resolveRestaurantImage(id: Int, name: String, storedRes: Int = 0): Int {
        val def = DEFAULT_RESTAURANTS.find { it.id == id || it.name.trim().equals(name.trim(), ignoreCase = true) }
        if (def != null && def.imageRes != 0) return def.imageRes
        val lower = name.lowercase()
        return when {
            lower.contains("phở") -> R.drawable.img_res_pho
            lower.contains("kfc") || lower.contains("gà") -> R.drawable.img_res_kfc
            lower.contains("bánh mì") -> R.drawable.img_res_banh_mi
            storedRes != 0 -> storedRes
            else -> R.drawable.img_res_pho
        }
    }

    /**
     * Tra cứu đường dẫn ảnh do admin upload theo foodId.
     */
    fun resolveFoodImageUri(foodId: Int, context: Context? = null): String? {
        val cached = _foods.find { it.id == foodId }?.imageUri
        if (!cached.isNullOrEmpty()) return cached
        if (context != null) {
            try {
                val db = DatabaseHelper.getInstance(context)
                val fromDb = db.getAllFoods().find { it.id == foodId }?.imageUri
                if (!fromDb.isNullOrEmpty()) return fromDb
            } catch (_: Throwable) {}
        }
        return null
    }

    /**
     * Giải mã ảnh an toàn chống OutOfMemoryError
     */
    fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            options.inSampleSize = inSampleSize
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Hiển thị ảnh món ăn ưu tiên ảnh người dùng upload từ máy (imageUri), sau đó đến drawable tài nguyên.
     */
    fun loadFoodImage(imageView: ImageView, emojiView: TextView?, food: Food) {
        val uri = if (!food.imageUri.isNullOrEmpty()) food.imageUri else resolveFoodImageUri(food.id, imageView.context)
        if (!uri.isNullOrEmpty()) {
            val file = File(uri)
            if (file.exists()) {
                try {
                    val bitmap = decodeSampledBitmap(file.absolutePath, 350, 350)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        emojiView?.visibility = View.GONE
                        return
                    }
                } catch (_: Throwable) {}
            }
        }

        val res = resolveFoodImage(food.id, food.name, food.imageRes, food.category)
        if (res != 0) {
            imageView.setImageResource(res)
            imageView.visibility = View.VISIBLE
            emojiView?.visibility = View.GONE
        } else {
            imageView.setImageResource(R.drawable.img_com_tam)
            imageView.visibility = View.VISIBLE
            emojiView?.visibility = View.GONE
        }
    }

    /**
     * Hiển thị ảnh cho CartItem trong Giỏ hàng và Đơn hàng.
     */
    fun loadCartItemImage(imageView: ImageView, emojiView: TextView?, item: CartItem) {
        val uri = if (!item.imageUri.isNullOrEmpty()) item.imageUri else resolveFoodImageUri(item.foodId, imageView.context)
        if (!uri.isNullOrEmpty()) {
            val file = File(uri)
            if (file.exists()) {
                try {
                    val bitmap = decodeSampledBitmap(file.absolutePath, 200, 200)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        emojiView?.visibility = View.GONE
                        return
                    }
                } catch (_: Throwable) {}
            }
        }

        val res = resolveFoodImage(item.foodId, item.name, item.imageRes)
        if (res != 0) {
            imageView.setImageResource(res)
            imageView.visibility = View.VISIBLE
            emojiView?.visibility = View.GONE
        } else {
            imageView.setImageResource(R.drawable.img_com_tam)
            imageView.visibility = View.VISIBLE
            emojiView?.visibility = View.GONE
        }
    }

    /**
     * Hiển thị ảnh cho Nhà hàng: Ưu tiên ảnh admin upload từ máy (imageUri), tiếp theo là imageRes, cuối cùng là màu gradient fallback.
     */
    fun loadRestaurantImage(imageView: ImageView, bannerContainer: View?, restaurant: Restaurant) {
        val uri = restaurant.imageUri
        if (!uri.isNullOrEmpty()) {
            val file = File(uri)
            if (file.exists()) {
                try {
                    val bitmap = decodeSampledBitmap(file.absolutePath, 500, 300)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        return
                    }
                } catch (_: Throwable) {}
            }
        }

        val res = resolveRestaurantImage(restaurant.id, restaurant.name, restaurant.imageRes)
        if (res != 0) {
            imageView.setImageResource(res)
            imageView.visibility = View.VISIBLE
        } else {
            val bgStart = if (restaurant.bgColorStart != 0) restaurant.bgColorStart else Color.parseColor("#E63946")
            val bgEnd = if (restaurant.bgColorEnd != 0) restaurant.bgColorEnd else Color.parseColor("#C1121F")
            if (bannerContainer != null) {
                imageView.visibility = View.GONE
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(bgStart, bgEnd)
                )
                bannerContainer.background = gradient
            } else {
                imageView.setImageDrawable(
                    GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(bgStart, bgEnd)
                    )
                )
                imageView.visibility = View.VISIBLE
            }
        }
    }

    var onFoodsChanged: (() -> Unit)? = null

    fun reloadFromDb(context: Context) {
        try {
            val db = DatabaseHelper.getInstance(context)
            val cats = db.getAllCategories()
            if (cats.isNotEmpty()) {
                _categories.clear()
                if (!cats.contains("Tất cả")) {
                    _categories.add("Tất cả")
                }
                _categories.addAll(cats.filter { it != "Tất cả" })
            }
            val foods = db.getAllFoods()
            if (foods.isNotEmpty()) {
                _foods.clear()
                _foods.addAll(foods.map { f ->
                    f.copy(imageRes = resolveFoodImage(f.id, f.name, f.imageRes, f.category))
                })
                foods.forEach { f ->
                    val c = f.category.trim()
                    if (c.isNotEmpty() && !_categories.any { it.equals(c, ignoreCase = true) }) {
                        _categories.add(c)
                    }
                }
            }
            val rests = db.getAllRestaurants()
            if (rests.isNotEmpty()) {
                _restaurants.clear()
                _restaurants.addAll(rests)
            }
            onFoodsChanged?.invoke()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
