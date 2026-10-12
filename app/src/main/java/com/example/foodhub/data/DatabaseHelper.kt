package com.example.foodhub.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.data.model.User

/**
 * Quản lý cơ sở dữ liệu SQLite cho ứng dụng FoodHub.
 * Lưu trữ:
 * 1. Bảng "users": Tài khoản người dùng.
 * 2. Bảng "cart_items": Giỏ hàng theo từng user_id riêng biệt.
 * 3. Bảng "orders" & "order_items": Lịch sử đơn hàng theo từng user_id riêng biệt.
 * 4. Bảng "notifications": Thông báo thật và trạng thái đã đọc theo từng user_id riêng biệt.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "foodhub_app.db"
        private const val DATABASE_VERSION = 3

        // Bảng users
        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_EMAIL = "email"
        const val COL_PHONE = "phone"
        const val COL_PASSWORD = "password"
        const val COL_GENDER = "gender"
        const val COL_BIRTHDAY = "birthday"
        const val COL_JOB = "job"

        // Bảng cart_items (Giỏ hàng riêng theo từng user_id)
        const val TABLE_CART = "cart_items"
        const val COL_CART_ID = "id"
        const val COL_CART_USER_ID = "user_id"
        const val COL_CART_FOOD_ID = "food_id"
        const val COL_CART_NAME = "name"
        const val COL_CART_RESTAURANT = "restaurant"
        const val COL_CART_EMOJI = "emoji"
        const val COL_CART_PRICE = "price"
        const val COL_CART_QUANTITY = "quantity"
        const val COL_CART_IMAGE_RES = "image_res"

        // Bảng orders (Đơn hàng riêng theo từng user_id)
        const val TABLE_ORDERS = "orders"
        const val COL_ORDER_ID = "order_id"
        const val COL_ORDER_USER_ID = "user_id"
        const val COL_ORDER_RESTAURANT = "restaurant"
        const val COL_ORDER_CREATED_AT = "created_at"
        const val COL_ORDER_SHIPPING_FEE = "shipping_fee"
        const val COL_ORDER_RECEIVER = "receiver_name"
        const val COL_ORDER_PHONE = "phone"
        const val COL_ORDER_ADDRESS = "address"
        const val COL_ORDER_PAYMENT = "payment_method"
        const val COL_ORDER_STATUS = "status"
        const val COL_ORDER_STEP = "process_step"

        // Bảng order_items (Chi tiết món của từng đơn hàng)
        const val TABLE_ORDER_ITEMS = "order_items"
        const val COL_OI_ID = "id"
        const val COL_OI_ORDER_ID = "order_id"
        const val COL_OI_FOOD_ID = "food_id"
        const val COL_OI_NAME = "name"
        const val COL_OI_RESTAURANT = "restaurant"
        const val COL_OI_EMOJI = "emoji"
        const val COL_OI_PRICE = "price"
        const val COL_OI_QUANTITY = "quantity"
        const val COL_OI_IMAGE_RES = "image_res"

        // Bảng notifications (Thông báo riêng theo từng user_id)
        const val TABLE_NOTIFICATIONS = "notifications"
        const val COL_NOTIF_ID = "id"
        const val COL_NOTIF_USER_ID = "user_id"
        const val COL_NOTIF_TYPE = "type"
        const val COL_NOTIF_EMOJI = "emoji"
        const val COL_NOTIF_TITLE = "title"
        const val COL_NOTIF_MESSAGE = "message"
        const val COL_NOTIF_TIME = "time"
        const val COL_NOTIF_IS_READ = "is_read"
        const val COL_NOTIF_ORDER_ID = "order_id"
        const val COL_NOTIF_PROMO_CODE = "promo_code"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Tạo bảng users
        val createUsersTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PHONE TEXT,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_GENDER TEXT DEFAULT '',
                $COL_BIRTHDAY TEXT DEFAULT '',
                $COL_JOB TEXT DEFAULT ''
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        // Tạo bảng cart_items
        val createCartTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_CART (
                $COL_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CART_USER_ID INTEGER NOT NULL,
                $COL_CART_FOOD_ID INTEGER NOT NULL,
                $COL_CART_NAME TEXT NOT NULL,
                $COL_CART_RESTAURANT TEXT NOT NULL,
                $COL_CART_EMOJI TEXT,
                $COL_CART_PRICE INTEGER NOT NULL,
                $COL_CART_QUANTITY INTEGER NOT NULL,
                $COL_CART_IMAGE_RES INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createCartTable)

        // Tạo bảng orders
        val createOrdersTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_ORDERS (
                $COL_ORDER_ID TEXT PRIMARY KEY,
                $COL_ORDER_USER_ID INTEGER NOT NULL,
                $COL_ORDER_RESTAURANT TEXT NOT NULL,
                $COL_ORDER_CREATED_AT INTEGER NOT NULL,
                $COL_ORDER_SHIPPING_FEE INTEGER NOT NULL,
                $COL_ORDER_RECEIVER TEXT NOT NULL,
                $COL_ORDER_PHONE TEXT NOT NULL,
                $COL_ORDER_ADDRESS TEXT NOT NULL,
                $COL_ORDER_PAYMENT TEXT NOT NULL,
                $COL_ORDER_STATUS TEXT NOT NULL,
                $COL_ORDER_STEP INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createOrdersTable)

        // Tạo bảng order_items
        val createOrderItemsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_ORDER_ITEMS (
                $COL_OI_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_OI_ORDER_ID TEXT NOT NULL,
                $COL_OI_FOOD_ID INTEGER NOT NULL,
                $COL_OI_NAME TEXT NOT NULL,
                $COL_OI_RESTAURANT TEXT NOT NULL,
                $COL_OI_EMOJI TEXT,
                $COL_OI_PRICE INTEGER NOT NULL,
                $COL_OI_QUANTITY INTEGER NOT NULL,
                $COL_OI_IMAGE_RES INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createOrderItemsTable)

        // Tạo bảng notifications
        val createNotifTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                $COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIF_USER_ID INTEGER NOT NULL,
                $COL_NOTIF_TYPE TEXT NOT NULL,
                $COL_NOTIF_EMOJI TEXT NOT NULL,
                $COL_NOTIF_TITLE TEXT NOT NULL,
                $COL_NOTIF_MESSAGE TEXT NOT NULL,
                $COL_NOTIF_TIME INTEGER NOT NULL,
                $COL_NOTIF_IS_READ INTEGER DEFAULT 0,
                $COL_NOTIF_ORDER_ID TEXT,
                $COL_NOTIF_PROMO_CODE TEXT
            )
        """.trimIndent()
        db.execSQL(createNotifTable)

        // Tài khoản mẫu mặc định
        val defaultUser = ContentValues().apply {
            put(COL_NAME, "Trường Thanh Toàn")
            put(COL_EMAIL, "truong@email.com")
            put(COL_PHONE, "+84 123 456 789")
            put(COL_PASSWORD, "123456")
            put(COL_GENDER, "Nam")
            put(COL_BIRTHDAY, "01/01/2000")
            put(COL_JOB, "Lập trình viên")
        }
        db.insert(TABLE_USERS, null, defaultUser)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createCartTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_CART (
                    $COL_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_CART_USER_ID INTEGER NOT NULL,
                    $COL_CART_FOOD_ID INTEGER NOT NULL,
                    $COL_CART_NAME TEXT NOT NULL,
                    $COL_CART_RESTAURANT TEXT NOT NULL,
                    $COL_CART_EMOJI TEXT,
                    $COL_CART_PRICE INTEGER NOT NULL,
                    $COL_CART_QUANTITY INTEGER NOT NULL,
                    $COL_CART_IMAGE_RES INTEGER DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(createCartTable)

            val createOrdersTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_ORDERS (
                    $COL_ORDER_ID TEXT PRIMARY KEY,
                    $COL_ORDER_USER_ID INTEGER NOT NULL,
                    $COL_ORDER_RESTAURANT TEXT NOT NULL,
                    $COL_ORDER_CREATED_AT INTEGER NOT NULL,
                    $COL_ORDER_SHIPPING_FEE INTEGER NOT NULL,
                    $COL_ORDER_RECEIVER TEXT NOT NULL,
                    $COL_ORDER_PHONE TEXT NOT NULL,
                    $COL_ORDER_ADDRESS TEXT NOT NULL,
                    $COL_ORDER_PAYMENT TEXT NOT NULL,
                    $COL_ORDER_STATUS TEXT NOT NULL,
                    $COL_ORDER_STEP INTEGER DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(createOrdersTable)

            val createOrderItemsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_ORDER_ITEMS (
                    $COL_OI_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_OI_ORDER_ID TEXT NOT NULL,
                    $COL_OI_FOOD_ID INTEGER NOT NULL,
                    $COL_OI_NAME TEXT NOT NULL,
                    $COL_OI_RESTAURANT TEXT NOT NULL,
                    $COL_OI_EMOJI TEXT,
                    $COL_OI_PRICE INTEGER NOT NULL,
                    $COL_OI_QUANTITY INTEGER NOT NULL,
                    $COL_OI_IMAGE_RES INTEGER DEFAULT 0
                )
            """.trimIndent()
            db.execSQL(createOrderItemsTable)
        }

        if (oldVersion < 3) {
            val createNotifTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
                    $COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NOTIF_USER_ID INTEGER NOT NULL,
                    $COL_NOTIF_TYPE TEXT NOT NULL,
                    $COL_NOTIF_EMOJI TEXT NOT NULL,
                    $COL_NOTIF_TITLE TEXT NOT NULL,
                    $COL_NOTIF_MESSAGE TEXT NOT NULL,
                    $COL_NOTIF_TIME INTEGER NOT NULL,
                    $COL_NOTIF_IS_READ INTEGER DEFAULT 0,
                    $COL_NOTIF_ORDER_ID TEXT,
                    $COL_NOTIF_PROMO_CODE TEXT
                )
            """.trimIndent()
            db.execSQL(createNotifTable)
        }
    }

    // =========================================================================
    // 1. QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG (USERS)
    // =========================================================================

    fun registerUser(user: User): Long {
        if (isEmailExists(user.email)) return -1L
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, user.name)
            put(COL_EMAIL, user.email.trim().lowercase())
            put(COL_PHONE, user.phone.trim())
            put(COL_PASSWORD, user.password)
            put(COL_GENDER, user.gender)
            put(COL_BIRTHDAY, user.birthday)
            put(COL_JOB, user.job)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "LOWER($COL_EMAIL) = ?",
            arrayOf(email.trim().lowercase()),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun checkUser(account: String, password: String): User? {
        val db = readableDatabase
        val cleanAccount = account.trim().lowercase()
        val query = """
            SELECT * FROM $TABLE_USERS 
            WHERE (LOWER($COL_EMAIL) = ? OR $COL_PHONE = ?) 
              AND $COL_PASSWORD = ? 
            LIMIT 1
        """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, arrayOf(cleanAccount, account.trim(), password))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        return user
    }

    fun getUserByEmail(email: String): User? {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS,
            null,
            "LOWER($COL_EMAIL) = ?",
            arrayOf(email.trim().lowercase()),
            null,
            null,
            null
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        return user
    }

    fun updateUserProfile(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, user.name)
            put(COL_PHONE, user.phone)
            put(COL_EMAIL, user.email.trim().lowercase())
            put(COL_GENDER, user.gender)
            put(COL_BIRTHDAY, user.birthday)
            put(COL_JOB, user.job)
        }
        val rows = db.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(user.id.toString())
        )
        return rows > 0
    }

    private fun cursorToUser(cursor: Cursor): User {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
        val email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL))
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)) ?: ""
        val password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
        val gender = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)) ?: ""
        val birthday = cursor.getString(cursor.getColumnIndexOrThrow(COL_BIRTHDAY)) ?: ""
        val job = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOB)) ?: ""

        return User(
            id = id,
            name = name,
            email = email,
            phone = phone,
            password = password,
            gender = gender,
            birthday = birthday,
            job = job
        )
    }

    // =========================================================================
    // 2. QUẢN LÝ GIỎ HÀNG THEO TỪNG USER_ID
    // =========================================================================

    /**
     * Lấy toàn bộ món ăn trong giỏ hàng của user hiện tại từ SQLite.
     */
    fun getCartItems(userId: Long): List<CartItem> {
        val list = mutableListOf<CartItem>()
        if (userId <= 0) return list
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CART,
            null,
            "$COL_CART_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COL_CART_ID ASC"
        )
        while (cursor.moveToNext()) {
            val foodId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CART_FOOD_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CART_NAME))
            val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(COL_CART_RESTAURANT))
            val emoji = cursor.getString(cursor.getColumnIndexOrThrow(COL_CART_EMOJI)) ?: "🍲"
            val price = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CART_PRICE))
            val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CART_QUANTITY))
            val imageRes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CART_IMAGE_RES))

            list.add(
                CartItem(
                    foodId = foodId,
                    name = name,
                    restaurant = restaurant,
                    emoji = emoji,
                    price = price,
                    quantity = quantity,
                    imageRes = imageRes
                )
            )
        }
        cursor.close()
        return list
    }

    /**
     * Thêm hoặc cập nhật số lượng món trong giỏ hàng vào SQLite cho user_id.
     */
    fun saveOrUpdateCartItem(userId: Long, item: CartItem) {
        if (userId <= 0) return
        val db = writableDatabase
        val cursor = db.query(
            TABLE_CART,
            arrayOf(COL_CART_ID),
            "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ?",
            arrayOf(userId.toString(), item.foodId.toString()),
            null,
            null,
            null
        )
        val exists = cursor.moveToFirst()
        cursor.close()

        val values = ContentValues().apply {
            put(COL_CART_USER_ID, userId)
            put(COL_CART_FOOD_ID, item.foodId)
            put(COL_CART_NAME, item.name)
            put(COL_CART_RESTAURANT, item.restaurant)
            put(COL_CART_EMOJI, item.emoji)
            put(COL_CART_PRICE, item.price)
            put(COL_CART_QUANTITY, item.quantity)
            put(COL_CART_IMAGE_RES, item.imageRes)
        }

        if (exists) {
            db.update(
                TABLE_CART,
                values,
                "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ?",
                arrayOf(userId.toString(), item.foodId.toString())
            )
        } else {
            db.insert(TABLE_CART, null, values)
        }
    }

    /**
     * Xóa 1 món khỏi giỏ hàng trong SQLite.
     */
    fun removeCartItem(userId: Long, foodId: Int) {
        if (userId <= 0) return
        val db = writableDatabase
        db.delete(
            TABLE_CART,
            "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ?",
            arrayOf(userId.toString(), foodId.toString())
        )
    }

    /**
     * Xóa sạch giỏ hàng của user_id (sau khi thanh toán đặt hàng thành công).
     */
    fun clearCart(userId: Long) {
        if (userId <= 0) return
        val db = writableDatabase
        db.delete(TABLE_CART, "$COL_CART_USER_ID = ?", arrayOf(userId.toString()))
    }

    // =========================================================================
    // 3. QUẢN LÝ ĐƠN HÀNG (ORDERS & ORDER_ITEMS) THEO TỪNG USER_ID
    // =========================================================================

    /**
     * Lưu 1 đơn hàng mới và danh sách món của đơn hàng vào SQLite.
     */
    fun insertOrder(userId: Long, order: Order) {
        if (userId <= 0) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val orderValues = ContentValues().apply {
                put(COL_ORDER_ID, order.id)
                put(COL_ORDER_USER_ID, userId)
                put(COL_ORDER_RESTAURANT, order.restaurant)
                put(COL_ORDER_CREATED_AT, order.createdAt)
                put(COL_ORDER_SHIPPING_FEE, order.shippingFee)
                put(COL_ORDER_RECEIVER, order.receiverName)
                put(COL_ORDER_PHONE, order.phone)
                put(COL_ORDER_ADDRESS, order.address)
                put(COL_ORDER_PAYMENT, order.paymentMethod)
                put(COL_ORDER_STATUS, order.status.name)
                put(COL_ORDER_STEP, order.processStep)
            }
            db.insertWithOnConflict(TABLE_ORDERS, null, orderValues, SQLiteDatabase.CONFLICT_REPLACE)

            // Đảm bảo không trùng lặp các món khi cập nhật/lưu lại đơn hàng
            db.delete(TABLE_ORDER_ITEMS, "$COL_OI_ORDER_ID = ?", arrayOf(order.id))

            order.items.forEach { item ->
                val itemValues = ContentValues().apply {
                    put(COL_OI_ORDER_ID, order.id)
                    put(COL_OI_FOOD_ID, item.foodId)
                    put(COL_OI_NAME, item.name)
                    put(COL_OI_RESTAURANT, item.restaurant)
                    put(COL_OI_EMOJI, item.emoji)
                    put(COL_OI_PRICE, item.price)
                    put(COL_OI_QUANTITY, item.quantity)
                    put(COL_OI_IMAGE_RES, item.imageRes)
                }
                db.insert(TABLE_ORDER_ITEMS, null, itemValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Tự động xóa các đơn hàng cũ hơn số ngày quy định (mặc định 3 ngày).
     * Xóa đồng bộ cả bảng orders và order_items.
     */
    fun cleanOrdersOlderThan(days: Int = 3, userId: Long? = null): Int {
        val db = writableDatabase
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000L)
        val selection = if (userId != null && userId > 0) {
            "$COL_ORDER_CREATED_AT < ? AND $COL_ORDER_USER_ID = ?"
        } else {
            "$COL_ORDER_CREATED_AT < ?"
        }
        val selectionArgs = if (userId != null && userId > 0) {
            arrayOf(threshold.toString(), userId.toString())
        } else {
            arrayOf(threshold.toString())
        }

        val cursor = db.query(
            TABLE_ORDERS,
            arrayOf(COL_ORDER_ID),
            selection,
            selectionArgs,
            null, null, null
        )
        val expiredIds = mutableListOf<String>()
        while (cursor.moveToNext()) {
            expiredIds.add(cursor.getString(0))
        }
        cursor.close()

        if (expiredIds.isEmpty()) return 0

        db.beginTransaction()
        try {
            expiredIds.forEach { orderId ->
                db.delete(TABLE_ORDER_ITEMS, "$COL_OI_ORDER_ID = ?", arrayOf(orderId))
                db.delete(TABLE_ORDERS, "$COL_ORDER_ID = ?", arrayOf(orderId))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return expiredIds.size
    }

    /**
     * Lấy toàn bộ đơn hàng của user_id từ SQLite (chỉ giữ đơn hàng trong vòng 3 ngày).
     */
    fun getOrdersForUser(userId: Long): List<Order> {
        val orders = mutableListOf<Order>()
        if (userId <= 0) return orders

        // Tự động dọn dẹp các đơn hàng quá 3 ngày
        cleanOrdersOlderThan(3, userId)

        val db = readableDatabase

        val cursor = db.query(
            TABLE_ORDERS,
            null,
            "$COL_ORDER_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COL_ORDER_CREATED_AT DESC"
        )

        while (cursor.moveToNext()) {
            val orderId = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_ID))
            val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_RESTAURANT))
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ORDER_CREATED_AT))
            val shippingFee = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORDER_SHIPPING_FEE))
            val receiverName = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_RECEIVER))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_PHONE))
            val address = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_ADDRESS))
            val paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_PAYMENT))
            val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORDER_STATUS))
            val processStep = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORDER_STEP))

            val status = try {
                OrderStatus.valueOf(statusStr)
            } catch (_: Exception) {
                OrderStatus.PROCESSING
            }

            val items = getItemsForOrder(db, orderId)

            orders.add(
                Order(
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
                    processStep = processStep
                )
            )
        }
        cursor.close()
        return orders
    }

    private fun getItemsForOrder(db: SQLiteDatabase, orderId: String): List<CartItem> {
        val items = mutableListOf<CartItem>()
        val cursor = db.query(
            TABLE_ORDER_ITEMS,
            null,
            "$COL_OI_ORDER_ID = ?",
            arrayOf(orderId),
            null,
            null,
            "$COL_OI_ID ASC"
        )
        while (cursor.moveToNext()) {
            val foodId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OI_FOOD_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_OI_NAME))
            val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(COL_OI_RESTAURANT))
            val emoji = cursor.getString(cursor.getColumnIndexOrThrow(COL_OI_EMOJI)) ?: "🍲"
            val price = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OI_PRICE))
            val quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OI_QUANTITY))
            val imageRes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_OI_IMAGE_RES))

            items.add(
                CartItem(
                    foodId = foodId,
                    name = name,
                    restaurant = restaurant,
                    emoji = emoji,
                    price = price,
                    quantity = quantity,
                    imageRes = imageRes
                )
            )
        }
        cursor.close()
        return items
    }

    /**
     * Cập nhật trạng thái tiến trình đơn hàng trong SQLite.
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus, processStep: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ORDER_STATUS, status.name)
            put(COL_ORDER_STEP, processStep)
        }
        db.update(TABLE_ORDERS, values, "$COL_ORDER_ID = ?", arrayOf(orderId))
    }

    /**
     * Hủy đơn hàng trong SQLite.
     */
    fun cancelOrder(orderId: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ORDER_STATUS, OrderStatus.CANCELLED.name)
            put(COL_ORDER_STEP, -1)
        }
        val rows = db.update(TABLE_ORDERS, values, "$COL_ORDER_ID = ?", arrayOf(orderId))
        return rows > 0
    }

    // =========================================================================
    // 4. QUẢN LÝ THÔNG BÁO (NOTIFICATIONS) THEO TỪNG USER_ID
    // =========================================================================

    /**
     * Lưu 1 thông báo mới vào SQLite. Trả về id vừa tạo.
     */
    fun insertNotification(userId: Long, notif: AppNotification): Long {
        if (userId <= 0) return -1L
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_USER_ID, userId)
            put(COL_NOTIF_TYPE, notif.type.name)
            put(COL_NOTIF_EMOJI, notif.emoji)
            put(COL_NOTIF_TITLE, notif.title)
            put(COL_NOTIF_MESSAGE, notif.message)
            put(COL_NOTIF_TIME, notif.time)
            put(COL_NOTIF_IS_READ, if (notif.isRead) 1 else 0)
            put(COL_NOTIF_ORDER_ID, notif.orderId)
            put(COL_NOTIF_PROMO_CODE, notif.promoCode)
        }
        return db.insert(TABLE_NOTIFICATIONS, null, values)
    }

    /**
     * Tự động xóa các thông báo cũ hơn số ngày quy định (mặc định 3 ngày) khỏi SQLite.
     */
    fun cleanNotificationsOlderThan(days: Int = 3, userId: Long? = null): Int {
        val db = writableDatabase
        val threshold = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000L)
        val selection = if (userId != null && userId > 0) {
            "$COL_NOTIF_TIME < ? AND $COL_NOTIF_USER_ID = ?"
        } else {
            "$COL_NOTIF_TIME < ?"
        }
        val selectionArgs = if (userId != null && userId > 0) {
            arrayOf(threshold.toString(), userId.toString())
        } else {
            arrayOf(threshold.toString())
        }
        return db.delete(TABLE_NOTIFICATIONS, selection, selectionArgs)
    }

    /**
     * Lấy toàn bộ thông báo của user_id từ SQLite, sắp xếp mới nhất lên đầu (chỉ giữ trong vòng 3 ngày).
     */
    fun getNotifications(userId: Long): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        if (userId <= 0) return list

        // Tự động dọn dẹp các thông báo quá 3 ngày
        cleanNotificationsOlderThan(3, userId)

        val db = readableDatabase
        val cursor = db.query(
            TABLE_NOTIFICATIONS,
            null,
            "$COL_NOTIF_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COL_NOTIF_TIME DESC, $COL_NOTIF_ID DESC"
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTIF_ID))
            val typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TYPE))
            val emoji = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_EMOJI))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_TITLE))
            val message = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_MESSAGE))
            val time = cursor.getLong(cursor.getColumnIndexOrThrow(COL_NOTIF_TIME))
            val isRead = cursor.getInt(cursor.getColumnIndexOrThrow(COL_NOTIF_IS_READ)) == 1
            val orderId = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_ORDER_ID))
            val promoCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTIF_PROMO_CODE))

            val type = try {
                NotificationType.valueOf(typeStr)
            } catch (_: Exception) {
                NotificationType.SYSTEM
            }

            list.add(
                AppNotification(
                    id = id,
                    type = type,
                    emoji = emoji,
                    title = title,
                    message = message,
                    time = time,
                    isRead = isRead,
                    orderId = orderId,
                    promoCode = promoCode
                )
            )
        }
        cursor.close()
        return list
    }

    /**
     * Cập nhật trạng thái đã đọc cho 1 thông báo trong SQLite.
     */
    fun markNotificationAsRead(userId: Long, notifId: Long): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_READ, 1)
        }
        val rows = db.update(
            TABLE_NOTIFICATIONS,
            values,
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_ID = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
        return rows > 0
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc trong SQLite (hoặc theo loại).
     */
    fun markAllNotificationsAsRead(userId: Long, type: NotificationType?): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_READ, 1)
        }
        val rows = if (type == null) {
            db.update(
                TABLE_NOTIFICATIONS,
                values,
                "$COL_NOTIF_USER_ID = ?",
                arrayOf(userId.toString())
            )
        } else {
            db.update(
                TABLE_NOTIFICATIONS,
                values,
                "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_TYPE = ?",
                arrayOf(userId.toString(), type.name)
            )
        }
        return rows > 0
    }

    /**
     * Xóa 1 thông báo trong SQLite.
     */
    fun deleteNotification(userId: Long, notifId: Long): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        val rows = db.delete(
            TABLE_NOTIFICATIONS,
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_ID = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
        return rows > 0
    }

    /**
     * Xóa tất cả thông báo trong SQLite (hoặc theo loại).
     */
    fun deleteAllNotifications(userId: Long, type: NotificationType?): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        val rows = if (type == null) {
            db.delete(
                TABLE_NOTIFICATIONS,
                "$COL_NOTIF_USER_ID = ?",
                arrayOf(userId.toString())
            )
        } else {
            db.delete(
                TABLE_NOTIFICATIONS,
                "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_TYPE = ?",
                arrayOf(userId.toString(), type.name)
            )
        }
        return rows > 0
    }
}
