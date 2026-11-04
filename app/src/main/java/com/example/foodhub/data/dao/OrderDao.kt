package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.utils.SampleData

// =========================================================================
// 3 & 7. QUẢN LÝ ĐƠN HÀNG VÀ CHI TIẾT ĐƠN HÀNG (ORDERS & ORDER_ITEMS)
// =========================================================================

interface OrderDao : BaseDao {
    fun ensureOrdersTable(db: SQLiteDatabase)
    fun ensureOrderItemsTable(db: SQLiteDatabase)

    /**
     * Thêm đơn hàng mới và toàn bộ món chi tiết vào SQLite theo userId.
     */
    fun insertOrder(userId: Long, order: Order): Boolean {
        if (userId <= 0) return false
        val db = dbWritable
        try {
            ensureOrdersTable(db)
            ensureOrderItemsTable(db)

            db.beginTransaction()
            try {
                val orderValues = ContentValues().apply {
                    put("id", order.id)
                    put("order_id", order.id)
                    put(DatabaseHelper.COL_ORDER_USER_ID, userId)
                    put(DatabaseHelper.COL_ORDER_RESTAURANT, order.restaurant)
                    put(DatabaseHelper.COL_ORDER_CREATED_AT, order.createdAt)
                    put(DatabaseHelper.COL_ORDER_SHIPPING_FEE, order.shippingFee)
                    put(DatabaseHelper.COL_ORDER_RECEIVER, order.receiverName)
                    put(DatabaseHelper.COL_ORDER_PHONE, order.phone)
                    put(DatabaseHelper.COL_ORDER_ADDRESS, order.address)
                    put(DatabaseHelper.COL_ORDER_PAYMENT, order.paymentMethod)
                    put(DatabaseHelper.COL_ORDER_STATUS, order.status.name)
                    put(DatabaseHelper.COL_ORDER_STEP, order.processStep)
                    put("step", order.processStep)
                    put(DatabaseHelper.COL_ORDER_NOTE, order.orderNote)
                    put(DatabaseHelper.COL_ORDER_VOUCHER, order.voucherCode)
                    put(DatabaseHelper.COL_ORDER_DISCOUNT, order.discount)
                }
                val res = db.insertWithOnConflict(DatabaseHelper.TABLE_ORDERS, null, orderValues, SQLiteDatabase.CONFLICT_REPLACE)
                if (res == -1L) return false

                order.items.forEach { item ->
                    val actualImageRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
                    val finalImageUri = if (!item.imageUri.isNullOrEmpty()) item.imageUri else (SampleData.resolveFoodImageUri(item.foodId) ?: "")
                    val itemValues = ContentValues().apply {
                        put(DatabaseHelper.COL_OI_ORDER_ID, order.id)
                        put(DatabaseHelper.COL_OI_FOOD_ID, item.foodId)
                        put(DatabaseHelper.COL_OI_NAME, item.name)
                        put(DatabaseHelper.COL_OI_RESTAURANT, item.restaurant)
                        put(DatabaseHelper.COL_OI_EMOJI, item.emoji)
                        put(DatabaseHelper.COL_OI_PRICE, item.price)
                        put(DatabaseHelper.COL_OI_QUANTITY, item.quantity)
                        put(DatabaseHelper.COL_OI_IMAGE_RES, actualImageRes)
                        put(DatabaseHelper.COL_OI_IMAGE_URI, finalImageUri)
                        put(DatabaseHelper.COL_OI_NOTE, item.note)
                    }
                    db.insert(DatabaseHelper.TABLE_ORDER_ITEMS, null, itemValues)
                }

                db.setTransactionSuccessful()
                return true
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun cursorToOrder(cursor: Cursor): Order {
        val idCol = listOf("order_id", "id", "_id")
            .map { cursor.getColumnIndex(it) }
            .firstOrNull { it >= 0 } ?: -1

        val restCol = listOf("restaurant", "order_restaurant")
            .map { cursor.getColumnIndex(it) }
            .firstOrNull { it >= 0 } ?: -1

        val dateCol = listOf("created_at", "createdAt", "date")
            .map { cursor.getColumnIndex(it) }
            .firstOrNull { it >= 0 } ?: -1

        val shipCol = listOf("shipping_fee", "shippingFee")
            .map { cursor.getColumnIndex(it) }
            .firstOrNull { it >= 0 } ?: -1

        val recvCol = listOf("receiver_name", "receiver", "name")
            .map { cursor.getColumnIndex(it) }
            .firstOrNull { it >= 0 } ?: -1

        val phoneCol = cursor.getColumnIndex("phone")
        val addrCol = cursor.getColumnIndex("address")
        val payCol = listOf("payment_method", "payment").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1
        val statCol = cursor.getColumnIndex("status")
        val stepCol = listOf("process_step", "step").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1
        val noteIndex = listOf("order_note", "note").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1
        val voucherIndex = listOf("voucher_code", "voucher").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1
        val discountIndex = listOf("discount", "order_discount").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1

        val orderId = if (idCol >= 0) cursor.getString(idCol) ?: "" else ""
        val restaurant = if (restCol >= 0) cursor.getString(restCol) ?: "" else ""
        val createdAt = if (dateCol >= 0) cursor.getLong(dateCol) else System.currentTimeMillis()
        val shippingFee = if (shipCol >= 0) cursor.getInt(shipCol) else 0
        val receiverName = if (recvCol >= 0) cursor.getString(recvCol) ?: "" else ""
        val phone = if (phoneCol >= 0) cursor.getString(phoneCol) ?: "" else ""
        val address = if (addrCol >= 0) cursor.getString(addrCol) ?: "" else ""
        val paymentMethod = if (payCol >= 0) cursor.getString(payCol) ?: "COD" else "COD"
        val statusStr = if (statCol >= 0) cursor.getString(statCol) ?: "PROCESSING" else "PROCESSING"
        val step = if (stepCol >= 0) cursor.getInt(stepCol) else 0
        val orderNote = if (noteIndex >= 0) cursor.getString(noteIndex) ?: "" else ""
        val voucherCode = if (voucherIndex >= 0) cursor.getString(voucherIndex) ?: "" else ""
        val discount = if (discountIndex >= 0) cursor.getInt(discountIndex) else 0

        val status = try {
            OrderStatus.valueOf(statusStr)
        } catch (_: Exception) {
            OrderStatus.PROCESSING
        }

        val items = getOrderItems(orderId)

        return Order(
            id = orderId,
            restaurant = restaurant,
            createdAt = createdAt,
            items = items,
            shippingFee = shippingFee,
            receiverName = receiverName,
            phone = phone,
            address = address,
            paymentMethod = paymentMethod,
            status = status,
            processStep = step,
            orderNote = orderNote,
            voucherCode = voucherCode,
            discount = discount
        )
    }

    fun getOrdersForUser(userId: Long): List<Order> {
        val list = mutableListOf<Order>()
        if (userId <= 0) return list
        try {
            val db = dbReadable
            ensureOrdersTable(db)
            val cursor = db.query(
                DatabaseHelper.TABLE_ORDERS,
                null,
                "${DatabaseHelper.COL_ORDER_USER_ID} = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseHelper.COL_ORDER_CREATED_AT} DESC"
            )
            while (cursor.moveToNext()) {
                list.add(cursorToOrder(cursor))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getOrderById(orderId: String): Order? {
        try {
            val db = dbReadable
            ensureOrdersTable(db)
            val cleanId = orderId.removePrefix("#")
            val cursor = db.query(
                DatabaseHelper.TABLE_ORDERS,
                null,
                "order_id = ? OR id = ? OR order_id = ? OR id = ?",
                arrayOf(orderId, orderId, cleanId, cleanId),
                null,
                null,
                null
            )
            val order = if (cursor.moveToFirst()) cursorToOrder(cursor) else null
            cursor.close()
            return order
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getOrderItems(orderId: String): List<CartItem> {
        val items = mutableListOf<CartItem>()
        if (orderId.isBlank()) return items
        try {
            val db = dbReadable
            ensureOrderItemsTable(db)

            val cleanId = orderId.removePrefix("#")
            val cursor = db.query(
                DatabaseHelper.TABLE_ORDER_ITEMS,
                null,
                "order_id = ? OR order_id = ?",
                arrayOf(orderId, cleanId),
                null,
                null,
                "id ASC"
            )
            while (cursor.moveToNext()) {
                val foodIdCol = listOf("food_id", "foodId").map { cursor.getColumnIndex(it) }.firstOrNull { it >= 0 } ?: -1
                val nameCol = cursor.getColumnIndex("name")
                val restCol = cursor.getColumnIndex("restaurant")
                val emojiCol = cursor.getColumnIndex("emoji")
                val priceCol = cursor.getColumnIndex("price")
                val qtyCol = cursor.getColumnIndex("quantity")
                val imgCol = cursor.getColumnIndex("image_res")
                val uriIndex = cursor.getColumnIndex("image_uri")
                val noteIndex = cursor.getColumnIndex("note")

                val foodId = if (foodIdCol >= 0) cursor.getInt(foodIdCol) else 0
                val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
                val restaurant = if (restCol >= 0) cursor.getString(restCol) ?: "" else ""
                val emoji = if (emojiCol >= 0) cursor.getString(emojiCol) ?: "🍲" else "🍲"
                val price = if (priceCol >= 0) cursor.getInt(priceCol) else 0
                val quantity = if (qtyCol >= 0) cursor.getInt(qtyCol) else 1
                val imageRes = if (imgCol >= 0) cursor.getInt(imgCol) else 0
                val dbUri = if (uriIndex >= 0) cursor.getString(uriIndex) ?: "" else ""
                val imageUri = dbUri.ifEmpty { SampleData.resolveFoodImageUri(foodId) }
                val note = if (noteIndex >= 0) cursor.getString(noteIndex) ?: "" else ""

                val actualImageRes = SampleData.resolveFoodImage(foodId, name, imageRes)

                items.add(
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
        return items
    }

    /**
     * Cập nhật trạng thái tiến trình đơn hàng trong SQLite.
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus, step: Int) {
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_ORDER_STATUS, status.name)
            put(DatabaseHelper.COL_ORDER_STEP, step)
            put("step", step)
        }
        val cleanId = orderId.removePrefix("#")
        db.update(DatabaseHelper.TABLE_ORDERS, values, "id = ? OR order_id = ? OR id = ? OR order_id = ?", arrayOf(orderId, orderId, cleanId, cleanId))
    }

    fun cancelOrder(orderId: String) {
        updateOrderStatus(orderId, OrderStatus.CANCELLED, -1)
    }

    /** Admin: Lấy toàn bộ đơn hàng của tất cả người dùng trong hệ thống */
    fun getAllOrders(): List<Order> {
        val list = mutableListOf<Order>()
        try {
            val db = dbReadable
            ensureOrdersTable(db)
            val cursor = db.query(
                DatabaseHelper.TABLE_ORDERS,
                null,
                null,
                null,
                null,
                null,
                "${DatabaseHelper.COL_ORDER_CREATED_AT} DESC"
            )
            while (cursor.moveToNext()) {
                list.add(cursorToOrder(cursor))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
