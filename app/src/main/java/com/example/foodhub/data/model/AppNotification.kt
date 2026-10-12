package com.example.foodhub.data.model

/** Loại thông báo, dùng để lọc theo tab và tô màu icon. */
enum class NotificationType(val label: String, val iconBg: String) {
    ORDER("Đơn hàng", "#E0EAFF"),
    PROMO("Khuyến mãi", "#FDECEE"),
    SYSTEM("Hệ thống", "#EEEEEE")
}

data class AppNotification(
    var id: Long,
    val type: NotificationType,
    val emoji: String,              // icon hiển thị trong vòng tròn bên trái
    val title: String,
    val message: String,
    val time: Long,                 // thời điểm nhận (millis)
    var isRead: Boolean = false,
    val orderId: String? = null,    // có giá trị nếu là thông báo đơn hàng -> bấm vào mở chi tiết đơn
    val promoCode: String? = null   // có giá trị nếu là khuyến mãi có mã giảm giá
)
