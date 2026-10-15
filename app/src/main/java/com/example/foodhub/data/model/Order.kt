package com.example.foodhub.data.model

enum class OrderStatus(val label: String) {
    PROCESSING("Đang xử lý"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy")
}

data class Order(
    val id: String,                 // ví dụ: "DH001"
    val restaurant: String,
    val createdAt: Long,            // thời gian đặt (millis)
    val items: List<CartItem>,
    val shippingFee: Int,
    val receiverName: String,
    val phone: String,
    val address: String,
    val paymentMethod: String,
    var status: OrderStatus,
    // Chỉ dùng khi status = PROCESSING:
    // 0 = Đã đặt hàng, 1 = Nhà hàng xác nhận, 2 = Đang chuẩn bị
    var processStep: Int = 0,
    /** Ghi chú chung của đơn gửi cho nhà hàng / tài xế. */
    val orderNote: String = "",
    /** Mã voucher đã áp dụng (để trống nếu không dùng). */
    val voucherCode: String = "",
    /** Số tiền đã giảm, phân bổ riêng cho đơn này (đồng). */
    val discount: Int = 0
) {
    val code: String
        get() = "#$id"

    val subtotal: Int
        get() = items.sumOf { it.total }

    val total: Int
        get() = (subtotal + shippingFee - discount).coerceAtLeast(0)

    val totalQuantity: Int
        get() = items.sumOf { it.quantity }

    /**
     * Bước hiện tại trên thanh tiến trình:
     * 0 Đã đặt hàng - 1 Nhà hàng xác nhận - 2 Đang chuẩn bị - 3 Đang giao - 4 Đã giao
     * Đơn đã hủy trả về -1.
     */
    fun progressStep(): Int = when (status) {
        OrderStatus.PROCESSING -> processStep.coerceIn(0, 2)
        OrderStatus.SHIPPING -> 3
        OrderStatus.DELIVERED -> 4
        OrderStatus.CANCELLED -> -1
    }

    // Chỉ cho hủy khi đơn chưa giao đi (đang xử lý).
    // Muốn cho hủy cả khi "Đang giao" thì đổi thành:
    // status == OrderStatus.PROCESSING || status == OrderStatus.SHIPPING
    fun canCancel(): Boolean = status == OrderStatus.PROCESSING

    /** Đơn đã giao thành công mới được đánh giá. */
    fun canReview(): Boolean = status == OrderStatus.DELIVERED

    fun canReorder(): Boolean = status == OrderStatus.DELIVERED
}
