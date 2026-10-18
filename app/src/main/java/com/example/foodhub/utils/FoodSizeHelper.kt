package com.example.foodhub.utils

import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Food
import org.json.JSONArray
import org.json.JSONObject

object FoodSizeHelper {

    data class SizeOption(
        val name: String,
        val priceDelta: Int,
        val fullPrice: Int
    )

    data class AdminSize(
        val name: String,
        val priceDelta: Int
    )

    data class AdminTopping(
        val name: String,
        val price: Int
    )

    fun parseSizesJson(json: String?): List<AdminSize> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<AdminSize>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                val priceDelta = obj.optInt("priceDelta", 0)
                if (name.isNotEmpty()) {
                    list.add(AdminSize(name, priceDelta))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun sizesToJson(list: List<AdminSize>): String {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("name", it.name.trim())
            obj.put("priceDelta", it.priceDelta)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseToppingsJson(json: String?): List<AdminTopping> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<AdminTopping>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                val price = obj.optInt("price", 0)
                if (name.isNotEmpty()) {
                    list.add(AdminTopping(name, price))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun toppingsToJson(list: List<AdminTopping>): String {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("name", it.name.trim())
            obj.put("price", it.price)
            arr.put(obj)
        }
        return arr.toString()
    }

    /**
     * Danh sách kích cỡ mặc định theo từng nhóm món nếu Admin chưa cài đặt riêng.
     */
    fun getDefaultSizes(foodName: String, category: String): List<AdminSize> {
        val lowerName = foodName.lowercase()
        val lowerCat = category.lowercase()

        return when {
            lowerCat.contains("uống") || lowerName.contains("chanh") || lowerName.contains("trà") || lowerName.contains("cà phê") -> {
                listOf(
                    AdminSize("Size Vừa (M)", 0),
                    AdminSize("Size Lớn (L)", 5000)
                )
            }
            lowerCat.contains("bánh mì") || lowerName.contains("bánh mì") -> {
                listOf(
                    AdminSize("Ổ Tiêu chuẩn", 0),
                    AdminSize("Ổ Đặc biệt (Nhiều nhân)", 8000)
                )
            }
            lowerCat.contains("cơm") || lowerName.contains("cơm") -> {
                listOf(
                    AdminSize("Dĩa Tiêu chuẩn", 0),
                    AdminSize("Dĩa Lớn (Thêm cơm & thịt)", 12000)
                )
            }
            lowerCat.contains("phở") || lowerCat.contains("mì") || lowerName.contains("phở") || lowerName.contains("mì") -> {
                listOf(
                    AdminSize("Tô Tiêu chuẩn", 0),
                    AdminSize("Tô Lớn Đặc biệt", 10000)
                )
            }
            lowerCat.contains("gà") || lowerName.contains("gà") -> {
                listOf(
                    AdminSize("Phần Tiêu chuẩn", 0),
                    AdminSize("Phần Lớn Siêu giòn", 15000)
                )
            }
            else -> {
                listOf(
                    AdminSize("Size Tiêu chuẩn (Vừa)", 0),
                    AdminSize("Size Lớn (Nhiều hơn)", 10000)
                )
            }
        }
    }

    /**
     * Danh sách topping mặc định theo từng nhóm món nếu Admin chưa cài đặt riêng.
     */
    fun getDefaultToppings(foodName: String, category: String): List<AdminTopping> {
        val lowerName = foodName.lowercase()
        val lowerCat = category.lowercase()
        return when {
            lowerCat.contains("uống") || lowerName.contains("chanh") || lowerName.contains("trà") || lowerName.contains("cà phê") -> {
                listOf(
                    AdminTopping("Trân châu đen dẻo", 6000),
                    AdminTopping("Thạch nha đam giòn", 6000),
                    AdminTopping("Kem cheese béo ngậy", 10000),
                    AdminTopping("Thêm chanh & mật ong", 5000)
                )
            }
            lowerCat.contains("cơm") || lowerName.contains("cơm") -> {
                listOf(
                    AdminTopping("Trứng ốp la lòng đào", 7000),
                    AdminTopping("Chả trứng hấp thịt", 8000),
                    AdminTopping("Thêm sườn nướng mật ong", 18000),
                    AdminTopping("Chén canh súp rong biển", 6000)
                )
            }
            lowerCat.contains("phở") || lowerCat.contains("mì") || lowerName.contains("phở") || lowerName.contains("mì") || lowerName.contains("bún") -> {
                listOf(
                    AdminTopping("Bò viên gân (3 viên)", 10000),
                    AdminTopping("Thịt bò tái thêm", 15000),
                    AdminTopping("Trứng gà chần nước dùng", 7000),
                    AdminTopping("Quẩy giòn giòn (2 cái)", 5000)
                )
            }
            lowerCat.contains("gà") || lowerName.contains("gà") -> {
                listOf(
                    AdminTopping("Khoai tây chiên giòn", 15000),
                    AdminTopping("Sốt phô mai cay", 6000),
                    AdminTopping("Bắp cải trộn coleslaw", 8000),
                    AdminTopping("1 Lon nước ngọt 330ml", 12000)
                )
            }
            lowerCat.contains("bánh mì") || lowerName.contains("bánh mì") -> {
                listOf(
                    AdminTopping("Trứng ốp la thêm", 6000),
                    AdminTopping("Thêm pate & bơ thơm", 5000),
                    AdminTopping("Thêm chả lụa bì heo", 7000),
                    AdminTopping("Thêm xúc xích nướng", 8000)
                )
            }
            else -> {
                listOf(
                    AdminTopping("Trứng ốp la", 7000),
                    AdminTopping("Thêm sốt đặc biệt", 5000),
                    AdminTopping("Thêm phần rau & đồ chua", 4000)
                )
            }
        }
    }

    /**
     * Lấy danh sách kích cỡ có sẵn cho 1 món ăn, mỗi size có giá tiền khác nhau.
     */
    fun getAvailableSizes(foodName: String, category: String, basePrice: Int): List<SizeOption> {
        val defSizes = getDefaultSizes(foodName, category)
        return defSizes.map { SizeOption(it.name, it.priceDelta, basePrice + it.priceDelta) }
    }

    fun getSizesForFood(food: Food): List<SizeOption> {
        val base = FormatUtils.parsePrice(food.price)
        val customSizes = parseSizesJson(food.sizesJson)
        if (customSizes.isNotEmpty()) {
            return customSizes.map { SizeOption(it.name, it.priceDelta, base + it.priceDelta) }
        }
        return getAvailableSizes(food.name, food.category, base)
    }

    fun getToppingsForFood(food: Food): List<AdminTopping> {
        val customToppings = parseToppingsJson(food.toppingsJson)
        if (customToppings.isNotEmpty()) {
            return customToppings
        }
        return getDefaultToppings(food.name, food.category)
    }

    fun getAvailableSizesForFood(food: Food): List<SizeOption> {
        return getSizesForFood(food)
    }

    /**
     * Tìm món Food gốc từ danh sách món ăn hệ thống theo foodId hoặc foodName.
     */
    fun findOriginalFood(item: CartItem): Food? {
        return SampleData.FOODS.find { it.id == item.foodId }
            ?: SampleData.FOODS.find { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
    }

    /**
     * Lấy danh sách size cho CartItem trong giỏ hàng.
     */
    fun getSizesForCartItem(item: CartItem): List<SizeOption> {
        val origFood = findOriginalFood(item)
        if (origFood != null) {
            return getSizesForFood(origFood)
        }
        val origBasePrice = item.price
        return getAvailableSizes(item.name, "", origBasePrice)
    }

    /**
     * Kiểm tra xem món trong giỏ hàng đã được chọn size hay chưa.
     */
    fun hasSize(item: CartItem): Boolean {
        val note = item.note.trim()
        if (note.isEmpty() || note.equals("Chưa chọn size", ignoreCase = true)) return false

        val available = getSizesForCartItem(item)
        if (available.isEmpty()) return true

        val matchesAnyAvailable = available.any { size ->
            val clean = size.name.replace(Regex("\\(.*\\)"), "").trim()
            note.contains(clean, ignoreCase = true) || note.contains(size.name, ignoreCase = true)
        }
        if (matchesAnyAvailable) return true

        val keywords = listOf(
            "Size", "Tô", "Dĩa", "Ổ", "Phần", "Tiêu chuẩn", "Lớn", "Đặc biệt", "Size M", "Size L", "Vừa"
        )
        return keywords.any { note.contains(it, ignoreCase = true) }
    }
}
