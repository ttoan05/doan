package com.example.foodhub.data.model

/** Đánh giá của người dùng cho 1 đơn hàng đã giao thành công. */
data class Review(
    val id: Long = 0L,
    val orderId: String,
    val restaurant: String,
    /** Số sao 1..5. */
    val stars: Int,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
