package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.FormatUtils

/**
 * Quản lý giỏ hàng của ứng dụng FoodHub.
 * Dữ liệu được lưu trữ thật vào CSDL SQLite theo từng user_id riêng biệt.
 * Khi đổi tài khoản, giỏ hàng sẽ chuyển sang tài khoản tương ứng, không bị lẫn lộn.
 */
object CartManager {

    private var appContext: Context? = null
    private var currentUserId: Long = -1L

    private val items = mutableListOf<CartItem>()

    /**
     * Khởi tạo giỏ hàng cho tài khoản đang đăng nhập từ SQLite.
     */
    fun init(context: Context, userId: Long) {
        appContext = context.applicationContext
        currentUserId = userId
        items.clear()
        if (userId > 0) {
            val db = DatabaseHelper.getInstance(context)
            items.addAll(db.getCartItems(userId))
        }
    }

    /**
     * Đảm bảo giỏ hàng đã được nạp cho user hiện tại.
     */
    fun ensureLoaded(context: Context) {
        val targetUserId = UserProfileManager.currentUserId
        if (appContext == null || currentUserId != targetUserId) {
            init(context, targetUserId)
        }
    }

    fun getItems(): List<CartItem> = items

    fun totalQuantity(): Int = items.sumOf { it.quantity }

    fun totalPrice(): Int = items.sumOf { it.total }

    fun isEmpty(): Boolean = items.isEmpty()

    fun addFood(food: Food) {
        val existing = items.find { it.foodId == food.id }
        if (existing != null) {
            existing.quantity++
            syncItemToDb(existing)
        } else {
            val newItem = CartItem(
                foodId = food.id,
                name = food.name,
                restaurant = food.restaurant,
                emoji = food.emoji,
                price = FormatUtils.parsePrice(food.price),
                quantity = 1,
                imageRes = food.imageRes
            )
            items.add(newItem)
            syncItemToDb(newItem)
        }
    }

    /** Dùng cho "Đặt lại": thêm toàn bộ món của đơn cũ vào giỏ và lưu SQLite. */
    fun addItems(list: List<CartItem>) {
        list.forEach { old ->
            val existing = items.find { it.foodId == old.foodId }
            if (existing != null) {
                existing.quantity += old.quantity
                syncItemToDb(existing)
            } else {
                val copy = old.copy()
                items.add(copy)
                syncItemToDb(copy)
            }
        }
    }

    fun increase(item: CartItem) {
        item.quantity++
        syncItemToDb(item)
    }

    fun decrease(item: CartItem) {
        if (item.quantity > 1) {
            item.quantity--
            syncItemToDb(item)
        }
    }

    fun remove(item: CartItem) {
        items.remove(item)
        val ctx = appContext
        val uid = currentUserId
        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).removeCartItem(uid, item.foodId)
        }
    }

    /**
     * Xóa toàn bộ giỏ hàng (sau khi người dùng bấm Mua / Đặt hàng thành công).
     */
    fun clear() {
        items.clear()
        val ctx = appContext
        val uid = currentUserId
        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).clearCart(uid)
        }
    }

    /**
     * Xóa giỏ hàng trên RAM khi người dùng Đăng xuất (vẫn giữ dữ liệu trong SQLite).
     */
    fun clearMemory() {
        items.clear()
        currentUserId = -1L
    }

    private fun syncItemToDb(item: CartItem) {
        val ctx = appContext
        val uid = if (currentUserId > 0) currentUserId else UserProfileManager.currentUserId
        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).saveOrUpdateCartItem(uid, item)
        }
    }
}
