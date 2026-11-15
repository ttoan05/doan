
package com.example.foodhub.data.model

/** Loại giảm giá của voucher. */
enum class VoucherType {
    /** Giảm theo % trên tạm tính, giới hạn bởi maxDiscount. */
    PERCENT,

    /** Giảm trực tiếp số tiền cố định. */
    AMOUNT,

    /** Miễn phí giao hàng (giảm đúng bằng phí ship). */
    FREESHIP
}

/** Voucher khuyến mãi dùng chung cho Admin và người dùng. */
data class Voucher(
    val code: String,
    val title: String,
    val desc: String,
    val type: VoucherType,
    /** PERCENT: % giảm. AMOUNT: số tiền giảm. FREESHIP: bỏ qua. */
    val value: Int = 0,
    /** Mức giảm tối đa khi type = PERCENT. */
    val maxDiscount: Int = 0,
    /** Giá trị đơn tối thiểu để được áp dụng. */
    val minOrder: Int = 0,
    val emoji: String = "🎟️",

    // Thông tin bổ sung phục vụ quản lý voucher Admin
    val id: Int = 0,
    val quantity: Int = 0,
    val remainingQuantity: Int = 0,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val isActive: Boolean = true
)

/** Kết quả khi người dùng nhập mã voucher ở Checkout. */
data class VoucherResult(
    val success: Boolean,
    val discount: Int = 0,
    val message: String = "",
    val voucher: Voucher? = null
)
