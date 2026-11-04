package com.example.foodhub.data.model

/**
 * Model tin nhắn trao đổi giữa Khách hàng và Shipper (Tài xế giao hàng).
 */
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val orderId: String,
    val sender: String, // "CUSTOMER" hoặc "SHIPPER"
    val text: String,
    val time: Long = System.currentTimeMillis()
)
