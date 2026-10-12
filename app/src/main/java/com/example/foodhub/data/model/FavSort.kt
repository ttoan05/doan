package com.example.foodhub.data.model

/** Các kiểu sắp xếp trong trang Yêu thích. */
enum class FavSort(val label: String) {
    NEWEST("Mới thêm gần nhất"),
    PRICE_ASC("Giá thấp → cao"),
    PRICE_DESC("Giá cao → thấp"),
    RATING("Đánh giá cao nhất"),
    NAME("Tên A → Z")
}
