package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.Order

/**
 * Quản lý danh sách thông báo lưu trữ thực tế trong CSDL SQLite.
 * Dữ liệu tách biệt theo từng user_id:
 * - Khi đặt hàng, đơn hàng đổi trạng thái -> thêm thông báo thật vào SQLite.
 * - Nhận voucher khuyến mãi thật lưu vào SQLite.
 * - Trạng thái ĐÃ ĐỌC (isRead) được lưu bền vững vào SQLite, thoát app vào lại không bị mất.
 */
object NotificationStore {

    // Giới hạn lưu thông báo: trong vòng 3 ngày (quá 3 ngày tự động xóa)
    private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000L

    private var appContext: Context? = null
    private var currentUserId: Long = -1L

    private val list = mutableListOf<AppNotification>()

    /** Màn hình đang hiển thị thông báo/badge gán callback này để được refresh. */
    var onChanged: (() -> Unit)? = null

    /** Được gọi khi có thông báo MỚI để hiện banner nhỏ. */
    var onNewNotification: ((AppNotification) -> Unit)? = null

    /**
     * Khởi tạo và nạp toàn bộ thông báo từ SQLite cho tài khoản đang đăng nhập.
     */
    fun init(context: Context, userId: Long) {
        appContext = context.applicationContext
        currentUserId = userId
        list.clear()

        if (userId > 0) {
            val db = DatabaseHelper.getInstance(context)
            db.cleanNotificationsOlderThan(3, userId)
            var notifs = db.getNotifications(userId)
            if (notifs.isEmpty()) {
                seedStarterNotifications(context, userId)
                notifs = db.getNotifications(userId)
            }
            list.addAll(notifs)
        }
        onChanged?.invoke()
    }

    /**
     * Đảm bảo thông báo đã được nạp đúng cho user hiện tại.
     */
    fun ensureLoaded(context: Context) {
        val targetUserId = UserProfileManager.currentUserId
        if (appContext == null || currentUserId != targetUserId) {
            init(context, targetUserId)
        }
    }

    // ─── Truy vấn ────────────────────────────────────────────────────────────
    fun getAll(type: NotificationType?): List<AppNotification> {
        val now = System.currentTimeMillis()
        val valid = list.filter { now - it.time <= THREE_DAYS_MS }
        val result = if (type == null) valid else valid.filter { it.type == type }
        return result.sortedWith(
            compareByDescending<AppNotification> { it.time }.thenByDescending { it.id }
        )
    }

    fun unreadCount(type: NotificationType? = null): Int {
        val now = System.currentTimeMillis()
        val valid = list.filter { now - it.time <= THREE_DAYS_MS }
        return valid.count { !it.isRead && (type == null || it.type == type) }
    }

    // ─── Thêm / sửa / xóa vào CSDL SQLite ────────────────────────────────────
    fun add(
        type: NotificationType,
        emoji: String,
        title: String,
        message: String,
        orderId: String? = null,
        promoCode: String? = null
    ) {
        val uid = if (currentUserId > 0) currentUserId else UserProfileManager.currentUserId
        val now = System.currentTimeMillis()
        val n = AppNotification(
            id = 0,
            type = type,
            emoji = emoji,
            title = title,
            message = message,
            time = now,
            isRead = false,
            orderId = orderId,
            promoCode = promoCode
        )

        val ctx = appContext
        if (ctx != null && uid > 0) {
            val newId = DatabaseHelper.getInstance(ctx).insertNotification(uid, n)
            n.id = newId
        } else {
            n.id = System.currentTimeMillis()
        }

        list.add(0, n)
        onChanged?.invoke()
        onNewNotification?.invoke(n)
    }

    /**
     * Đánh dấu đã đọc cho 1 thông báo và lưu ngay vào SQLite.
     */
    fun markRead(id: Long) {
        val n = list.find { it.id == id } ?: return
        if (n.isRead) return
        n.isRead = true

        val ctx = appContext
        val uid = currentUserId
        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).markNotificationAsRead(uid, id)
        }

        onChanged?.invoke()
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc và lưu vào SQLite.
     */
    fun markAllRead(type: NotificationType?) {
        var changed = false
        list.filter { type == null || it.type == type }.forEach {
            if (!it.isRead) {
                it.isRead = true
                changed = true
            }
        }
        if (changed) {
            val ctx = appContext
            val uid = currentUserId
            if (ctx != null && uid > 0) {
                DatabaseHelper.getInstance(ctx).markAllNotificationsAsRead(uid, type)
            }
            onChanged?.invoke()
        }
    }

    /**
     * Xóa 1 thông báo khỏi SQLite.
     */
    fun delete(id: Long) {
        val removed = list.removeAll { it.id == id }
        if (removed) {
            val ctx = appContext
            val uid = currentUserId
            if (ctx != null && uid > 0) {
                DatabaseHelper.getInstance(ctx).deleteNotification(uid, id)
            }
            onChanged?.invoke()
        }
    }

    /**
     * Xóa tất cả thông báo khỏi SQLite.
     */
    fun deleteAll(type: NotificationType?) {
        val removed = list.removeAll { type == null || it.type == type }
        if (removed) {
            val ctx = appContext
            val uid = currentUserId
            if (ctx != null && uid > 0) {
                DatabaseHelper.getInstance(ctx).deleteAllNotifications(uid, type)
            }
            onChanged?.invoke()
        }
    }

    fun clearMemory() {
        list.clear()
        currentUserId = -1L
    }

    // ─── Thông báo đơn hàng thực tế (do OrderManager gọi) ─────────────────────
    fun onOrderPlaced(order: Order) {
        add(
            NotificationType.ORDER, "✅", "Đặt hàng thành công",
            "Đơn ${order.code} tại ${order.restaurant} đã được gửi đi. Vui lòng chờ nhà hàng xác nhận nhé!",
            orderId = order.id
        )
    }

    fun onOrderProgress(order: Order) {
        val code = order.code
        val shop = order.restaurant
        when (order.progressStep()) {
            1 -> add(
                NotificationType.ORDER, "🧾", "Nhà hàng đã xác nhận",
                "$shop đã nhận đơn $code của bạn.", orderId = order.id
            )
            2 -> add(
                NotificationType.ORDER, "🍳", "Đang chuẩn bị món",
                "$shop đang chuẩn bị ${order.totalQuantity} món cho đơn $code.", orderId = order.id
            )
            3 -> add(
                NotificationType.ORDER, "🛵", "Đơn hàng đang được giao",
                "Tài xế đang trên đường giao đơn $code đến ${order.address}. Bạn chú ý điện thoại nhé!",
                orderId = order.id
            )
            4 -> add(
                NotificationType.ORDER, "🎉", "Giao hàng thành công",
                "Đơn $code đã được giao. Chúc bạn ngon miệng!", orderId = order.id
            )
        }
    }

    fun onOrderCancelled(order: Order) {
        add(
            NotificationType.ORDER, "❌", "Đã hủy đơn hàng",
            "Đơn ${order.code} tại ${order.restaurant} đã được hủy theo yêu cầu của bạn.",
            orderId = order.id
        )
    }

    // ─── Tạo voucher và thông báo khởi tạo ban đầu vào SQLite ─────────────────
    private fun seedStarterNotifications(context: Context, userId: Long) {
        val db = DatabaseHelper.getInstance(context)
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        if (userId == 1L) {
            // Tài khoản mẫu khớp hoàn toàn với giao diện thông báo
            val sampleList = listOf(
                AppNotification(
                    id = 0,
                    type = NotificationType.ORDER,
                    emoji = "🛵",
                    title = "Đơn hàng đang được giao",
                    message = "Tài xế đang trên đường giao đơn #DH002 đến 12 Nguyễn Huệ, Quận 1, TP.HCM. Bạn chú ý điện thoại nhé!",
                    time = now - 15 * minute,
                    isRead = false,
                    orderId = "DH002"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.ORDER,
                    emoji = "🧾",
                    title = "Nhà hàng đã xác nhận",
                    message = "Quán Phở Tuyệt đã nhận đơn #DH001 của bạn.",
                    time = now - 20 * minute,
                    isRead = false,
                    orderId = "DH001"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.ORDER,
                    emoji = "✅",
                    title = "Đặt hàng thành công",
                    message = "Đơn #DH001 tại Quán Phở Tuyệt đã được gửi đi. Vui lòng chờ nhà hàng xác nhận nhé!",
                    time = now - 30 * minute,
                    isRead = true,
                    orderId = "DH001"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.PROMO,
                    emoji = "🎁",
                    title = "Giảm 30% cho đơn đầu tiên",
                    message = "Nhập mã FIRST30 khi thanh toán để được giảm 30% cho đơn hàng đầu tiên của bạn.",
                    time = now - 2 * hour,
                    isRead = false,
                    promoCode = "FIRST30"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.PROMO,
                    emoji = "🚚",
                    title = "Freeship đơn từ 50.000đ",
                    message = "Miễn phí giao hàng cho mọi đơn từ 50.000đ. Đặt món ngay để không bỏ lỡ!",
                    time = now - 6 * hour,
                    isRead = false,
                    promoCode = "FREESHIP"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.PROMO,
                    emoji = "🍗",
                    title = "Ưu đãi 20% combo KFC",
                    message = "KFC Việt Nam giảm 20% cho các combo gà rán. Nhập mã COMBO20 để nhận ưu đãi.",
                    time = now - 2 * day,
                    isRead = true,
                    promoCode = "COMBO20"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.SYSTEM,
                    emoji = "👋",
                    title = "Chào mừng đến với FoodHub",
                    message = "Cảm ơn bạn đã đăng ký tài khoản. Khám phá các món ngon quanh bạn ngay nhé!",
                    time = now - 2 * day,
                    isRead = true
                )
            )
            sampleList.forEach { db.insertNotification(userId, it) }
        } else {
            // Người dùng đăng ký mới: Tặng sẵn các voucher khuyến mãi thực tế và lời chào
            val newUserList = listOf(
                AppNotification(
                    id = 0,
                    type = NotificationType.PROMO,
                    emoji = "🎁",
                    title = "Giảm 30% cho đơn đầu tiên",
                    message = "Nhập mã FIRST30 khi thanh toán để được giảm 30% cho đơn hàng đầu tiên của bạn.",
                    time = now - 10 * minute,
                    isRead = false,
                    promoCode = "FIRST30"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.PROMO,
                    emoji = "🚚",
                    title = "Freeship đơn từ 50.000đ",
                    message = "Miễn phí giao hàng cho mọi đơn từ 50.000đ. Đặt món ngay để không bỏ lỡ!",
                    time = now - 30 * minute,
                    isRead = false,
                    promoCode = "FREESHIP"
                ),
                AppNotification(
                    id = 0,
                    type = NotificationType.SYSTEM,
                    emoji = "👋",
                    title = "Chào mừng bạn đến với FoodHub",
                    message = "Cảm ơn bạn đã tham gia cùng chúng tôi. Khám phá các món ăn hấp dẫn ngay nhé!",
                    time = now - hour,
                    isRead = false
                )
            )
            newUserList.forEach { db.insertNotification(userId, it) }
        }
    }
}
