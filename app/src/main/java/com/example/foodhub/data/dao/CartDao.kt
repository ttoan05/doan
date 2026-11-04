package com.example.foodhub.data.dao

import android.content.ContentValues
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.utils.SampleData

// =========================================================================
// 2. QUẢN LÝ GIỎ HÀNG THEO TỪNG USER_ID
// =========================================================================

interface CartDao : BaseDao {
    fun createCartTable(): String

    fun getCartItems(userId: Long): List<CartItem> {
        val list = mutableListOf<CartItem>()
        if (userId <= 0) return list
        try {
            val db = dbReadable
            db.execSQL(createCartTable())
            addColumnIfMissing(db, DatabaseHelper.TABLE_CART, DatabaseHelper.COL_CART_IMAGE_URI, "TEXT DEFAULT ''")

            val cursor = db.query(
                DatabaseHelper.TABLE_CART,
                null,
                "${DatabaseHelper.COL_CART_USER_ID} = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseHelper.COL_CART_ID} ASC"
            )
            while (cursor.moveToNext()) {
                val foodId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_FOOD_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_NAME))
                val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_RESTAURANT))
                val emoji = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_EMOJI)) ?: "🍲"
                val price = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_PRICE))
                val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_QUANTITY))
                val imageRes = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_IMAGE_RES))
                val uriIndex = cursor.getColumnIndex(DatabaseHelper.COL_CART_IMAGE_URI)
                val dbUri = if (uriIndex >= 0) cursor.getString(uriIndex) ?: "" else ""
                val imageUri = dbUri.ifEmpty { SampleData.resolveFoodImageUri(foodId) }
                val noteIndex = cursor.getColumnIndex(DatabaseHelper.COL_CART_NOTE)
                val note = if (noteIndex >= 0) cursor.getString(noteIndex) ?: "" else ""

                val actualImageRes = SampleData.resolveFoodImage(foodId, name, imageRes)

                list.add(
                    CartItem(
                        foodId = foodId,
                        name = name,
                        restaurant = restaurant,
                        emoji = emoji,
                        price = price,
                        quantity = quantity,
                        imageRes = actualImageRes,
                        note = note,
                        imageUri = imageUri
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Thêm món vào giỏ hàng hoặc tăng số lượng nếu đã có món cùng ghi chú.
     */
    fun addToCart(userId: Long, item: CartItem) {
        if (userId <= 0) return
        try {
            val db = dbWritable
            db.execSQL(createCartTable())
            addColumnIfMissing(db, DatabaseHelper.TABLE_CART, DatabaseHelper.COL_CART_IMAGE_URI, "TEXT DEFAULT ''")

            // Tìm món cùng food_id và cùng ghi chú
            val cursor = db.query(
                DatabaseHelper.TABLE_CART,
                arrayOf(DatabaseHelper.COL_CART_ID, DatabaseHelper.COL_CART_QUANTITY),
                "${DatabaseHelper.COL_CART_USER_ID} = ? AND ${DatabaseHelper.COL_CART_FOOD_ID} = ? AND ${DatabaseHelper.COL_CART_NOTE} = ?",
                arrayOf(userId.toString(), item.foodId.toString(), item.note),
                null,
                null,
                null
            )
            val actualImageRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
            val finalImageUri = if (!item.imageUri.isNullOrEmpty()) item.imageUri else (SampleData.resolveFoodImageUri(item.foodId) ?: "")
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_CART_USER_ID, userId)
                put(DatabaseHelper.COL_CART_FOOD_ID, item.foodId)
                put(DatabaseHelper.COL_CART_NAME, item.name)
                put(DatabaseHelper.COL_CART_RESTAURANT, item.restaurant)
                put(DatabaseHelper.COL_CART_EMOJI, item.emoji)
                put(DatabaseHelper.COL_CART_PRICE, item.price)
                put(DatabaseHelper.COL_CART_QUANTITY, item.quantity)
                put(DatabaseHelper.COL_CART_IMAGE_RES, actualImageRes)
                put(DatabaseHelper.COL_CART_IMAGE_URI, finalImageUri)
                put(DatabaseHelper.COL_CART_NOTE, item.note)
            }
            if (cursor.moveToFirst()) {
                val cartId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CART_ID))
                db.update(DatabaseHelper.TABLE_CART, values, "${DatabaseHelper.COL_CART_ID} = ?", arrayOf(cartId.toString()))
            } else {
                db.insert(DatabaseHelper.TABLE_CART, null, values)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateCartQuantity(userId: Long, foodId: Int, note: String, quantity: Int) {
        if (userId <= 0) return
        val db = dbWritable
        if (quantity <= 0) {
            db.delete(
                DatabaseHelper.TABLE_CART,
                "${DatabaseHelper.COL_CART_USER_ID} = ? AND ${DatabaseHelper.COL_CART_FOOD_ID} = ? AND ${DatabaseHelper.COL_CART_NOTE} = ?",
                arrayOf(userId.toString(), foodId.toString(), note)
            )
        } else {
            val values = ContentValues().apply { put(DatabaseHelper.COL_CART_QUANTITY, quantity) }
            db.update(
                DatabaseHelper.TABLE_CART,
                values,
                "${DatabaseHelper.COL_CART_USER_ID} = ? AND ${DatabaseHelper.COL_CART_FOOD_ID} = ? AND ${DatabaseHelper.COL_CART_NOTE} = ?",
                arrayOf(userId.toString(), foodId.toString(), note)
            )
        }
    }

    fun removeCartItem(userId: Long, foodId: Int, note: String) {
        if (userId <= 0) return
        val db = dbWritable
        db.delete(
            DatabaseHelper.TABLE_CART,
            "${DatabaseHelper.COL_CART_USER_ID} = ? AND ${DatabaseHelper.COL_CART_FOOD_ID} = ? AND ${DatabaseHelper.COL_CART_NOTE} = ?",
            arrayOf(userId.toString(), foodId.toString(), note)
        )
    }

    fun clearCart(userId: Long) {
        if (userId <= 0) return
        val db = dbWritable
        db.delete(DatabaseHelper.TABLE_CART, "${DatabaseHelper.COL_CART_USER_ID} = ?", arrayOf(userId.toString()))
    }

    fun saveOrUpdateCartItem(userId: Long, item: CartItem) {
        addToCart(userId, item)
    }
}
