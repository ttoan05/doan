package com.example.foodhub.ui.home

import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.FormatUtils

/**
 * Các tùy chọn sắp xếp danh sách tại màn hình Trang chủ FoodHub.
 */
enum class HomeSortOption(val id: String, val label: String) {
    POPULAR("popular", "Phổ biến"),
    BEST_SELLER("best_seller", "🔥 Bán chạy"),
    RATING("rating", "⭐ Đánh giá cao"),
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
        }
    }

    fun filterAndSortRestaurants(
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
            else -> list
        }
    }
}
