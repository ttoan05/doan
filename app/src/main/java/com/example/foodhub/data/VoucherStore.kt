package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.Voucher
import com.example.foodhub.data.model.VoucherResult
import com.example.foodhub.data.model.VoucherType

/**
 * Kho voucher mẫu offline + logic tính giảm giá.
 * Đồng bộ chính xác theo thiết kế Ví Voucher FoodHub.
 */
object VoucherStore {

    /** Danh sách voucher mẫu hiển thị ở trang Voucher. */
    val VOUCHERS: List<Voucher> = listOf(
        Voucher(
            code = "FIRST30",
            title = "Giảm 30% đơn đầu tiên",
            desc = "Áp dụng cho đơn hàng đầu tiên. Giảm tối đa 50.000đ.",
            type = VoucherType.PERCENT,
            value = 30,
            maxDiscount = 50_000,
            minOrder = 0,
            emoji = "🎉"
        ),
        Voucher(
            code = "FREESHIP",
            title = "Miễn phí vận chuyển",
            desc = "Miễn phí ship cho đơn từ 30.000đ trở lên.\nĐơn từ 30.000đ",
            type = VoucherType.FREESHIP,
            minOrder = 30_000,
            emoji = "🚚"
        ),
        Voucher(
            code = "SAVE20",
            title = "Giảm 20% tất cả món",
            desc = "Giảm 20% toàn bộ món trong menu. Tối đa 30.000đ.\nĐơn từ 50.000đ",
            type = VoucherType.PERCENT,
            value = 20,
            maxDiscount = 30_000,
            minOrder = 50_000,
            emoji = "💰"
        ),
        Voucher(
            code = "WEEKEND",
            title = "Ưu đãi cuối tuần 15%",
            desc = "Chỉ áp dụng thứ 7 và Chủ nhật. Tối đa 25.000đ.\nĐơn từ 40.000đ",
            type = VoucherType.PERCENT,
            value = 15,
            maxDiscount = 25_000,
            minOrder = 40_000,
            emoji = "⭐"
        )
    )

    private const val PREFS = "voucher_prefs"

    /**
     * Kiểm tra mã người dùng nhập. `shippingForFreeship` chỉ dùng khi type = FREESHIP.
     * Trả về số tiền được giảm trên toàn giỏ (không âm).
     */
    fun validate(
        rawCode: String,
        subtotal: Int,
        shippingForFreeship: Int = 0
    ): VoucherResult {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) {
            return VoucherResult(false, message = "Vui lòng nhập mã voucher")
        }
        val voucher = VOUCHERS.find { it.code.uppercase() == code }
            ?: return VoucherResult(false, message = "Mã không tồn tại")
        if (subtotal < voucher.minOrder) {
            return VoucherResult(
                false,
                message = "Đơn tối thiểu ${voucher.minOrder / 1_000}k mới áp dụng được mã này"
            )
        }
        val discount: Int = when (voucher.type) {
            VoucherType.PERCENT -> {
                val raw = (subtotal * voucher.value) / 100
                if (voucher.maxDiscount > 0) minOf(raw, voucher.maxDiscount) else raw
            }
            VoucherType.AMOUNT -> minOf(voucher.value, subtotal)
            VoucherType.FREESHIP -> minOf(shippingForFreeship, shippingForFreeship)
        }
        if (discount <= 0 && voucher.type != VoucherType.FREESHIP) {
            return VoucherResult(false, message = "Mã không áp dụng được cho đơn này")
        }
        val displayDiscount = if (voucher.type == VoucherType.FREESHIP) {
            if (shippingForFreeship > 0) shippingForFreeship else 15_000
        } else {
            discount
        }
        return VoucherResult(
            success = true,
            discount = displayDiscount,
            message = "Áp dụng mã $code: giảm ${displayDiscount / 1_000}k",
            voucher = voucher
        )
    }

    /**
     * Tìm voucher theo mã chính xác (không phân biệt hoa thường). Trả null nếu không có.
     */
    fun find(code: String): Voucher? =
        VOUCHERS.find { it.code.equals(code.trim(), ignoreCase = true) }

    /** Lưu mã đã dùng (đánh dấu tick trong trang Voucher) cho tài khoản hiện tại. */
    fun markUsed(context: Context, code: String) {
        val uid = UserProfileManager.currentUserId
        prefs(context).edit().putBoolean(keyUsed(uid, code), true).apply()
    }

    fun isUsed(context: Context, code: String): Boolean {
        val uid = UserProfileManager.currentUserId
        return prefs(context).getBoolean(keyUsed(uid, code), false)
    }

    /**
     * Tổng số lượng voucher hiện có trong Ví Voucher FoodHub.
     */
    fun getTotalCount(): Int = VOUCHERS.size

    /**
     * Số lượng voucher còn khả dụng (chưa sử dụng) của tài khoản hiện tại.
     */
    fun getAvailableCount(context: Context): Int {
        return VOUCHERS.count { !isUsed(context, it.code) }
    }

    /**
     * Kiểm tra xem banner voucher chào mừng ("FIRST30") đã được bấm dùng hoặc đã sử dụng chưa.
     * Được lưu riêng biệt theo từng tài khoản (uid).
     */
    fun isWelcomeBannerDismissed(context: Context): Boolean {
        val uid = UserProfileManager.currentUserId
        val key = if (uid > 0L) "welcome_banner_dismissed_$uid" else "welcome_banner_dismissed_default"
        return prefs(context).getBoolean(key, false) || isUsed(context, "FIRST30")
    }

    /**
     * Đánh dấu tài khoản hiện tại đã bấm "DÙNG NGAY" voucher chào mừng.
     * Từ đây về sau sẽ không hiển thị banner chào mừng này nữa cho tài khoản này.
     */
    fun markWelcomeBannerDismissed(context: Context) {
        val uid = UserProfileManager.currentUserId
        val key = if (uid > 0L) "welcome_banner_dismissed_$uid" else "welcome_banner_dismissed_default"
        prefs(context).edit().putBoolean(key, true).apply()
    }

    private fun keyUsed(uid: Long, code: String) =
        "used_${uid}_${code.trim().uppercase()}"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
