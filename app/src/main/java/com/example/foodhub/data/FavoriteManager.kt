package com.example.foodhub.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.SampleData

/**
 * Quản lý danh sách Yêu thích (món ăn + nhà hàng).
 *
 * Khác với CartManager/OrderManager (chỉ lưu trong bộ nhớ), danh sách này được lưu
 * bằng SharedPreferences nên tắt app mở lại vẫn còn. Cần gọi init(context) 1 lần
 * (MainActivity làm việc này ở onCreate).
 *
 * Mỗi mục lưu dạng "id:thời_điểm_thêm" để còn sắp xếp theo "Mới thêm gần nhất".
 */
object FavoriteManager {

    private const val PREFS_NAME = "foodhub_favorites"
    private const val KEY_FOODS = "foods"
    private const val KEY_RESTAURANTS = "restaurants"
    private const val KEY_PROMO_SENT = "promo_sent"

    // Sau khi thả tim 1 món, chờ chừng này rồi gửi thông báo "món yêu thích đang giảm giá"
    private const val PROMO_DELAY_MS = 20_000L

    private var prefs: SharedPreferences? = null
    private val foods = LinkedHashMap<Int, Long>()
    private val restaurants = LinkedHashMap<Int, Long>()

    // Những món đã gửi thông báo giảm giá rồi (mỗi món chỉ báo 1 lần)
    private val promoSent = mutableSetOf<Int>()
    private var promoPending = false
    private val handler = Handler(Looper.getMainLooper())

    /** Màn hình đang hiển thị yêu thích/badge gán callback này để được refresh. */
    var onChanged: (() -> Unit)? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        readInto(p.getStringSet(KEY_FOODS, null), foods)
        readInto(p.getStringSet(KEY_RESTAURANTS, null), restaurants)
        p.getStringSet(KEY_PROMO_SENT, null)?.forEach { s ->
            s.toIntOrNull()?.let { promoSent.add(it) }
        }
    }

    // ─── Truy vấn ──────────────────────────────────────
    fun isFood(id: Int): Boolean = foods.containsKey(id)
    fun isRestaurant(id: Int): Boolean = restaurants.containsKey(id)

    fun foodCount(): Int = foods.size
    fun restaurantCount(): Int = restaurants.size
    fun totalCount(): Int = foods.size + restaurants.size

    /** Thời điểm thêm (millis); 0 nếu chưa thích. */
    fun foodAddedAt(id: Int): Long = foods[id] ?: 0L
    fun restaurantAddedAt(id: Int): Long = restaurants[id] ?: 0L

    fun favoriteFoods(): List<Food> =
        SampleData.FOODS.filter { foods.containsKey(it.id) }

    fun favoriteRestaurants(): List<Restaurant> =
        SampleData.RESTAURANTS.filter { restaurants.containsKey(it.id) }

    // ─── Món ăn ────────────────────────────────────────
    /** Bấm tim: đang thích thì bỏ thích, chưa thích thì thêm. Trả về trạng thái MỚI. */
    fun toggleFood(food: Food): Boolean {
        return if (foods.containsKey(food.id)) {
            foods.remove(food.id)
            changed()
            false
        } else {
            foods[food.id] = System.currentTimeMillis()
            changed()
            schedulePromo(food)
            true
        }
    }

    /** Dùng cho "Hoàn tác": thêm lại đúng thời điểm cũ để giữ nguyên thứ tự. */
    fun restoreFood(id: Int, time: Long) {
        foods[id] = if (time > 0) time else System.currentTimeMillis()
        changed()
    }

    /** Xóa hết món yêu thích, trả về bản sao để có thể hoàn tác. */
    fun clearFoods(): Map<Int, Long> {
        val copy = LinkedHashMap(foods)
        foods.clear()
        changed()
        return copy
    }

    fun restoreFoods(backup: Map<Int, Long>) {
        foods.putAll(backup)
        changed()
    }

    // ─── Nhà hàng ──────────────────────────────────────
    fun toggleRestaurant(restaurant: Restaurant): Boolean {
        return if (restaurants.containsKey(restaurant.id)) {
            restaurants.remove(restaurant.id)
            changed()
            false
        } else {
            restaurants[restaurant.id] = System.currentTimeMillis()
            changed()
            true
        }
    }

    fun restoreRestaurant(id: Int, time: Long) {
        restaurants[id] = if (time > 0) time else System.currentTimeMillis()
        changed()
    }

    fun clearRestaurants(): Map<Int, Long> {
        val copy = LinkedHashMap(restaurants)
        restaurants.clear()
        changed()
        return copy
    }

    fun restoreRestaurants(backup: Map<Int, Long>) {
        restaurants.putAll(backup)
        changed()
    }

    // ─── Nội bộ ────────────────────────────────────────
    private fun changed() {
        save()
        onChanged?.invoke()
    }

    private fun readInto(set: Set<String>?, target: MutableMap<Int, Long>) {
        set?.forEach { s ->
            val parts = s.split(":")
            val id = parts.getOrNull(0)?.toIntOrNull()
            val time = parts.getOrNull(1)?.toLongOrNull()
            if (id != null && time != null) target[id] = time
        }
    }

    private fun save() {
        prefs?.edit()
            ?.putStringSet(KEY_FOODS, foods.map { "${it.key}:${it.value}" }.toSet())
            ?.putStringSet(KEY_RESTAURANTS, restaurants.map { "${it.key}:${it.value}" }.toSet())
            ?.putStringSet(KEY_PROMO_SENT, promoSent.map { it.toString() }.toSet())
            ?.apply()
    }

    /**
     * Thả tim 1 món -> sau vài giây app gửi thông báo "món yêu thích đang giảm giá"
     * (hiện trong tab Thông báo). Mỗi món chỉ báo 1 lần, và chỉ có 1 thông báo chờ tại 1 thời điểm
     * để khỏi spam khi thả tim liên tục.
     */
    private fun schedulePromo(food: Food) {
        if (promoPending || promoSent.contains(food.id)) return
        promoPending = true
        handler.postDelayed({
            promoPending = false
            // Nếu người dùng đã bỏ thích trong lúc chờ thì thôi
            if (foods.containsKey(food.id) && promoSent.add(food.id)) {
                save()
                NotificationStore.add(
                    NotificationType.PROMO, "❤️", "Món yêu thích đang giảm giá",
                    "${food.name} của ${food.restaurant} đang giảm 15% trong hôm nay. " +
                            "Nhập mã YEUTHICH15 để nhận ưu đãi!",
                    promoCode = "YEUTHICH15"
                )
            }
        }, PROMO_DELAY_MS)
    }
}
