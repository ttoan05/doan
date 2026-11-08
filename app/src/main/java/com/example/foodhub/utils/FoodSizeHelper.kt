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
        if (json.isNullOrBlank() || json.trim() == "[]") return emptyList()
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
        if (json.isNullOrBlank() || json.trim() == "[]") return emptyList()
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
            lowerCat.contains("uống") || lowerName.contains("chanh") || lowerName.contains("trà") || lowerName.contains("cà phê") || lowerName.contains("nước") -> {
                listOf(
                    AdminSize("Size Vừa (Ly 500ml - M)", 0),
                    AdminSize("Size Lớn (Ly 700ml - L)", 6000),
                    AdminSize("Size Khổng Lồ (Ly 1000ml - XL)", 12000)
                )
            }
            lowerName.contains("mì quảng") || lowerName.contains("quảng") -> {
                listOf(
                    AdminSize("Tô Vừa (Tiêu chuẩn)", 0),
                    AdminSize("Tô Lớn (Nhiều tôm & thịt)", 12000),
                    AdminSize("Tô Đặc Biệt Cao Cấp", 20000)
                )
            }
            lowerCat.contains("bánh mì") || lowerName.contains("bánh mì") -> {
                listOf(
                    AdminSize("Ổ Tiêu chuẩn", 0),
                    AdminSize("Ổ Đặc biệt (Gấp đôi nhân thịt)", 10000)
                )
            }
            lowerCat.contains("cơm") || lowerName.contains("cơm") -> {
                listOf(
                    AdminSize("Dĩa Tiêu chuẩn", 0),
                    AdminSize("Dĩa Lớn (Thêm cơm & mỡ hành)", 8000),
                    AdminSize("Dĩa Đặc Biệt Sườn Cây Khủng", 20000)
                )
            }
            lowerCat.contains("phở") || lowerCat.contains("mì") || lowerCat.contains("bún") || lowerName.contains("phở") || lowerName.contains("bún") || lowerName.contains("hủ tiếu") -> {
                listOf(
                    AdminSize("Tô Tiêu chuẩn", 0),
                    AdminSize("Tô Lớn (Thêm bánh & thịt)", 10000),
                    AdminSize("Tô Đặc Biệt Khổng Lồ", 20000)
                )
            }
            lowerCat.contains("gà") || lowerName.contains("gà") -> {
                listOf(
                    AdminSize("Phần 1 Miếng (Tiêu chuẩn)", 0),
                    AdminSize("Phần 2 Miếng Giòn Cay", 35000),
                    AdminSize("Combo 3 Miếng + Khoai tây", 65000)
                )
            }
            else -> {
                listOf(
                    AdminSize("Phần Tiêu chuẩn", 0),
                    AdminSize("Phần Lớn Đặc biệt", 12000)
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
            lowerCat.contains("uống") || lowerName.contains("chanh") || lowerName.contains("trà") || lowerName.contains("cà phê") || lowerName.contains("nước") -> {
                listOf(
                    AdminTopping("Trân châu đen dẻo", 6000),
                    AdminTopping("Thạch nha đam giòn", 6000),
                    AdminTopping("Kem cheese béo ngậy", 10000),
                    AdminTopping("Trân châu trắng giòn 3Q", 6000),
                    AdminTopping("Thêm chanh & mật ong hoa rừng", 5000)
                )
            }
            lowerName.contains("mì quảng") || lowerName.contains("quảng") -> {
                listOf(
                    AdminTopping("Bánh tráng nướng mè giòn rụm", 6000),
                    AdminTopping("Trứng cút rim đậm đà (3 quả)", 6000),
                    AdminTopping("Thêm tôm tươi rim thịt ba chỉ", 15000),
                    AdminTopping("Đậu phộng rang & ớt xanh ngâm", 3000),
                    AdminTopping("Đĩa rau sống búp chuối tươi", 5000)
                )
            }
            lowerCat.contains("cơm") || lowerName.contains("cơm") -> {
                listOf(
                    AdminTopping("Trứng ốp la lòng đào", 7000),
                    AdminTopping("Chả trứng hấp thịt nấm mèo", 8000),
                    AdminTopping("Bì heo thơm thính giòn", 7000),
                    AdminTopping("Thêm sườn nướng mật ong", 22000),
                    AdminTopping("Chén canh súp rong biển", 6000)
                )
            }
            lowerCat.contains("phở") || lowerCat.contains("mì") || lowerCat.contains("bún") || lowerName.contains("phở") || lowerName.contains("bún") || lowerName.contains("hủ tiếu") -> {
                listOf(
                    AdminTopping("Bò viên gân giòn (3 viên)", 10000),
                    AdminTopping("Thịt bò tái thêm", 15000),
                    AdminTopping("Trứng gà chần nước dùng", 7000),
                    AdminTopping("Quẩy giòn giòn (2 cái)", 5000),
                    AdminTopping("Tiết luộc mềm", 5000)
                )
            }
            lowerCat.contains("gà") || lowerName.contains("gà") -> {
                listOf(
                    AdminTopping("Khoai tây chiên giòn", 15000),
                    AdminTopping("Sốt phô mai cay béo ngậy", 8000),
                    AdminTopping("Bắp cải trộn coleslaw", 10000),
                    AdminTopping("1 Lon Pepsi / Coca 330ml", 12000),
                    AdminTopping("Thêm tương ớt & sốt cà", 3000)
                )
            }
            lowerCat.contains("bánh mì") || lowerName.contains("bánh mì") -> {
                listOf(
                    AdminTopping("Trứng ốp la thêm", 6000),
                    AdminTopping("Thêm pate béo & bơ thơm", 5000),
                    AdminTopping("Thêm chả lụa & giò thủ", 7000),
                    AdminTopping("Thêm xúc xích nướng", 8000),
                    AdminTopping("Thêm dưa chua & ngò gai", 3000)
                )
            }
            else -> {
                listOf(
                    AdminTopping("Trứng ốp la", 7000),
                    AdminTopping("Thêm sốt đặc biệt", 5000),
                    AdminTopping("Thêm phần rau & đồ chua", 4000),
                    AdminTopping("Thêm chén súp nóng", 6000)
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
        val custom = parseToppingsJson(food.toppingsJson)
        if (custom.isNotEmpty()) {
            return custom
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
