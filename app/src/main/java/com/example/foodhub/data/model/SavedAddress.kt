package com.example.foodhub.data.model

/** Địa chỉ giao hàng quen thuộc của người dùng, lưu theo từng user_id. */
data class SavedAddress(
    val id: Long = 0L,
    /** Nhãn gợi nhớ: "Nhà", "Công ty"... */
    val label: String,
    val receiver: String,
    val phone: String,
    val address: String,
    val isDefault: Boolean = false
) {
    /** Dòng hiển thị gọn trong Checkout: "Nhà • 12 Nguyễn Huệ, Q1". */
    val shortLine: String
        get() = "$label • $address"

    /** Dòng đầy đủ: người nhận + SĐT + địa chỉ. */
    val fullLine: String
        get() = "$receiver • $phone\n$address"
}
