package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.SampleData

// =========================================================================
// 9. QUẢN TRỊ ADMIN - MÓN ĂN HỆ THỐNG (FOODS)
// =========================================================================

interface FoodDao : BaseDao {
    fun ensureFoodsTable(db: SQLiteDatabase)
    fun seedInitialFoods(db: SQLiteDatabase)

    /** Lấy toàn bộ món ăn hệ thống từ SQLite (ưu tiên món mới thêm lên đầu). */
    fun getAllFoods(): List<Food> {
        val list = mutableListOf<Food>()
        try {
            val db = dbWritable
            ensureFoodsTable(db)
            seedInitialFoods(db)
            val orderBy = "CASE WHEN ${DatabaseHelper.COL_FOOD_ID} >= 100 THEN 0 ELSE 1 END, ${DatabaseHelper.COL_FOOD_ID} DESC"
            val cursor = db.query(DatabaseHelper.TABLE_FOODS, null, null, null, null, null, orderBy)
            while (cursor.moveToNext()) {
                val idCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_ID)
                val nameCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_NAME)
                val restCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_RESTAURANT)
                val ratingCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_RATING)
                val reviewsCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_REVIEWS)
                val priceCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_PRICE)
                val catCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_CATEGORY)
                val emojiCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_EMOJI)
                val bgStartCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_BG_START)
                val bgEndCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_BG_END)
                val imgCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IMAGE_RES)
                val uriCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IMAGE_URI)
                val sizesCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_SIZES)
                val toppingsCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_TOPPINGS)
                val availCol = cursor.getColumnIndex(DatabaseHelper.COL_FOOD_IS_AVAILABLE)

                val id = if (idCol >= 0) cursor.getInt(idCol) else 0
                val name = if (nameCol >= 0) cursor.getString(nameCol) else ""
                val restaurant = if (restCol >= 0) cursor.getString(restCol) else ""
                val rating = if (ratingCol >= 0) cursor.getDouble(ratingCol) else 5.0
                val reviews = if (reviewsCol >= 0) cursor.getInt(reviewsCol) else 0
                val price = if (priceCol >= 0) cursor.getString(priceCol) else "0đ"
                val category = if (catCol >= 0) cursor.getString(catCol) else ""
                val emoji = if (emojiCol >= 0) cursor.getString(emojiCol) ?: "🍲" else "🍲"
                val bgStart = if (bgStartCol >= 0) cursor.getInt(bgStartCol) else 0
                val bgEnd = if (bgEndCol >= 0) cursor.getInt(bgEndCol) else 0
                val imageRes = if (imgCol >= 0) cursor.getInt(imgCol) else 0
                val imageUri = if (uriCol >= 0) cursor.getString(uriCol) ?: "" else ""
                val rawSizes = if (sizesCol >= 0 && !cursor.isNull(sizesCol)) cursor.getString(sizesCol) else null
                val sizesJson = rawSizes?.takeIf { it.isNotBlank() && it.trim() != "[]" }

                val rawToppings = if (toppingsCol >= 0 && !cursor.isNull(toppingsCol)) cursor.getString(toppingsCol) else null
                val toppingsJson = rawToppings?.takeIf { it.isNotBlank() && it.trim() != "[]" }
                val isAvailable = if (availCol >= 0) cursor.getInt(availCol) == 1 else true

                val actualImageRes = SampleData.resolveFoodImage(id, name, imageRes, category)

                list.add(
                    Food(
                        id = id,
                        name = name,
                        restaurant = restaurant,
                        rating = rating,
                        reviews = reviews,
                        price = price,
                        category = category,
                        emoji = emoji,
                        bgColorStart = bgStart,
                        bgColorEnd = bgEnd,
                        imageRes = actualImageRes,
                        imageUri = imageUri.ifEmpty { null },
                        sizesJson = sizesJson,
                        toppingsJson = toppingsJson,
                        isAvailable = isAvailable
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (list.isNotEmpty()) list else SampleData.DEFAULT_FOODS
    }

    /** Tạo ID tự tăng tiếp theo cho món ăn mới. */
    fun getNextFoodId(): Int {
        return try {
            val db = dbWritable
            ensureFoodsTable(db)
            val cursor = db.rawQuery("SELECT MAX(${DatabaseHelper.COL_FOOD_ID}) FROM ${DatabaseHelper.TABLE_FOODS}", null)
            var maxId = 0
            if (cursor.moveToNext()) {
                maxId = cursor.getInt(0)
            }
            cursor.close()
            maxOf(maxId + 1, 100)
        } catch (e: Exception) {
            e.printStackTrace()
            100
        }
    }

    /** Thêm món ăn mới vào hệ thống. */
    fun insertFood(food: Food): Long {
        return try {
            val db = dbWritable
            ensureFoodsTable(db)
            val actualImageRes = SampleData.resolveFoodImage(food.id, food.name, food.imageRes, food.category)
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_FOOD_ID, food.id)
                put(DatabaseHelper.COL_FOOD_NAME, food.name)
                put(DatabaseHelper.COL_FOOD_RESTAURANT, food.restaurant)
                put(DatabaseHelper.COL_FOOD_RATING, food.rating)
                put(DatabaseHelper.COL_FOOD_REVIEWS, food.reviews)
                put(DatabaseHelper.COL_FOOD_PRICE, food.price)
                put(DatabaseHelper.COL_FOOD_CATEGORY, food.category)
                put(DatabaseHelper.COL_FOOD_EMOJI, food.emoji)
                put(DatabaseHelper.COL_FOOD_BG_START, food.bgColorStart)
                put(DatabaseHelper.COL_FOOD_BG_END, food.bgColorEnd)
                put(DatabaseHelper.COL_FOOD_IMAGE_RES, actualImageRes)
                put(DatabaseHelper.COL_FOOD_IMAGE_URI, food.imageUri ?: "")
                if (food.sizesJson != null) {
                    put(DatabaseHelper.COL_FOOD_SIZES, food.sizesJson)
                } else {
                    putNull(DatabaseHelper.COL_FOOD_SIZES)
                }
                if (food.toppingsJson != null) {
                    put(DatabaseHelper.COL_FOOD_TOPPINGS, food.toppingsJson)
                } else {
                    putNull(DatabaseHelper.COL_FOOD_TOPPINGS)
                }
                put(DatabaseHelper.COL_FOOD_IS_AVAILABLE, if (food.isAvailable) 1 else 0)
            }
            db.insertWithOnConflict(DatabaseHelper.TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /** Cập nhật thông tin món ăn (upsert: nếu chưa có thì insert luôn). */
    fun updateFood(food: Food): Boolean {
        return try {
            val db = dbWritable
            ensureFoodsTable(db)
            val actualImageRes = SampleData.resolveFoodImage(food.id, food.name, food.imageRes, food.category)
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_FOOD_ID, food.id)
                put(DatabaseHelper.COL_FOOD_NAME, food.name)
                put(DatabaseHelper.COL_FOOD_RESTAURANT, food.restaurant)
                put(DatabaseHelper.COL_FOOD_RATING, food.rating)
                put(DatabaseHelper.COL_FOOD_REVIEWS, food.reviews)
                put(DatabaseHelper.COL_FOOD_PRICE, food.price)
                put(DatabaseHelper.COL_FOOD_CATEGORY, food.category)
                put(DatabaseHelper.COL_FOOD_EMOJI, food.emoji)
                put(DatabaseHelper.COL_FOOD_BG_START, food.bgColorStart)
                put(DatabaseHelper.COL_FOOD_BG_END, food.bgColorEnd)
                put(DatabaseHelper.COL_FOOD_IMAGE_RES, actualImageRes)
                put(DatabaseHelper.COL_FOOD_IMAGE_URI, food.imageUri ?: "")
                if (food.sizesJson != null) {
                    put(DatabaseHelper.COL_FOOD_SIZES, food.sizesJson)
                } else {
                    putNull(DatabaseHelper.COL_FOOD_SIZES)
                }
                if (food.toppingsJson != null) {
                    put(DatabaseHelper.COL_FOOD_TOPPINGS, food.toppingsJson)
                } else {
                    putNull(DatabaseHelper.COL_FOOD_TOPPINGS)
                }
                put(DatabaseHelper.COL_FOOD_IS_AVAILABLE, if (food.isAvailable) 1 else 0)
            }
            val rows = db.update(DatabaseHelper.TABLE_FOODS, cv, "${DatabaseHelper.COL_FOOD_ID} = ?", arrayOf(food.id.toString()))
            if (rows <= 0) {
                val inserted = db.insertWithOnConflict(DatabaseHelper.TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                inserted != -1L
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Cập nhật trạng thái Còn hàng / Tạm hết của món ăn */
    fun updateFoodAvailability(foodId: Int, isAvailable: Boolean): Boolean {
        return try {
            val db = dbWritable
            ensureFoodsTable(db)
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_FOOD_IS_AVAILABLE, if (isAvailable) 1 else 0)
            }
            val rows = db.update(DatabaseHelper.TABLE_FOODS, cv, "${DatabaseHelper.COL_FOOD_ID} = ?", arrayOf(foodId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Xóa món ăn khỏi hệ thống. */
    fun deleteFood(foodId: Int): Boolean {
        return try {
            val db = dbWritable
            ensureFoodsTable(db)
            val rows = db.delete(DatabaseHelper.TABLE_FOODS, "${DatabaseHelper.COL_FOOD_ID} = ?", arrayOf(foodId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Tự động đồng bộ kích cỡ và topping mặc định phù hợp cho các món chưa có dữ liệu */
    fun syncDefaultFoodSizesAndToppings() {
        try {
            val db = dbWritable
            ensureFoodsTable(db)
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.COL_FOOD_ID}, ${DatabaseHelper.COL_FOOD_NAME}, ${DatabaseHelper.COL_FOOD_CATEGORY}, ${DatabaseHelper.COL_FOOD_SIZES}, ${DatabaseHelper.COL_FOOD_TOPPINGS} FROM ${DatabaseHelper.TABLE_FOODS}",
                null
            )
            val updates = mutableListOf<Triple<Int, String, String>>()
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val name = cursor.getString(1) ?: ""
                val category = cursor.getString(2) ?: ""
                val sizes = cursor.getString(3) ?: ""
                val toppings = cursor.getString(4) ?: ""

                val needSizes = sizes.isBlank() || sizes.trim() == "[]"
                val needToppings = toppings.isBlank() || toppings.trim() == "[]"

                if (needSizes || needToppings) {
                    val sJson = if (needSizes) {
                        com.example.foodhub.utils.FoodSizeHelper.sizesToJson(
                            com.example.foodhub.utils.FoodSizeHelper.getDefaultSizes(name, category)
                        )
                    } else sizes

                    val tJson = if (needToppings) {
                        com.example.foodhub.utils.FoodSizeHelper.toppingsToJson(
                            com.example.foodhub.utils.FoodSizeHelper.getDefaultToppings(name, category)
                        )
                    } else toppings

                    updates.add(Triple(id, sJson, tJson))
                }
            }
            cursor.close()

            updates.forEach { (id, sJson, tJson) ->
                val cv = ContentValues().apply {
                    put(DatabaseHelper.COL_FOOD_SIZES, sJson)
                    put(DatabaseHelper.COL_FOOD_TOPPINGS, tJson)
                }
                db.update(DatabaseHelper.TABLE_FOODS, cv, "${DatabaseHelper.COL_FOOD_ID} = ?", arrayOf(id.toString()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
