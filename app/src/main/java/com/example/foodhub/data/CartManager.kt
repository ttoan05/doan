package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.FormatUtils

/**
 * Quản lý giỏ hàng của ứng dụng FoodHub.
 * Dữ liệu được lưu trữ thật vào CSDL SQLite theo từng user_id riêng biệt.
 * Cho phép bỏ nhiều món hàng khác nhau vào giỏ hàng.
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

    /**
     * Trả về tên nhà hàng của món hiện có trong giỏ hàng (nếu có).
     */
    fun getActiveRestaurant(): String? {
        return items.firstOrNull { it.restaurant.isNotBlank() }?.restaurant
    }

    /**
     * Kiểm tra xem món sắp thêm có phải từ quán khác với quán đang có trong giỏ hay không.
     */
    fun isDifferentRestaurant(targetRestaurant: String): Boolean {
        if (items.isEmpty()) return false
        val currentRest = getActiveRestaurant() ?: return false
        val newRest = targetRestaurant.trim()
        if (newRest.isEmpty()) return false
        return !currentRest.equals(newRest, ignoreCase = true)
    }

    fun addFood(food: Food, quantity: Int = 1, note: String = "", customUnitPrice: Int? = null) {
        val finalPrice = customUnitPrice ?: FormatUtils.parsePrice(food.price)
        val cleanNote = if (note.isBlank() && customUnitPrice == null) "Chưa chọn size" else note.trim()
        val existing = items.find { it.foodId == food.id && it.note == cleanNote && it.price == finalPrice }
        if (existing != null) {
            existing.quantity += quantity
            syncItemToDb(existing)
        } else {
            val newItem = CartItem(
                foodId = food.id,
                name = food.name,
                restaurant = food.restaurant,
                emoji = food.emoji,
                price = finalPrice,
                quantity = quantity,
                imageRes = food.imageRes,
                note = cleanNote,
                imageUri = food.imageUri
            )
            items.add(newItem)
            syncItemToDb(newItem)
        }
    }

    /**
     * Cập nhật kích cỡ và đơn giá mới cho món trong giỏ hàng.
     */
    fun updateItemSizeAndPrice(item: CartItem, newSizeName: String, newPrice: Int) {
        val oldNote = item.note

        val finalNote = if (oldNote.isBlank() || oldNote.equals("Chưa chọn size", ignoreCase = true)) {
            newSizeName
        } else {
            val parts = oldNote.split(" • ").toMutableList()
            if (parts.isNotEmpty() && (parts[0].contains("Size") || parts[0].contains("Tô") || parts[0].contains("Dĩa") || parts[0].contains("Ổ") || parts[0].contains("Phần") || parts[0].contains("Tiêu chuẩn") || parts[0].contains("Lớn") || parts[0].contains("Đặc biệt"))) {
                parts[0] = newSizeName
            } else {
                parts.add(0, newSizeName)
            }
            parts.joinToString(" • ")
        }

        val ctx = appContext
        val uid = if (currentUserId > 0) currentUserId else UserProfileManager.currentUserId
        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).removeCartItem(uid, item.foodId, oldNote)
        }

        item.price = newPrice
        item.note = finalNote

        if (ctx != null && uid > 0) {
            DatabaseHelper.getInstance(ctx).saveOrUpdateCartItem(uid, item)
        }
    }

    /** Dùng cho "Đặt lại": thêm toàn bộ món của đơn cũ vào giỏ và lưu SQLite. */
    fun addItems(list: List<CartItem>) {
        list.forEach { old ->
            val existing = items.find { it.foodId == old.foodId && it.note == old.note && it.price == old.price }
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
            DatabaseHelper.getInstance(ctx).removeCartItem(uid, item.foodId, item.note)
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
