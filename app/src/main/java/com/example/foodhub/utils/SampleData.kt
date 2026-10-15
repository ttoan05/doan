package com.example.foodhub.utils

import android.content.Context
import android.graphics.Color
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant

object SampleData {

    private val _categories = mutableListOf("Tất cả", "Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống")
    val CATEGORIES: List<String>
        get() = _categories

    val DEFAULT_FOODS = listOf(
        Food(1, "Phở bò đặc biệt",   "Quán Phở Tuyệt",   4.8, 156, "45.000đ", "Phở",      "🍜", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_pho_bo),
        Food(2, "Gà rán giòn cay",   "KFC Việt Nam",      4.9, 234, "55.000đ", "Gà rán",   "🍗", Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_ga_ran),
        Food(3, "Bánh mì thập cẩm",  "Bánh mì Sài Gòn",  4.7, 189, "25.000đ", "Bánh mì",  "🥖", Color.parseColor("#F4A261"), Color.parseColor("#F9C74F"), R.drawable.img_banh_mi),
        Food(4, "Cơm tấm sườn nướng","Cơm tấm Quỳnh",    4.6, 112, "35.000đ", "Cơm",      "🍛", Color.parseColor("#E63946"), Color.parseColor("#C1121F"), R.drawable.img_com_tam),
        Food(5, "Mì Quảng tôm thịt", "Mì Quảng Huế",     4.8,  98, "40.000đ", "Mì",       "🍝", Color.parseColor("#F4A261"), Color.parseColor("#E9C46A"), R.drawable.img_mi_quang),
        Food(6, "Nước chanh đá",     "Quán nước mía",     4.5,  45, "10.000đ", "Đồ uống",  "🥤", Color.parseColor("#457B9D"), Color.parseColor("#1D3557"), R.drawable.img_nuoc_chanh)
    )

    private val _foods = mutableListOf<Food>().apply { addAll(DEFAULT_FOODS) }
    val FOODS: List<Food>
        get() = _foods

    val RESTAURANTS = listOf(
        Restaurant(1, "Quán Phở Tuyệt",  "Phở, Cơm, Mì • Quận 1",             4.8, "30-45 phút", "1.2 km", "Freeship đơn từ 50k", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_res_pho),
        Restaurant(2, "KFC Việt Nam",    "Gà rán, Thức ăn nhanh • Quận 3",     4.9, "20-30 phút", "2.5 km", "Ưu đãi 20% combo",    Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_res_kfc),
        Restaurant(3, "Bánh mì Sài Gòn","Bánh mì, Nước giải khát • Quận 10",  4.7, "15-25 phút", "0.8 km", "Bán chạy nhất",       Color.parseColor("#F9C74F"), Color.parseColor("#F4A261"), R.drawable.img_res_banh_mi)
    )

    /**
     * Tự động ánh xạ ảnh drawable chuẩn cho món ăn theo ID hoặc tên món.
     * Giải quyết triệt để vấn đề ID tài nguyên Android (R.drawable.*) bị thay đổi sau mỗi lần build/compile.
     */
    fun resolveFoodImage(id: Int, name: String, storedRes: Int = 0): Int {
        val def = DEFAULT_FOODS.find { it.id == id || it.name.trim().equals(name.trim(), ignoreCase = true) }
        if (def != null && def.imageRes != 0) return def.imageRes
        val lower = name.lowercase()
        return when {
            lower.contains("phở") -> R.drawable.img_pho_bo
            lower.contains("gà") -> R.drawable.img_ga_ran
            lower.contains("bánh mì") -> R.drawable.img_banh_mi
            lower.contains("cơm") -> R.drawable.img_com_tam
            lower.contains("mì quảng") || (lower.contains("mì") && !lower.contains("bánh")) -> R.drawable.img_mi_quang
            lower.contains("chanh") || lower.contains("nước") -> R.drawable.img_nuoc_chanh
            else -> storedRes
        }
    }

    fun reloadFromDb(context: Context) {
        try {
            val db = DatabaseHelper.getInstance(context)
            val cats = db.getAllCategories()
            if (cats.isNotEmpty()) {
                _categories.clear()
                _categories.addAll(cats)
            }
            val foods = db.getAllFoods()
            if (foods.isNotEmpty()) {
                _foods.clear()
                _foods.addAll(foods.map { f ->
                    f.copy(imageRes = resolveFoodImage(f.id, f.name, f.imageRes))
                })
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
