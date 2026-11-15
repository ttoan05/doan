package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.Voucher
import com.example.foodhub.data.model.VoucherResult
import com.example.foodhub.data.model.VoucherType
import com.example.foodhub.data.dao.VoucherDao
import com.example.foodhub.data.UserProfileManager
/**
 * Kho voucher mẫu offline + logic tính giảm giá.
 * Đồng bộ chính xác theo thiết kế Ví Voucher FoodHub.
 */
object VoucherStore {

    /** Danh sách voucher mẫu hiển thị ở trang Voucher. */
    val VOUCHERS: List<Voucher> = listOf(
        Voucher(
            code = "FIRST30",
            title = "Giảm 30k đơn đầu tiên",
            desc = "Áp dụng cho đơn hàng đầu tiên. Giảm ngay 30.000đ.",
            type = VoucherType.AMOUNT,
            value = 30_000,
            maxDiscount = 30_000,
            minOrder = 0,
            emoji = "🎉",
            quantity = 9999,
            remainingQuantity = 9999,
            isActive = true
        ),
        Voucher(
            code = "FREESHIP",
            title = "Miễn phí vận chuyển",
            desc = "Miễn phí ship cho đơn từ 30.000đ trở lên.\nĐơn từ 30.000đ",
            type = VoucherType.FREESHIP,
            minOrder = 30_000,
            emoji = "🚚",
            quantity = 9999,
            remainingQuantity = 9999,
            isActive = true
        ),
        Voucher(
            code = "SAVE20",
            title = "Giảm 20% tất cả món",
            desc = "Giảm 20% toàn bộ món trong menu. Tối đa 30.000đ.\nĐơn từ 50.000đ",
            type = VoucherType.PERCENT,
            value = 20,
            maxDiscount = 30_000,
            minOrder = 50_000,
            emoji = "💰",
            quantity = 9999,
            remainingQuantity = 9999,
            isActive = true
        ),
        Voucher(
            code = "WEEKEND",
            title = "Ưu đãi cuối tuần 15%",
            desc = "Chỉ áp dụng thứ 7 và Chủ nhật. Tối đa 25.000đ.\nĐơn từ 40.000đ",
            type = VoucherType.PERCENT,
            value = 15,
            maxDiscount = 25_000,
            minOrder = 40_000,
            emoji = "⭐",
            quantity = 9999,
            remainingQuantity = 9999,
            isActive = true
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
        shippingForFreeship: Int = 0,
        context: Context? = null
    ): VoucherResult {

        val code = rawCode.trim().uppercase()

        if (code.isEmpty()) {
            return VoucherResult(false, message = "Vui lòng nhập mã voucher")
        }

        if (subtotal <= 0) {
            return VoucherResult(false, message = "Giỏ hàng không hợp lệ")
        }

        if (context == null) {
            return VoucherResult(false, message = "Không thể kiểm tra voucher")
        }

        // 1. Tìm voucher trong SQLite
        val dbHelper = DatabaseHelper.getInstance(context)
        val voucher = dbHelper.getVoucherByCode(code)
            ?: return VoucherResult(false, message = "Mã voucher không tồn tại")

        // 2. Kiểm tra trạng thái
        if (!voucher.isActive) {
            return VoucherResult(false, message = "Voucher hiện không hoạt động")
        }

        // 3. Kiểm tra thời hạn
        val now = System.currentTimeMillis()

        if (voucher.startAt > 0L && now < voucher.startAt) {
            return VoucherResult(false, message = "Voucher chưa đến thời gian áp dụng")
        }

        if (voucher.endAt > 0L && now > voucher.endAt) {
            return VoucherResult(false, message = "Voucher đã hết hạn")
        }

        // 4. Kiểm tra tài khoản hiện tại đã sử dụng chưa
        val userId = if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L

        if (isUsed(context, code) || dbHelper.hasUserUsedVoucher(voucher.id, userId.toInt())) {
            return VoucherResult(false, message = "Bạn đã sử dụng mã voucher này rồi")
        }

        // 6. Kiểm tra đơn hàng tối thiểu
        if (subtotal < voucher.minOrder) {
            return VoucherResult(
                false,
                message = "Đơn hàng tối thiểu ${voucher.minOrder / 1000}k mới áp dụng được mã này"
            )
        }

        // 7. Tính tiền giảm
        val discount = when (voucher.type) {

            VoucherType.PERCENT -> {
                val rawDiscount = (subtotal.toLong() * voucher.value / 100).toInt()

                if (voucher.maxDiscount > 0) {
                    minOf(rawDiscount, voucher.maxDiscount)
                } else {
                    rawDiscount
                }
            }

            VoucherType.AMOUNT -> {
                minOf(voucher.value, subtotal)
            }

            VoucherType.FREESHIP -> {
                if (shippingForFreeship <= 0) {
                    return VoucherResult(
                        false,
                        message = "Không xác định được phí vận chuyển"
                    )
                }

                shippingForFreeship
            }
        }

        if (discount <= 0) {
            return VoucherResult(
                false,
                message = "Mã voucher không áp dụng được cho đơn hàng này"
            )
        }

        // 8. Trả kết quả
        return VoucherResult(
            success = true,
            discount = discount,
            message = "Áp dụng mã $code: giảm ${discount / 1000}k",
            voucher = voucher
        )
    }


    /**
     * Tìm voucher theo mã chính xác (không phân biệt hoa thường). Trả null nếu không có.
     */
    fun find(code: String, context: Context): Voucher? {
        if (code.isBlank()) return null

        return DatabaseHelper.getInstance(context)
            .getVoucherByCode(code.trim())
    }

    /** Lưu mã đã dùng (đánh dấu đã sử dụng) cho tài khoản hiện tại. */
    /**
     * Đánh dấu voucher đã sử dụng cho tài khoản hiện tại.
     * Lưu đồng thời vào cả SharedPreferences (đảm bảo tức thì theo UID) và SQLite.
     */
    fun markUsed(context: Context, code: String): Boolean {
        val uid = if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L
        if (code.isBlank()) return false

        val cleanCode = code.trim().uppercase()

        // 1. Lưu ngay vào SharedPreferences theo uid của tài khoản hiện tại
        try {
            prefs(context).edit().putBoolean(keyUsed(uid, cleanCode), true).apply()
        } catch (_: Exception) {}

        // 2. Lưu vào SQLite bảng voucher_usage
        try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val voucher = dbHelper.getVoucherByCode(cleanCode)
            if (voucher != null) {
                dbHelper.markVoucherAsUsed(voucher.id, uid.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return true
    }

    /**
     * Kiểm tra xem tài khoản hiện tại đã dùng voucher này chưa.
     */
    fun isUsed(context: Context, code: String): Boolean {
        val uid = if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L
        if (code.isBlank()) return false

        val cleanCode = code.trim().uppercase()

        // 1. Kiểm tra SharedPreferences theo uid
        try {
            if (prefs(context).getBoolean(keyUsed(uid, cleanCode), false)) {
                return true
            }
        } catch (_: Exception) {}

        // 2. Kiểm tra SQLite
        return try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val voucher = dbHelper.getVoucherByCode(cleanCode) ?: return false
            dbHelper.hasUserUsedVoucher(voucher.id, uid.toInt())
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tổng số lượng voucher hiện có trong Ví Voucher FoodHub.
     */
    fun getTotalCount(): Int = VOUCHERS.size

    /**
     * Số lượng voucher còn khả dụng (chưa sử dụng) của tài khoản hiện tại.
     */
    fun getAvailableCount(context: Context): Int {
        return getAvailableVouchers(context).size
    }

    /**
     * Lấy danh sách voucher còn khả dụng (chưa dùng) của tài khoản hiện tại.
     */
    fun getAvailableVouchers(context: Context): List<Voucher> {
        val uid = if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L
        val dbHelper = DatabaseHelper.getInstance(context)
        val now = System.currentTimeMillis()

        return dbHelper.getAllVouchers().filter { voucher ->
            val isInTime =
                (voucher.startAt <= 0L || now >= voucher.startAt) &&
                        (voucher.endAt <= 0L || now <= voucher.endAt)

            voucher.isActive &&
                    isInTime &&
                    !isUsed(context, voucher.code) &&
                    !dbHelper.hasUserUsedVoucher(voucher.id, uid.toInt())
        }
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
