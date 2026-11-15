package com.example.foodhub.ui.home

import android.content.Context
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.FormatUtils

/**
 * Các tùy chọn sắp xếp và gợi ý danh sách tại màn hình Trang chủ FoodHub.
 */
enum class HomeSortOption(val id: String, val label: String) {
    POPULAR("popular", "Phổ biến"),
    BEST_SELLER("best_seller", "🔥 Bán chạy"),
    RATING("rating", "⭐ Đánh giá cao"),
    ORDER_HISTORY("order_history", "🔄 Thường gọi"),
    FAVORITE_RESTAURANT("favorite_restaurant", "🏠 Quán quen"),
    PRICE_ASC("price_asc", "Giá: Thấp → Cao"),
    PRICE_DESC("price_desc", "Giá: Cao → Thấp"),
    NEARBY("nearby", "📍 Gần tôi nhất")
}

/**
 * Trạng thái bộ lọc và sắp xếp nâng cao hiện tại.
 */
data class HomeFilterState(
    var category: String = "Tất cả",
    var searchQuery: String = "",
    var sortOption: HomeSortOption = HomeSortOption.POPULAR,
    var filterFreeship: Boolean = false,
    var filterUnder35k: Boolean = false,
    var filterRating45: Boolean = false,
    var filterFastDelivery: Boolean = false
) {
    fun isFiltered(): Boolean {
        return category != "Tất cả" ||
                searchQuery.isNotBlank() ||
                sortOption != HomeSortOption.POPULAR ||
                filterFreeship ||
                filterUnder35k ||
                filterRating45 ||
                filterFastDelivery
    }

    fun reset() {
        category = "Tất cả"
        searchQuery = ""
        sortOption = HomeSortOption.POPULAR
        filterFreeship = false
        filterUnder35k = false
        filterRating45 = false
        filterFastDelivery = false
    }

    fun getActiveFilterSummary(): String {
        val parts = mutableListOf<String>()
        if (sortOption != HomeSortOption.POPULAR) {
            parts.add(sortOption.label)
        }
        if (filterFreeship) parts.add("Freeship")
        if (filterUnder35k) parts.add("≤ 35k")
        if (filterRating45) parts.add("⭐ ≥ 4.5")
        if (filterFastDelivery) parts.add("⚡ Giao nhanh")
        return parts.joinToString(" • ")
    }
}

/**
 * Helper chuyên biệt xử lý logic lọc đa tiêu chí và sắp xếp cho Trang chủ.
 */
object HomeFilterSortHelper {

    fun filterAndSortFoods(
        context: Context,
        allFoods: List<Food>,
        allRestaurants: List<Restaurant>,
        state: HomeFilterState
    ): List<Food> {
        var list = allFoods

        // 1. Lọc theo danh mục
        if (state.category != "Tất cả") {
            list = list.filter { it.category.trim().equals(state.category.trim(), ignoreCase = true) }
        }

        // 2. Lọc theo từ khóa tìm kiếm (hỗ trợ gõ 1-2 chữ cái, có dấu hoặc không dấu)
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery
            list = list.filter {
                FormatUtils.matchesSearch(it.name, q) || FormatUtils.matchesSearch(it.restaurant, q)
            }
        }

        // 3. Lọc nhanh: Giá sinh viên dưới 35k
        if (state.filterUnder35k) {
            list = list.filter { FormatUtils.parsePrice(it.price) <= 35000 }
        }

        // 4. Lọc nhanh: Đánh giá cao từ 4.5 trở lên
        if (state.filterRating45) {
            list = list.filter { it.rating >= 4.5 }
        }

        // 5. Lọc nhanh: Freeship
        if (state.filterFreeship) {
            val freeshipResNames = allRestaurants.filter {
                it.badge.contains("Freeship", ignoreCase = true) || it.desc.contains("Freeship", ignoreCase = true)
            }.map { it.name.lowercase() }
            list = list.filter { food ->
                freeshipResNames.any { food.restaurant.lowercase().contains(it) } ||
                        food.restaurant.contains("Phở Tuyệt", ignoreCase = true)
            }
        }

        // 6. Lọc nhanh: Giao hàng nhanh dưới 25 phút
        if (state.filterFastDelivery) {
            val fastResNames = allRestaurants.filter {
                val minTime = it.time.filter { c -> c.isDigit() || c == '-' }.split("-").firstOrNull()?.toIntOrNull() ?: 30
                minTime <= 20
            }.map { it.name.lowercase() }
            list = list.filter { food ->
                fastResNames.any { food.restaurant.lowercase().contains(it) }
            }
        }

        // 7. Sắp xếp danh sách
        return when (state.sortOption) {
            HomeSortOption.POPULAR -> list
            HomeSortOption.BEST_SELLER -> list.sortedByDescending { it.reviews }
            HomeSortOption.RATING -> list.sortedWith(compareByDescending<Food> { it.rating }.thenByDescending { it.reviews })
            HomeSortOption.PRICE_ASC -> list.sortedBy { FormatUtils.parsePrice(it.price) }
            HomeSortOption.PRICE_DESC -> list.sortedByDescending { FormatUtils.parsePrice(it.price) }
            HomeSortOption.NEARBY -> {
                val resDistMap = allRestaurants.associate {
                    it.name to (it.dist.replace("km", "").trim().toDoubleOrNull() ?: 99.0)
                }
                list.sortedBy { food -> resDistMap[food.restaurant] ?: 99.0 }
            }
            HomeSortOption.ORDER_HISTORY -> {
                // Cá nhân hóa 1: Gợi ý theo Lịch sử gọi món
                sortByOrderHistory(context, list)
            }
            HomeSortOption.FAVORITE_RESTAURANT -> {
                // Cá nhân hóa 2: Gợi ý theo Quán quen của khách
                sortByFavoriteRestaurant(context, list)
            }
        }
    }

    /**
     * Thuật toán cá nhân hóa 1: Đưa các món mà người dùng đã từng đặt (hoặc cùng danh mục hay gọi) lên đầu danh sách.
     */
    private fun sortByOrderHistory(context: Context, foods: List<Food>): List<Food> {
        val currentUid = UserProfileManager.currentUserId
        if (currentUid <= 0) {
            // Chưa đăng nhập hoặc user mới -> ưu tiên món bán chạy / đánh giá cao
            return foods.sortedWith(compareByDescending<Food> { it.reviews }.thenByDescending { it.rating })
        }

        val userOrders = DatabaseHelper.getInstance(context).getOrdersForUser(currentUid)
        if (userOrders.isEmpty()) {
            return foods.sortedWith(compareByDescending<Food> { it.reviews }.thenByDescending { it.rating })
        }

        // Đếm số lần từng món được người này đặt
        val foodOrderCount = mutableMapOf<String, Int>()
        val categoryOrderCount = mutableMapOf<String, Int>()

        userOrders.forEach { order ->
            order.items.forEach { item ->
                val foodNameKey = item.name.trim().lowercase()
                foodOrderCount[foodNameKey] = (foodOrderCount[foodNameKey] ?: 0) + item.quantity
            }
        }

        // Thống kê danh mục mà khách hay đặt dựa trên tên món đã đặt
        foods.forEach { food ->
            val count = foodOrderCount[food.name.trim().lowercase()] ?: 0
            if (count > 0 && food.category.isNotBlank()) {
                val catKey = food.category.trim().lowercase()
                categoryOrderCount[catKey] = (categoryOrderCount[catKey] ?: 0) + count
            }
        }

        // Tính điểm ưu tiên: Món đã từng đặt được +100 điểm * số lượng; Món cùng danh mục quen được +10 điểm; Đánh giá cao cộng thêm điểm phụ
        return foods.sortedWith(
            compareByDescending<Food> { food ->
                val orderedQty = foodOrderCount[food.name.trim().lowercase()] ?: 0
                val catPref = categoryOrderCount[food.category.trim().lowercase()] ?: 0
                (orderedQty * 100) + (catPref * 10)
            }.thenByDescending { it.rating }.thenByDescending { it.reviews }
        )
    }

    /**
     * Thuật toán cá nhân hóa 2: Đưa các món ăn thuộc Quán quen (nhà hàng mà khách đặt nhiều nhất) lên đầu danh sách.
     */
    private fun sortByFavoriteRestaurant(context: Context, foods: List<Food>): List<Food> {
        val currentUid = UserProfileManager.currentUserId
        if (currentUid <= 0) {
            return foods.sortedWith(compareByDescending<Food> { it.rating }.thenByDescending { it.reviews })
        }

        val userOrders = DatabaseHelper.getInstance(context).getOrdersForUser(currentUid)
        if (userOrders.isEmpty()) {
            return foods.sortedWith(compareByDescending<Food> { it.rating }.thenByDescending { it.reviews })
        }

        // Đếm số lần đặt theo từng nhà hàng
        val restaurantOrderCount = mutableMapOf<String, Int>()
        userOrders.forEach { order ->
            val res = order.restaurant.trim().lowercase()
            if (res.isNotBlank()) {
                restaurantOrderCount[res] = (restaurantOrderCount[res] ?: 0) + 1
            }
        }

        return foods.sortedWith(
            compareByDescending<Food> { food ->
                restaurantOrderCount[food.restaurant.trim().lowercase()] ?: 0
            }.thenByDescending { it.rating }.thenByDescending { it.reviews }
        )
    }

    fun filterAndSortRestaurants(
        context: Context,
        allRestaurants: List<Restaurant>,
        state: HomeFilterState
    ): List<Restaurant> {
        var list = allRestaurants

        // Lọc theo từ khóa tìm kiếm (hỗ trợ gõ 1-2 chữ cái, có dấu hoặc không dấu)
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery
            list = list.filter {
                FormatUtils.matchesSearch(it.name, q) || FormatUtils.matchesSearch(it.desc, q)
            }
        }

        // Lọc Freeship
        if (state.filterFreeship) {
            list = list.filter {
                it.badge.contains("Freeship", ignoreCase = true) || it.desc.contains("Freeship", ignoreCase = true)
            }
        }

        // Lọc Đánh giá 4.5+
        if (state.filterRating45) {
            list = list.filter { it.rating >= 4.5 }
        }

        // Lọc Giao nhanh
        if (state.filterFastDelivery) {
            list = list.filter {
                val minTime = it.time.filter { c -> c.isDigit() || c == '-' }.split("-").firstOrNull()?.toIntOrNull() ?: 30
                minTime <= 20
            }
        }

        // Sắp xếp
        return when (state.sortOption) {
            HomeSortOption.POPULAR -> list
            HomeSortOption.BEST_SELLER -> list.sortedByDescending { it.rating }
            HomeSortOption.RATING -> list.sortedByDescending { it.rating }
            HomeSortOption.NEARBY -> list.sortedBy { it.dist.replace("km", "").trim().toDoubleOrNull() ?: 99.0 }
            HomeSortOption.FAVORITE_RESTAURANT, HomeSortOption.ORDER_HISTORY -> {
                val currentUid = UserProfileManager.currentUserId
                if (currentUid <= 0) {
                    list.sortedByDescending { it.rating }
                } else {
                    val userOrders = DatabaseHelper.getInstance(context).getOrdersForUser(currentUid)
                    val countMap = mutableMapOf<String, Int>()
                    userOrders.forEach { order ->
                        val r = order.restaurant.trim().lowercase()
                        countMap[r] = (countMap[r] ?: 0) + 1
                    }
                    list.sortedWith(
                        compareByDescending<Restaurant> { countMap[it.name.trim().lowercase()] ?: 0 }
                            .thenByDescending { it.rating }
                    )
                }
            }
            else -> list
        }
    }
}
