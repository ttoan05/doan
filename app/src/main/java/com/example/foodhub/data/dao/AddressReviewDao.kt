package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Review
import com.example.foodhub.data.model.SavedAddress

// =========================================================================
// 5 & 6. QUẢN LÝ SỔ ĐỊA CHỈ VÀ ĐÁNH GIÁ (ADDRESSES & REVIEWS)
// =========================================================================

interface AddressReviewDao : BaseDao {
    fun createAddressesTable(): String
    fun createReviewsTable(): String

    fun ensureReviewsTable(db: SQLiteDatabase) {
        db.execSQL(createReviewsTable())
        addColumnIfMissing(db, DatabaseHelper.TABLE_REVIEWS, DatabaseHelper.COL_REVIEW_IMAGE_URI, "TEXT DEFAULT ''")
    }

    fun getAddresses(userId: Long): List<SavedAddress> {
        val list = mutableListOf<SavedAddress>()
        if (userId <= 0) return list
        try {
            val db = dbReadable
            db.execSQL(createAddressesTable())
            val cursor = db.query(
                DatabaseHelper.TABLE_ADDRESSES,
                null,
                "${DatabaseHelper.COL_ADDR_USER_ID} = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseHelper.COL_ADDR_IS_DEFAULT} DESC, ${DatabaseHelper.COL_ADDR_ID} DESC"
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_ID))
                val label = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_LABEL))
                val receiver = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_RECEIVER))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_PHONE))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_ADDRESS))
                val isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ADDR_IS_DEFAULT)) == 1

                list.add(
                    SavedAddress(
                        id = id,
                        label = label,
                        receiver = receiver,
                        phone = phone,
                        address = address,
                        isDefault = isDefault
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun insertAddress(userId: Long, address: SavedAddress): Long {
        if (userId <= 0) return -1L
        val db = dbWritable
        db.execSQL(createAddressesTable())
        if (address.isDefault) {
            val resetValues = ContentValues().apply { put(DatabaseHelper.COL_ADDR_IS_DEFAULT, 0) }
            db.update(DatabaseHelper.TABLE_ADDRESSES, resetValues, "${DatabaseHelper.COL_ADDR_USER_ID} = ?", arrayOf(userId.toString()))
        }
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_ADDR_USER_ID, userId)
            put(DatabaseHelper.COL_ADDR_LABEL, address.label)
            put(DatabaseHelper.COL_ADDR_RECEIVER, address.receiver)
            put(DatabaseHelper.COL_ADDR_PHONE, address.phone)
            put(DatabaseHelper.COL_ADDR_ADDRESS, address.address)
            put(DatabaseHelper.COL_ADDR_IS_DEFAULT, if (address.isDefault) 1 else 0)
        }
        return db.insert(DatabaseHelper.TABLE_ADDRESSES, null, cv)
    }

    fun updateAddress(userId: Long, address: SavedAddress): Boolean {
        if (userId <= 0) return false
        val db = dbWritable
        db.execSQL(createAddressesTable())
        if (address.isDefault) {
            val resetValues = ContentValues().apply { put(DatabaseHelper.COL_ADDR_IS_DEFAULT, 0) }
            db.update(DatabaseHelper.TABLE_ADDRESSES, resetValues, "${DatabaseHelper.COL_ADDR_USER_ID} = ?", arrayOf(userId.toString()))
        }
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_ADDR_LABEL, address.label)
            put(DatabaseHelper.COL_ADDR_RECEIVER, address.receiver)
            put(DatabaseHelper.COL_ADDR_PHONE, address.phone)
            put(DatabaseHelper.COL_ADDR_ADDRESS, address.address)
            put(DatabaseHelper.COL_ADDR_IS_DEFAULT, if (address.isDefault) 1 else 0)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_ADDRESSES,
            cv,
            "${DatabaseHelper.COL_ADDR_USER_ID} = ? AND ${DatabaseHelper.COL_ADDR_ID} = ?",
            arrayOf(userId.toString(), address.id.toString())
        )
        return rows > 0
    }

    fun setDefaultAddress(userId: Long, addressId: Long): Boolean {
        if (userId <= 0) return false
        val db = dbWritable
        db.execSQL(createAddressesTable())
        db.beginTransaction()
        try {
            val resetValues = ContentValues().apply { put(DatabaseHelper.COL_ADDR_IS_DEFAULT, 0) }
            db.update(DatabaseHelper.TABLE_ADDRESSES, resetValues, "${DatabaseHelper.COL_ADDR_USER_ID} = ?", arrayOf(userId.toString()))

            val setValues = ContentValues().apply { put(DatabaseHelper.COL_ADDR_IS_DEFAULT, 1) }
            val rows = db.update(
                DatabaseHelper.TABLE_ADDRESSES,
                setValues,
                "${DatabaseHelper.COL_ADDR_USER_ID} = ? AND ${DatabaseHelper.COL_ADDR_ID} = ?",
                arrayOf(userId.toString(), addressId.toString())
            )
            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun deleteAddress(userId: Long, addressId: Long): Boolean {
        if (userId <= 0) return false
        val db = dbWritable
        val rows = db.delete(
            DatabaseHelper.TABLE_ADDRESSES,
            "${DatabaseHelper.COL_ADDR_USER_ID} = ? AND ${DatabaseHelper.COL_ADDR_ID} = ?",
            arrayOf(userId.toString(), addressId.toString())
        )
        return rows > 0
    }

    fun insertReview(userId: Long, review: Review): Long {
        if (userId <= 0) return -1L
        val db = dbWritable
        ensureReviewsTable(db)
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_REVIEW_USER_ID, userId)
            put(DatabaseHelper.COL_REVIEW_ORDER_ID, review.orderId)
            put(DatabaseHelper.COL_REVIEW_RESTAURANT, review.restaurant)
            put(DatabaseHelper.COL_REVIEW_STARS, review.stars)
            put(DatabaseHelper.COL_REVIEW_COMMENT, review.comment)
            put(DatabaseHelper.COL_REVIEW_IMAGE_URI, review.imageUri ?: "")
            put(DatabaseHelper.COL_REVIEW_CREATED_AT, review.createdAt)
        }
        return db.insert(DatabaseHelper.TABLE_REVIEWS, null, cv)
    }

    fun isOrderReviewed(userId: Long, orderId: String): Boolean {
        if (userId <= 0) return false
        val db = dbReadable
        ensureReviewsTable(db)
        val cursor = db.query(
            DatabaseHelper.TABLE_REVIEWS,
            arrayOf(DatabaseHelper.COL_REVIEW_ID),
            "${DatabaseHelper.COL_REVIEW_USER_ID} = ? AND ${DatabaseHelper.COL_REVIEW_ORDER_ID} = ?",
            arrayOf(userId.toString(), orderId),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun hasReview(userId: Long, orderId: String): Boolean = isOrderReviewed(userId, orderId)

    fun getReviewForOrder(userId: Long, orderId: String): Review? {
        if (userId <= 0) return null
        val db = dbReadable
        ensureReviewsTable(db)
        val cursor = db.query(
            DatabaseHelper.TABLE_REVIEWS,
            null,
            "${DatabaseHelper.COL_REVIEW_USER_ID} = ? AND ${DatabaseHelper.COL_REVIEW_ORDER_ID} = ?",
            arrayOf(userId.toString(), orderId),
            null,
            null,
            null
        )
        var review: Review? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_ID))
            val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_RESTAURANT))
            val stars = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_STARS))
            val comment = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_COMMENT))
            val imgCol = cursor.getColumnIndex(DatabaseHelper.COL_REVIEW_IMAGE_URI)
            val imageUri = if (imgCol >= 0) cursor.getString(imgCol) else null
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_CREATED_AT))
            review = Review(
                id = id,
                orderId = orderId,
                restaurant = restaurant,
                stars = stars,
                comment = comment,
                imageUri = if (imageUri.isNullOrEmpty()) null else imageUri,
                createdAt = createdAt
            )
        }
        cursor.close()
        return review
    }

    fun avgRatingForRestaurant(restaurant: String): Pair<Double, Int> {
        val db = dbReadable
        val cursor = db.rawQuery(
            "SELECT AVG(${DatabaseHelper.COL_REVIEW_STARS}), COUNT(*) FROM ${DatabaseHelper.TABLE_REVIEWS} WHERE ${DatabaseHelper.COL_REVIEW_RESTAURANT} = ?",
            arrayOf(restaurant)
        )
        var avg = 5.0
        var count = 0
        if (cursor.moveToFirst()) {
            val rawAvg = cursor.getDouble(0)
            count = cursor.getInt(1)
            if (count > 0 && rawAvg > 0) {
                avg = rawAvg
            }
        }
        cursor.close()
        return Pair(avg, count)
    }

    fun getReviews(userId: Long): List<Review> {
        val list = mutableListOf<Review>()
        if (userId <= 0) return list
        try {
            val db = dbReadable
            ensureReviewsTable(db)
            val cursor = db.query(
                DatabaseHelper.TABLE_REVIEWS,
                null,
                "${DatabaseHelper.COL_REVIEW_USER_ID} = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseHelper.COL_REVIEW_CREATED_AT} DESC"
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_ID))
                val orderId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_ORDER_ID))
                val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_RESTAURANT))
                val stars = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_STARS))
                val comment = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_COMMENT))
                val imgCol = cursor.getColumnIndex(DatabaseHelper.COL_REVIEW_IMAGE_URI)
                val imageUri = if (imgCol >= 0) cursor.getString(imgCol) else null
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_REVIEW_CREATED_AT))

                list.add(
                    Review(
                        id = id,
                        orderId = orderId,
                        restaurant = restaurant,
                        stars = stars,
                        comment = comment,
                        imageUri = if (imageUri.isNullOrEmpty()) null else imageUri,
                        createdAt = createdAt
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
