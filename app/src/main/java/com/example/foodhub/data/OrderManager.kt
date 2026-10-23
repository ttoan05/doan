package com.example.foodhub.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus

/**
 * Quản lý danh sách đơn hàng thực tế lưu trong CSDL SQLite.
 * Tách biệt theo từng user_id: Mỗi tài khoản chỉ thấy đơn hàng của chính mình.
 */
object OrderManager {

    // Tắt tự động nhảy bước để đơn hàng chờ Admin duyệt trong AdminOrderActivity
    private const val AUTO_ADVANCE = false
    private const val STEP_DELAY_MS = 15_000L

    private const val FREE_SHIP_FROM = 50_000
    private const val SHIP_FEE = 15_000

    private var appContext: Context? = null
    private var currentUserId: Long = -1L

    private val orders = mutableListOf<Order>()
    private val handler = Handler(Looper.getMainLooper())

    var onChanged: (() -> Unit)? = null

    /**
     * Khởi tạo đơn hàng từ CSDL SQLite cho tài khoản đang đăng nhập.
     */
    fun init(context: Context, userId: Long) {
        appContext = context.applicationContext
        currentUserId = userId
        orders.clear()

        if (userId > 0) {
            val db = DatabaseHelper.getInstance(context)
            val dbOrders = db.getOrdersForUser(userId)
            if (dbOrders.isNotEmpty()) {
                orders.addAll(dbOrders)
            } else if (userId == 1L) {
                // Tài khoản mẫu mặc định (ID 1) nếu chưa có đơn thì tạo sẵn vài đơn mẫu
                seedSampleOrders(context, userId)
            }
        }
        onChanged?.invoke()
    }

    /**
     * Đảm bảo đơn hàng đã được nạp cho user hiện tại.
     */
    fun ensureLoaded(context: Context) {
        appContext = context.applicationContext
        val targetUserId = if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L
        if (currentUserId != targetUserId || orders.isEmpty()) {
            init(context, targetUserId)
        }
    }

    /**
     * Cập nhật trạng thái đơn hàng trong RAM khi Admin hoặc hệ thống thay đổi trạng thái.
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus, step: Int) {
        val order = findById(orderId)
        if (order != null) {
            order.status = status
            order.processStep = step
            NotificationStore.onOrderProgress(order)
            onChanged?.invoke()
        }
    }

    /**
     * Lấy toàn bộ lịch sử đơn hàng (không giới hạn số ngày để phục vụ việc xem lại và đặt lại món).
     */
    fun getOrders(status: OrderStatus?): List<Order> {
        val list = if (status == null) orders else orders.filter { it.status == status }
        return list.sortedByDescending { it.createdAt }
    }

    fun countOrders(status: OrderStatus?): Int {
        return if (status == null) orders.size else orders.count { it.status == status }
    }

    fun findById(id: String): Order? = orders.find { it.id == id }

    fun shippingFeeFor(subtotal: Int): Int =
        if (subtotal >= FREE_SHIP_FROM) 0 else SHIP_FEE

    /**
     * Tạo đơn hàng thật khi người dùng bấm "Mua / Xác nhận đặt hàng".
     * Lưu trữ vĩnh viễn vào SQLite cho tài khoản hiện tại.
     */
    fun createOrders(
        cartItems: List<CartItem>,
        receiverName: String,
        phone: String,
        address: String,
        paymentMethod: String,
        orderNote: String = "",
        voucherCode: String = "",
        discount: Int = 0,
        context: Context? = null
    ): List<Order> {
        val created = mutableListOf<Order>()
        val now = System.currentTimeMillis()
        if (context != null) {
            appContext = context.applicationContext
        }
        val ctx = appContext
        val uid = if (currentUserId > 0) currentUserId else (if (UserProfileManager.currentUserId > 0) UserProfileManager.currentUserId else 1L)

        val grouped = cartItems.groupBy { it.restaurant }
        var remainingDiscount = discount

        grouped.forEach { (restaurant, list) ->
            val copies = list.map { it.copy() }
            val subtotal = copies.sumOf { it.total }
            val orderId = generateOrderId()

            val orderDiscount = if (grouped.size == 1) {
                discount
            } else {
                val share = minOf(remainingDiscount, subtotal)
                remainingDiscount -= share
                share
            }

            val order = Order(
                id = orderId,
                restaurant = restaurant,
                createdAt = now,
                items = copies,
                shippingFee = shippingFeeFor(subtotal),
                receiverName = receiverName,
                phone = phone,
                address = address,
                paymentMethod = paymentMethod,
                status = OrderStatus.PROCESSING,
                processStep = 0,
                orderNote = orderNote,
                voucherCode = if (orderDiscount > 0 || voucherCode.isNotEmpty()) voucherCode else "",
                discount = orderDiscount
            )

            orders.add(0, order)
            created.add(order)

            // Lưu trực tiếp vào SQLite
            if (ctx != null) {
                DatabaseHelper.getInstance(ctx).insertOrder(uid, order)
            }

            NotificationStore.onOrderPlaced(order)
            if (AUTO_ADVANCE) scheduleAdvance(order)
        }

        onChanged?.invoke()
        return created
    }

    fun cancel(orderId: String): Boolean {
        val order = findById(orderId) ?: return false
        if (!order.canCancel()) return false
        order.status = OrderStatus.CANCELLED
        order.processStep = -1

        val ctx = appContext
        if (ctx != null) {
            DatabaseHelper.getInstance(ctx).cancelOrder(orderId)
        }

        NotificationStore.onOrderCancelled(order)
        onChanged?.invoke()
        return true
    }

    fun clearMemory() {
        orders.clear()
        currentUserId = -1L
    }

    private fun generateOrderId(): String {
        val randomSuffix = (100..999).random()
        val timeSuffix = (System.currentTimeMillis() % 100000).toString().padStart(5, '0')
        return "DH$timeSuffix$randomSuffix"
    }

    private fun scheduleAdvance(order: Order) {
        handler.postDelayed({
            when (order.status) {
                OrderStatus.PROCESSING -> {
                    if (order.processStep < 2) order.processStep++
                    else order.status = OrderStatus.SHIPPING
                }
                OrderStatus.SHIPPING -> order.status = OrderStatus.DELIVERED
                else -> return@postDelayed
            }

            // Đồng bộ trạng thái mới vào CSDL SQLite
            val ctx = appContext
            if (ctx != null) {
                DatabaseHelper.getInstance(ctx).updateOrderStatus(order.id, order.status, order.processStep)
            }

            NotificationStore.onOrderProgress(order)
            onChanged?.invoke()
            if (order.status != OrderStatus.DELIVERED) scheduleAdvance(order)
        }, STEP_DELAY_MS)
    }

    private fun seedSampleOrders(context: Context, userId: Long) {
        val now = System.currentTimeMillis()
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        val name = "Trương Thanh Toàn"
        val phone = "+84 123 456 789"
        val address = "12 Nguyễn Huệ, Quận 1, TP.HCM"

        fun add(
            restaurant: String, time: Long, items: List<CartItem>,
            payment: String, status: OrderStatus, step: Int = 0
        ) {
            val subtotal = items.sumOf { it.total }
            val order = Order(
                id = generateOrderId(),
                restaurant = restaurant,
                createdAt = time,
                items = items,
                shippingFee = shippingFeeFor(subtotal),
                receiverName = name,
                phone = phone,
                address = address,
                paymentMethod = payment,
                status = status,
                processStep = step
            )
            orders.add(order)
            DatabaseHelper.getInstance(context).insertOrder(userId, order)
        }

        add(
            "Quán Phở Tuyệt", now - 30 * minute,
            listOf(CartItem(1, "Phở bò đặc biệt", "Quán Phở Tuyệt", "🍲", 45_000, 2)),
            "Thanh toán khi nhận hàng (COD)", OrderStatus.PROCESSING, 1
        )
        add(
            "KFC Việt Nam", now - hour,
            listOf(CartItem(2, "Gà rán giòn cay", "KFC Việt Nam", "🍗", 55_000, 2)),
            "Ví MoMo", OrderStatus.SHIPPING
        )
        add(
            "Bánh mì Sài Gòn", now - day,
            listOf(
                CartItem(3, "Bánh mì thập cẩm", "Bánh mì Sài Gòn", "🥖", 25_000, 2),
                CartItem(6, "Nước chanh đá", "Bánh mì Sài Gòn", "🍋", 10_000, 1)
            ),
            "Thanh toán khi nhận hàng (COD)", OrderStatus.DELIVERED
        )
        add(
            "Cơm tấm Quỳnh", now - 2 * day,
            listOf(CartItem(4, "Cơm tấm sườn nướng", "Cơm tấm Quỳnh", "🍛", 35_000, 1)),
            "Thanh toán khi nhận hàng (COD)", OrderStatus.CANCELLED, -1
        )
    }
}
