package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.SampleData

// =========================================================================
// 10. QUẢN TRỊ ADMIN - NHÀ HÀNG HỆ THỐNG (RESTAURANTS)
// =========================================================================

interface RestaurantDao : BaseDao {
    fun ensureRestaurantsTable(db: SQLiteDatabase)
    fun seedInitialRestaurants(db: SQLiteDatabase)

    /** Lấy toàn bộ danh sách quán ăn / nhà hàng từ SQLite. */
    fun getAllRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        try {
            val db = dbWritable
            ensureRestaurantsTable(db)
            seedInitialRestaurants(db)
            val cursor = db.query(DatabaseHelper.TABLE_RESTAURANTS, null, null, null, null, null, "${DatabaseHelper.COL_REST_ID} ASC")
            while (cursor.moveToNext()) {
                val idCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_ID)
                val nameCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_NAME)
                val descCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_DESC)
                val ratingCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_RATING)
                val timeCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_TIME)
                val distCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_DIST)
                val badgeCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_BADGE)
                val bgStartCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_BG_START)
                val bgEndCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_BG_END)
                val imgCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_IMAGE_RES)
                val uriCol = cursor.getColumnIndex(DatabaseHelper.COL_REST_IMAGE_URI)

                val id = if (idCol >= 0) cursor.getInt(idCol) else 0
                val name = if (nameCol >= 0) cursor.getString(nameCol) else ""
                val desc = if (descCol >= 0) cursor.getString(descCol) else ""
                val rating = if (ratingCol >= 0) cursor.getDouble(ratingCol) else 5.0
                val time = if (timeCol >= 0) cursor.getString(timeCol) ?: "20-30 phút" else "20-30 phút"
                val dist = if (distCol >= 0) cursor.getString(distCol) ?: "1.5 km" else "1.5 km"
                val badge = if (badgeCol >= 0) cursor.getString(badgeCol) ?: "" else ""
                val bgStart = if (bgStartCol >= 0) cursor.getInt(bgStartCol) else 0
                val bgEnd = if (bgEndCol >= 0) cursor.getInt(bgEndCol) else 0
                val imageRes = if (imgCol >= 0) cursor.getInt(imgCol) else 0
                val imageUri = if (uriCol >= 0) cursor.getString(uriCol) ?: "" else ""

                list.add(
                    Restaurant(
                        id = id,
                        name = name,
                        desc = desc,
                        rating = rating,
                        time = time,
                        dist = dist,
                        badge = badge,
                        bgColorStart = bgStart,
                        bgColorEnd = bgEnd,
                        imageRes = imageRes,
                        imageUri = imageUri.ifEmpty { null }
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (list.isNotEmpty()) list else SampleData.DEFAULT_RESTAURANTS
    }

    /** Tạo ID tự tăng tiếp theo cho nhà hàng mới. */
    fun getNextRestaurantId(): Int {
        return try {
            val db = dbWritable
            ensureRestaurantsTable(db)
            val cursor = db.rawQuery("SELECT MAX(${DatabaseHelper.COL_REST_ID}) FROM ${DatabaseHelper.TABLE_RESTAURANTS}", null)
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

    /** Thêm nhà hàng mới vào hệ thống. */
    fun insertRestaurant(restaurant: Restaurant): Long {
        return try {
            val db = dbWritable
            ensureRestaurantsTable(db)
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_REST_ID, restaurant.id)
                put(DatabaseHelper.COL_REST_NAME, restaurant.name)
                put(DatabaseHelper.COL_REST_DESC, restaurant.desc)
                put(DatabaseHelper.COL_REST_RATING, restaurant.rating)
                put(DatabaseHelper.COL_REST_TIME, restaurant.time)
                put(DatabaseHelper.COL_REST_DIST, restaurant.dist)
                put(DatabaseHelper.COL_REST_BADGE, restaurant.badge)
                put(DatabaseHelper.COL_REST_BG_START, restaurant.bgColorStart)
                put(DatabaseHelper.COL_REST_BG_END, restaurant.bgColorEnd)
                put(DatabaseHelper.COL_REST_IMAGE_RES, restaurant.imageRes)
                put(DatabaseHelper.COL_REST_IMAGE_URI, restaurant.imageUri ?: "")
            }
            db.insertWithOnConflict(DatabaseHelper.TABLE_RESTAURANTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /** Cập nhật thông tin nhà hàng. */
    fun updateRestaurant(restaurant: Restaurant): Boolean {
        return try {
            val db = dbWritable
            ensureRestaurantsTable(db)
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_REST_NAME, restaurant.name)
                put(DatabaseHelper.COL_REST_DESC, restaurant.desc)
                put(DatabaseHelper.COL_REST_RATING, restaurant.rating)
                put(DatabaseHelper.COL_REST_TIME, restaurant.time)
                put(DatabaseHelper.COL_REST_DIST, restaurant.dist)
                put(DatabaseHelper.COL_REST_BADGE, restaurant.badge)
                put(DatabaseHelper.COL_REST_BG_START, restaurant.bgColorStart)
                put(DatabaseHelper.COL_REST_BG_END, restaurant.bgColorEnd)
                put(DatabaseHelper.COL_REST_IMAGE_RES, restaurant.imageRes)
                put(DatabaseHelper.COL_REST_IMAGE_URI, restaurant.imageUri ?: "")
            }
            val rows = db.update(DatabaseHelper.TABLE_RESTAURANTS, cv, "${DatabaseHelper.COL_REST_ID} = ?", arrayOf(restaurant.id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Xóa nhà hàng theo id. */
    fun deleteRestaurant(id: Int): Boolean {
        return try {
            val db = dbWritable
            ensureRestaurantsTable(db)
            val rows = db.delete(DatabaseHelper.TABLE_RESTAURANTS, "${DatabaseHelper.COL_REST_ID} = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
