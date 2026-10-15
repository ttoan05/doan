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

/** Voucher khuyến mãi dùng offline (demo), lưu trong code mẫu. */
data class Voucher(
    val code: String,
    val title: String,
    val desc: String,
    val type: VoucherType,
    /** PERCENT: % giảm (vd 30). AMOUNT: số tiền giảm. FREESHIP: bỏ qua. */
    val value: Int = 0,
    /** Mức giảm tối đa (đồng) khi type = PERCENT. 0 = không giới hạn. */
    val maxDiscount: Int = 0,
    /** Giá trị đơn tối thiểu (tạm tính, đồng) để được áp dụng. */
    val minOrder: Int = 0,
    val emoji: String = "🎟️"
)

/** Kết quả khi người dùng nhập mã voucher ở Checkout. */
data class VoucherResult(
    val success: Boolean,
    /** Số tiền được giảm trên toàn giỏ (đồng). */
    val discount: Int = 0,
    val message: String = "",
    val voucher: Voucher? = null
)
