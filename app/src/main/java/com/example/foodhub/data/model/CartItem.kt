package com.example.foodhub.data.model

/**
 * Một dòng món ăn dùng chung cho Giỏ hàng và Đơn hàng.
 * price = giá 1 phần (đơn vị: đồng, kiểu số nguyên).
 */
data class CartItem(
    val foodId: Int,
    val name: String,
    val restaurant: String,
    val emoji: String,
    var price: Int,
    var quantity: Int,
    val imageRes: Int = 0,
    /** Ghi chú / tùy chọn của riêng dòng món này (vd: "Size L, ít đường, thêm topping"). */
    var note: String = "",
    val imageUri: String? = null
) {
    val total: Int
        get() = price * quantity
}
