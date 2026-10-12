package com.example.foodhub.utils

import android.graphics.Color
import com.example.foodhub.R
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant

object SampleData {

    val CATEGORIES = listOf("Tất cả", "Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống")

    val FOODS = listOf(
        Food(1, "Phở bò đặc biệt",   "Quán Phở Tuyệt",   4.8, 156, "45.000đ", "Phở",      "🍜", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_pho_bo),
        Food(2, "Gà rán giòn cay",   "KFC Việt Nam",      4.9, 234, "55.000đ", "Gà rán",   "🍗", Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_ga_ran),
        Food(3, "Bánh mì thập cẩm",  "Bánh mì Sài Gòn",  4.7, 189, "25.000đ", "Bánh mì",  "🥖", Color.parseColor("#F4A261"), Color.parseColor("#F9C74F"), R.drawable.img_banh_mi),
        Food(4, "Cơm tấm sườn nướng","Cơm tấm Quỳnh",    4.6, 112, "35.000đ", "Cơm",      "🍚", Color.parseColor("#E63946"), Color.parseColor("#C1121F"), R.drawable.img_com_tam),
        Food(5, "Mì Quảng tôm thịt", "Mì Quảng Huế",     4.8,  98, "40.000đ", "Mì",       "🍝", Color.parseColor("#F4A261"), Color.parseColor("#E9C46A"), R.drawable.img_mi_quang),
        Food(6, "Nước chanh đá",     "Quán nước mía",     4.5,  45, "10.000đ", "Đồ uống",  "🧃", Color.parseColor("#457B9D"), Color.parseColor("#1D3557"), R.drawable.img_nuoc_chanh)
    )

    val RESTAURANTS = listOf(
        Restaurant(1, "Quán Phở Tuyệt",  "Phở, Cơm, Mì • Quận 1",             4.8, "30-45 phút", "1.2 km", "Freeship đơn từ 50k", Color.parseColor("#F4A261"), Color.parseColor("#E76F51"), R.drawable.img_res_pho),
        Restaurant(2, "KFC Việt Nam",    "Gà rán, Thức ăn nhanh • Quận 3",     4.9, "20-30 phút", "2.5 km", "Ưu đãi 20% combo",    Color.parseColor("#E76F51"), Color.parseColor("#C1440E"), R.drawable.img_res_kfc),
        Restaurant(3, "Bánh mì Sài Gòn","Bánh mì, Nước giải khát • Quận 10",  4.7, "15-25 phút", "0.8 km", "Bán chạy nhất",       Color.parseColor("#F9C74F"), Color.parseColor("#F4A261"), R.drawable.img_res_banh_mi)
    )
}
