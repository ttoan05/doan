package com.example.foodhub.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.data.model.Category
import com.example.foodhub.data.model.Food
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.data.model.Review
import com.example.foodhub.data.model.SavedAddress
import com.example.foodhub.data.model.User
import com.example.foodhub.utils.SampleData

/**
 * Quản lý cơ sở dữ liệu SQLite cho ứng dụng FoodHub.
 * Lưu trữ:
 * 1. Bảng "users": Tài khoản người dùng & vai trò (user / admin).
 * 2. Bảng "cart_items": Giỏ hàng theo từng user_id riêng biệt.
 * 3. Bảng "orders" & "order_items": Lịch sử đơn hàng theo từng user_id riêng biệt.
 * 4. Bảng "notifications": Thông báo thật và trạng thái đã đọc theo từng user_id riêng biệt.
 * 5. Bảng "addresses": Sổ địa chỉ giao hàng.
 * 6. Bảng "reviews": Đánh giá và chấm sao món ăn / quán.
 * 7. Bảng "categories": Danh mục món ăn hệ thống (Admin quản lý).
 * 8. Bảng "foods": Danh sách món ăn hệ thống (Admin thêm / sửa / xóa / bật tắt còn hàng).
 * 9. Bảng "restaurants": Danh sách nhà hàng / quán ăn hệ thống (Admin thêm / sửa / xóa).
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "foodhub_app.db"
        private const val DATABASE_VERSION = 13

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
        const val COL_ROLE = "role" // "user" hoặc "admin"

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
        const val COL_CART_IMAGE_URI = "image_uri"
        const val COL_CART_NOTE = "note"

        // Bảng orders (Lịch sử đơn hàng riêng theo từng user_id)
        const val TABLE_ORDERS = "orders"
        const val COL_ORDER_ID = "id"
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
        const val COL_ORDER_NOTE = "order_note"
        const val COL_ORDER_VOUCHER = "voucher_code"
        const val COL_ORDER_DISCOUNT = "discount"

        // Bảng order_items (Chi tiết món ăn trong đơn hàng)
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
        const val COL_OI_IMAGE_URI = "image_uri"
        const val COL_OI_NOTE = "note"

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

        // Bảng addresses (Sổ địa chỉ giao hàng quen thuộc)
        const val TABLE_ADDRESSES = "addresses"
        const val COL_ADDR_ID = "id"
        const val COL_ADDR_USER_ID = "user_id"
        const val COL_ADDR_LABEL = "label"
        const val COL_ADDR_RECEIVER = "receiver"
        const val COL_ADDR_PHONE = "phone"
        const val COL_ADDR_ADDRESS = "address"
        const val COL_ADDR_IS_DEFAULT = "is_default"

        // Bảng reviews (Đánh giá sau giao hàng)
        const val TABLE_REVIEWS = "reviews"
        const val COL_REVIEW_ID = "id"
        const val COL_REVIEW_USER_ID = "user_id"
        const val COL_REVIEW_ORDER_ID = "order_id"
        const val COL_REVIEW_RESTAURANT = "restaurant"
        const val COL_REVIEW_STARS = "stars"
        const val COL_REVIEW_COMMENT = "comment"
        const val COL_REVIEW_CREATED_AT = "created_at"

        // Bảng categories (Danh mục món ăn hệ thống - Admin)
        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_IMAGE_URI = "image_uri"

        // Bảng foods (Danh sách món ăn hệ thống - Admin)
        const val TABLE_FOODS = "foods"
        const val COL_FOOD_ID = "id"
        const val COL_FOOD_NAME = "name"
        const val COL_FOOD_RESTAURANT = "restaurant"
        const val COL_FOOD_RATING = "rating"
        const val COL_FOOD_REVIEWS = "reviews"
        const val COL_FOOD_PRICE = "price"
        const val COL_FOOD_CATEGORY = "category"
        const val COL_FOOD_EMOJI = "emoji"
        const val COL_FOOD_BG_START = "bg_start"
        const val COL_FOOD_BG_END = "bg_end"
        const val COL_FOOD_IMAGE_RES = "image_res"
        const val COL_FOOD_IMAGE_URI = "image_uri"
        const val COL_FOOD_SIZES = "sizes"
        const val COL_FOOD_TOPPINGS = "toppings"
        const val COL_FOOD_IS_AVAILABLE = "is_available"

        // Bảng restaurants (Danh sách quán ăn / nhà hàng - Admin)
        const val TABLE_RESTAURANTS = "restaurants"
        const val COL_REST_ID = "id"
        const val COL_REST_NAME = "name"
        const val COL_REST_DESC = "desc"
        const val COL_REST_RATING = "rating"
        const val COL_REST_TIME = "time"
        const val COL_REST_DIST = "dist"
        const val COL_REST_BADGE = "badge"
        const val COL_REST_BG_START = "bg_start"
        const val COL_REST_BG_END = "bg_end"
        const val COL_REST_IMAGE_RES = "image_res"
        const val COL_REST_IMAGE_URI = "image_uri"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(createUsersTable())
        db.execSQL(createCartTable())
        db.execSQL(createOrdersTable())
        db.execSQL(createOrderItemsTable())
        db.execSQL(createNotifTable())
        db.execSQL(createAddressesTable())
        db.execSQL(createReviewsTable())
        db.execSQL(createCategoriesTable())
        db.execSQL(createFoodsTable())
        db.execSQL(createRestaurantsTable())

        // Tài khoản mẫu mặc định
        val defaultUser = ContentValues().apply {
            put(COL_NAME, "Trương Thanh Toàn")
            put(COL_EMAIL, "truongthanhtoan01012004@gmail.com")
            put(COL_PHONE, "0867656271")
            put(COL_PASSWORD, "123456")
            put(COL_GENDER, "Nam")
            put(COL_BIRTHDAY, "01/01/2004")
            put(COL_JOB, "Kỹ sư phần mềm")
            put(COL_ROLE, "user")
        }
        db.insert(TABLE_USERS, null, defaultUser)

        // Tài khoản Admin hệ thống
        val adminUser = ContentValues().apply {
            put(COL_NAME, "Quản trị viên FoodHub")
            put(COL_EMAIL, "admin@foodhub.com")
            put(COL_PHONE, "1900 1234")
            put(COL_PASSWORD, "123456")
            put(COL_GENDER, "Nam")
            put(COL_BIRTHDAY, "01/01/1990")
            put(COL_JOB, "Quản lý hệ thống")
            put(COL_ROLE, "admin")
        }
        db.insert(TABLE_USERS, null, adminUser)

        seedInitialCategories(db)
        seedInitialFoods(db)
        seedInitialRestaurants(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            // Đảm bảo tất cả bảng cần thiết luôn tồn tại
            db.execSQL(createUsersTable())
            db.execSQL(createCartTable())
            db.execSQL(createOrdersTable())
            db.execSQL(createOrderItemsTable())
            db.execSQL(createNotifTable())
            db.execSQL(createAddressesTable())
            db.execSQL(createReviewsTable())
            db.execSQL(createCategoriesTable())
            db.execSQL(createFoodsTable())
            db.execSQL(createRestaurantsTable())

            // Thêm các cột nếu phiên bản cũ chưa có
            addColumnIfMissing(db, TABLE_USERS, COL_ROLE, "TEXT DEFAULT 'user'")
            addColumnIfMissing(db, TABLE_CART, COL_CART_NOTE, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_CART, COL_CART_IMAGE_RES, "INTEGER DEFAULT 0")
            addColumnIfMissing(db, TABLE_CART, COL_CART_IMAGE_URI, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_NOTE, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_IMAGE_RES, "INTEGER DEFAULT 0")
            addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_IMAGE_URI, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_NOTE, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_VOUCHER, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_DISCOUNT, "INTEGER DEFAULT 0")
            addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_IS_AVAILABLE, "INTEGER DEFAULT 1")
            ensureFoodsTable(db)
            ensureRestaurantsTable(db)
            ensureOrdersTable(db)
            ensureOrderItemsTable(db)

            // Cập nhật lại resource ID chuẩn cho các món ăn và quán mặc định trong DB
            try {
                SampleData.DEFAULT_FOODS.forEach { f ->
                    val cv = ContentValues().apply {
                        put(COL_FOOD_IMAGE_RES, f.imageRes)
                    }
                    db.update(TABLE_FOODS, cv, "$COL_FOOD_ID = ?", arrayOf(f.id.toString()))
                }
                SampleData.DEFAULT_RESTAURANTS.forEach { r ->
                    val cv = ContentValues().apply {
                        put(COL_REST_IMAGE_RES, r.imageRes)
                    }
                    db.update(TABLE_RESTAURANTS, cv, "$COL_REST_ID = ?", arrayOf(r.id.toString()))
                }
            } catch (_: Throwable) {}

            // Seed admin account nếu chưa có
            try {
                val cursorAdmin = db.query(TABLE_USERS, arrayOf(COL_ID), "$COL_EMAIL = ?", arrayOf("admin@foodhub.com"), null, null, null)
                if (!cursorAdmin.moveToFirst()) {
                    val adminUser = ContentValues().apply {
                        put(COL_NAME, "Quản trị viên FoodHub")
                        put(COL_EMAIL, "admin@foodhub.com")
                        put(COL_PHONE, "1900 1234")
                        put(COL_PASSWORD, "123456")
                        put(COL_GENDER, "Nam")
                        put(COL_BIRTHDAY, "01/01/1990")
                        put(COL_JOB, "Quản lý hệ thống")
                        put(COL_ROLE, "admin")
                    }
                    db.insert(TABLE_USERS, null, adminUser)
                }
                cursorAdmin.close()
            } catch (_: Throwable) {}

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createUsersTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_USERS (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NAME TEXT NOT NULL,
            $COL_EMAIL TEXT UNIQUE NOT NULL,
            $COL_PHONE TEXT NOT NULL,
            $COL_PASSWORD TEXT NOT NULL,
            $COL_GENDER TEXT DEFAULT '',
            $COL_BIRTHDAY TEXT DEFAULT '',
            $COL_JOB TEXT DEFAULT '',
            $COL_ROLE TEXT DEFAULT 'user'
        )
    """.trimIndent()

    private fun createCartTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_CART (
            $COL_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_CART_USER_ID INTEGER NOT NULL,
            $COL_CART_FOOD_ID INTEGER NOT NULL,
            $COL_CART_NAME TEXT NOT NULL,
            $COL_CART_RESTAURANT TEXT NOT NULL,
            $COL_CART_EMOJI TEXT DEFAULT '🍲',
            $COL_CART_PRICE INTEGER NOT NULL,
            $COL_CART_QUANTITY INTEGER NOT NULL,
            $COL_CART_IMAGE_RES INTEGER DEFAULT 0,
            $COL_CART_IMAGE_URI TEXT DEFAULT '',
            $COL_CART_NOTE TEXT DEFAULT ''
        )
    """.trimIndent()

    private fun createOrdersTable(): String = """
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
            $COL_ORDER_STEP INTEGER DEFAULT 0,
            $COL_ORDER_NOTE TEXT DEFAULT '',
            $COL_ORDER_VOUCHER TEXT DEFAULT '',
            $COL_ORDER_DISCOUNT INTEGER DEFAULT 0
        )
    """.trimIndent()

    private fun createOrderItemsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_ORDER_ITEMS (
            $COL_OI_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_OI_ORDER_ID TEXT NOT NULL,
            $COL_OI_FOOD_ID INTEGER NOT NULL,
            $COL_OI_NAME TEXT NOT NULL,
            $COL_OI_RESTAURANT TEXT NOT NULL,
            $COL_OI_EMOJI TEXT DEFAULT '🍲',
            $COL_OI_PRICE INTEGER NOT NULL,
            $COL_OI_QUANTITY INTEGER NOT NULL,
            $COL_OI_IMAGE_RES INTEGER DEFAULT 0,
            $COL_OI_IMAGE_URI TEXT DEFAULT '',
            $COL_OI_NOTE TEXT DEFAULT ''
        )
    """.trimIndent()

    private fun createNotifTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_NOTIFICATIONS (
            $COL_NOTIF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NOTIF_USER_ID INTEGER NOT NULL,
            $COL_NOTIF_TYPE TEXT NOT NULL,
            $COL_NOTIF_EMOJI TEXT NOT NULL,
            $COL_NOTIF_TITLE TEXT NOT NULL,
            $COL_NOTIF_MESSAGE TEXT NOT NULL,
            $COL_NOTIF_TIME INTEGER NOT NULL,
            $COL_NOTIF_IS_READ INTEGER DEFAULT 0,
            $COL_NOTIF_ORDER_ID TEXT DEFAULT '',
            $COL_NOTIF_PROMO_CODE TEXT DEFAULT ''
        )
    """.trimIndent()

    private fun createAddressesTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_ADDRESSES (
            $COL_ADDR_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_ADDR_USER_ID INTEGER NOT NULL,
            $COL_ADDR_LABEL TEXT NOT NULL,
            $COL_ADDR_RECEIVER TEXT NOT NULL,
            $COL_ADDR_PHONE TEXT NOT NULL,
            $COL_ADDR_ADDRESS TEXT NOT NULL,
            $COL_ADDR_IS_DEFAULT INTEGER DEFAULT 0
        )
    """.trimIndent()

    private fun createReviewsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_REVIEWS (
            $COL_REVIEW_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REVIEW_USER_ID INTEGER NOT NULL,
            $COL_REVIEW_ORDER_ID TEXT NOT NULL,
            $COL_REVIEW_RESTAURANT TEXT NOT NULL,
            $COL_REVIEW_STARS INTEGER NOT NULL,
            $COL_REVIEW_COMMENT TEXT DEFAULT '',
            $COL_REVIEW_CREATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    private fun createCategoriesTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
            $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_CAT_NAME TEXT UNIQUE NOT NULL,
            $COL_CAT_IMAGE_URI TEXT DEFAULT ''
        )
    """.trimIndent()

    /** Bảng danh sách món ăn (foods). */
    private fun createFoodsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_FOODS (
            $COL_FOOD_ID INTEGER PRIMARY KEY,
            $COL_FOOD_NAME TEXT NOT NULL,
            $COL_FOOD_RESTAURANT TEXT NOT NULL,
            $COL_FOOD_RATING REAL DEFAULT 5.0,
            $COL_FOOD_REVIEWS INTEGER DEFAULT 0,
            $COL_FOOD_PRICE TEXT NOT NULL,
            $COL_FOOD_CATEGORY TEXT NOT NULL,
            $COL_FOOD_EMOJI TEXT DEFAULT '🍲',
            $COL_FOOD_BG_START INTEGER DEFAULT 0,
            $COL_FOOD_BG_END INTEGER DEFAULT 0,
            $COL_FOOD_IMAGE_RES INTEGER DEFAULT 0,
            $COL_FOOD_IMAGE_URI TEXT DEFAULT '',
            $COL_FOOD_SIZES TEXT DEFAULT '',
            $COL_FOOD_TOPPINGS TEXT DEFAULT '',
            $COL_FOOD_IS_AVAILABLE INTEGER DEFAULT 1
        )
    """.trimIndent()

    private fun seedInitialCategories(db: SQLiteDatabase) {
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CATEGORIES", null)
        val count = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
        countCursor.close()
        if (count == 0) {
            listOf("Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống").forEach { cat ->
                val cv = ContentValues().apply { put(COL_CAT_NAME, cat) }
                db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
    }

    fun ensureFoodsTable(db: SQLiteDatabase) {
        db.execSQL(createFoodsTable())
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_NAME, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_RESTAURANT, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_RATING, "REAL DEFAULT 5.0")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_REVIEWS, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_PRICE, "TEXT DEFAULT '0đ'")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_CATEGORY, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_EMOJI, "TEXT DEFAULT '🍲'")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_BG_START, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_BG_END, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_IMAGE_RES, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_IMAGE_URI, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_SIZES, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_TOPPINGS, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_FOODS, COL_FOOD_IS_AVAILABLE, "INTEGER DEFAULT 1")
    }

    fun ensureRestaurantsTable(db: SQLiteDatabase) {
        db.execSQL(createRestaurantsTable())
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_NAME, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_DESC, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_RATING, "REAL DEFAULT 5.0")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_TIME, "TEXT DEFAULT '20-30 phút'")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_DIST, "TEXT DEFAULT '1.5 km'")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_BADGE, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_BG_START, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_BG_END, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_IMAGE_RES, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_RESTAURANTS, COL_REST_IMAGE_URI, "TEXT DEFAULT ''")
    }

    fun ensureOrdersTable(db: SQLiteDatabase) {
        db.execSQL(createOrdersTable())
        addColumnIfMissing(db, TABLE_ORDERS, "order_id", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, "id", "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_USER_ID, "INTEGER NOT NULL DEFAULT 1")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_RESTAURANT, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_CREATED_AT, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_SHIPPING_FEE, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_RECEIVER, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_PHONE, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_ADDRESS, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_PAYMENT, "TEXT DEFAULT 'COD'")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_STATUS, "TEXT DEFAULT 'PROCESSING'")
        addColumnIfMissing(db, TABLE_ORDERS, "process_step", "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDERS, "step", "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_NOTE, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_VOUCHER, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDERS, COL_ORDER_DISCOUNT, "INTEGER DEFAULT 0")

        // Tự động đồng bộ order_id và id nếu một trong hai bị trống
        try {
            db.execSQL("UPDATE $TABLE_ORDERS SET id = order_id WHERE (id IS NULL OR id = '') AND order_id IS NOT NULL AND order_id != ''")
            db.execSQL("UPDATE $TABLE_ORDERS SET order_id = id WHERE (order_id IS NULL OR order_id = '') AND id IS NOT NULL AND id != ''")
        } catch (_: Throwable) {}
    }

    fun ensureOrderItemsTable(db: SQLiteDatabase) {
        db.execSQL(createOrderItemsTable())
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_ORDER_ID, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_FOOD_ID, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_NAME, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_RESTAURANT, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_EMOJI, "TEXT DEFAULT '🍲'")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_PRICE, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_QUANTITY, "INTEGER DEFAULT 1")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_IMAGE_RES, "INTEGER DEFAULT 0")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_IMAGE_URI, "TEXT DEFAULT ''")
        addColumnIfMissing(db, TABLE_ORDER_ITEMS, COL_OI_NOTE, "TEXT DEFAULT ''")
    }

    fun seedInitialFoods(db: SQLiteDatabase) {
        try {
            ensureFoodsTable(db)
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FOODS", null)
            val count = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
            countCursor.close()
            if (count == 0) {
                SampleData.DEFAULT_FOODS.forEach { f ->
                    val cv = ContentValues().apply {
                        put(COL_FOOD_ID, f.id)
                        put(COL_FOOD_NAME, f.name)
                        put(COL_FOOD_RESTAURANT, f.restaurant)
                        put(COL_FOOD_RATING, f.rating)
                        put(COL_FOOD_REVIEWS, f.reviews)
                        put(COL_FOOD_PRICE, f.price)
                        put(COL_FOOD_CATEGORY, f.category)
                        put(COL_FOOD_EMOJI, f.emoji)
                        put(COL_FOOD_BG_START, f.bgColorStart)
                        put(COL_FOOD_BG_END, f.bgColorEnd)
                        put(COL_FOOD_IMAGE_RES, f.imageRes)
                        put(COL_FOOD_IMAGE_URI, "")
                        put(COL_FOOD_SIZES, "")
                        put(COL_FOOD_TOPPINGS, "")
                        put(COL_FOOD_IS_AVAILABLE, if (f.isAvailable) 1 else 0)
                    }
                    db.insertWithOnConflict(TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Bảng danh sách nhà hàng / quán ăn (restaurants). */
    private fun createRestaurantsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_RESTAURANTS (\
            $COL_REST_ID INTEGER PRIMARY KEY,
            $COL_REST_NAME TEXT NOT NULL,
            $COL_REST_DESC TEXT NOT NULL,
            $COL_REST_RATING REAL DEFAULT 5.0,
            $COL_REST_TIME TEXT DEFAULT '20-30 phút',
            $COL_REST_DIST TEXT DEFAULT '1.5 km',
            $COL_REST_BADGE TEXT DEFAULT '',
            $COL_REST_BG_START INTEGER DEFAULT 0,
            $COL_REST_BG_END INTEGER DEFAULT 0,
            $COL_REST_IMAGE_RES INTEGER DEFAULT 0,
            $COL_REST_IMAGE_URI TEXT DEFAULT ''
        )
    """.trimIndent()

    private fun seedInitialRestaurants(db: SQLiteDatabase) {
        try {
            ensureRestaurantsTable(db)
            val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_RESTAURANTS", null)
            val count = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
            countCursor.close()
            if (count == 0) {
                SampleData.DEFAULT_RESTAURANTS.forEach { r ->
                    val cv = ContentValues().apply {
                        put(COL_REST_ID, r.id)
                        put(COL_REST_NAME, r.name)
                        put(COL_REST_DESC, r.desc)
                        put(COL_REST_RATING, r.rating)
                        put(COL_REST_TIME, r.time)
                        put(COL_REST_DIST, r.dist)
                        put(COL_REST_BADGE, r.badge)
                        put(COL_REST_BG_START, r.bgColorStart)
                        put(COL_REST_BG_END, r.bgColorEnd)
                        put(COL_REST_IMAGE_RES, r.imageRes)
                        put(COL_REST_IMAGE_URI, "")
                    }
                    db.insertWithOnConflict(TABLE_RESTAURANTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addColumnIfMissing(
        db: SQLiteDatabase,
        table: String,
        column: String,
        definition: String
    ) {
        try {
            val cursor = db.rawQuery("PRAGMA table_info($table)", null)
            var exists = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == column) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            put(COL_ROLE, if (user.role.isNotEmpty()) user.role else "user")
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

    fun updateUser(user: User): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, user.name)
            put(COL_EMAIL, user.email.trim().lowercase())
            put(COL_PHONE, user.phone.trim())
            put(COL_PASSWORD, user.password)
            put(COL_GENDER, user.gender)
            put(COL_BIRTHDAY, user.birthday)
            put(COL_JOB, user.job)
            if (user.role.isNotEmpty()) {
                put(COL_ROLE, user.role)
            }
        }
        return db.update(TABLE_USERS, values, "$COL_ID = ?", arrayOf(user.id.toString()))
    }

    fun updateUserProfile(user: User): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, user.name)
            put(COL_EMAIL, user.email.trim().lowercase())
            put(COL_PHONE, user.phone.trim())
            put(COL_GENDER, user.gender)
            put(COL_BIRTHDAY, user.birthday)
            put(COL_JOB, user.job)
        }
        val rows = db.update(TABLE_USERS, values, "$COL_ID = ?", arrayOf(user.id.toString()))
        return rows > 0
    }

    fun updateUserPassword(userId: Long, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PASSWORD, newPassword)
        }
        val rows = db.update(TABLE_USERS, values, "$COL_ID = ?", arrayOf(userId.toString()))
        return rows > 0
    }

    private fun cursorToUser(cursor: Cursor): User {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME))
        val email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL))
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE))
        val password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD))
        val gender = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)) ?: ""
        val birthday = cursor.getString(cursor.getColumnIndexOrThrow(COL_BIRTHDAY)) ?: ""
        val job = cursor.getString(cursor.getColumnIndexOrThrow(COL_JOB)) ?: ""
        val roleColIndex = cursor.getColumnIndex(COL_ROLE)
        val role = if (roleColIndex >= 0) cursor.getString(roleColIndex) ?: "user" else "user"
        return User(id, name, email, phone, password, gender, birthday, job, role)
    }

    // =========================================================================
    // 2. QUẢN LÝ GIỎ HÀNG THEO TỪNG USER_ID
    // =========================================================================

    fun getCartItems(userId: Long): List<CartItem> {
        val list = mutableListOf<CartItem>()
        if (userId <= 0) return list
        try {
            val db = readableDatabase
            db.execSQL(createCartTable())
            addColumnIfMissing(db, TABLE_CART, COL_CART_IMAGE_URI, "TEXT DEFAULT ''")

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
                val uriIndex = cursor.getColumnIndex(COL_CART_IMAGE_URI)
                val dbUri = if (uriIndex >= 0) cursor.getString(uriIndex) ?: "" else ""
                val imageUri = dbUri.ifEmpty { SampleData.resolveFoodImageUri(foodId) }
                val noteIndex = cursor.getColumnIndex(COL_CART_NOTE)
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
            val db = writableDatabase
            db.execSQL(createCartTable())
            addColumnIfMissing(db, TABLE_CART, COL_CART_IMAGE_URI, "TEXT DEFAULT ''")

            // Tìm món cùng food_id và cùng ghi chú
            val cursor = db.query(
                TABLE_CART,
                arrayOf(COL_CART_ID, COL_CART_QUANTITY),
                "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ? AND $COL_CART_NOTE = ?",
                arrayOf(userId.toString(), item.foodId.toString(), item.note),
                null,
                null,
                null
            )
            val actualImageRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
            val finalImageUri = if (!item.imageUri.isNullOrEmpty()) item.imageUri else (SampleData.resolveFoodImageUri(item.foodId) ?: "")
            val values = ContentValues().apply {
                put(COL_CART_USER_ID, userId)
                put(COL_CART_FOOD_ID, item.foodId)
                put(COL_CART_NAME, item.name)
                put(COL_CART_RESTAURANT, item.restaurant)
                put(COL_CART_EMOJI, item.emoji)
                put(COL_CART_PRICE, item.price)
                put(COL_CART_QUANTITY, item.quantity)
                put(COL_CART_IMAGE_RES, actualImageRes)
                put(COL_CART_IMAGE_URI, finalImageUri)
                put(COL_CART_NOTE, item.note)
            }
            if (cursor.moveToFirst()) {
                val cartId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CART_ID))
                db.update(TABLE_CART, values, "$COL_CART_ID = ?", arrayOf(cartId.toString()))
            } else {
                db.insert(TABLE_CART, null, values)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateCartQuantity(userId: Long, foodId: Int, note: String, quantity: Int) {
        if (userId <= 0) return
        val db = writableDatabase
        if (quantity <= 0) {
            db.delete(
                TABLE_CART,
                "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ? AND $COL_CART_NOTE = ?",
                arrayOf(userId.toString(), foodId.toString(), note)
            )
        } else {
            val values = ContentValues().apply { put(COL_CART_QUANTITY, quantity) }
            db.update(
                TABLE_CART,
                values,
                "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ? AND $COL_CART_NOTE = ?",
                arrayOf(userId.toString(), foodId.toString(), note)
            )
        }
    }

    fun removeCartItem(userId: Long, foodId: Int, note: String) {
        if (userId <= 0) return
        val db = writableDatabase
        db.delete(
            TABLE_CART,
            "$COL_CART_USER_ID = ? AND $COL_CART_FOOD_ID = ? AND $COL_CART_NOTE = ?",
            arrayOf(userId.toString(), foodId.toString(), note)
        )
    }

    fun clearCart(userId: Long) {
        if (userId <= 0) return
        val db = writableDatabase
        db.delete(TABLE_CART, "$COL_CART_USER_ID = ?", arrayOf(userId.toString()))
    }

    // =========================================================================
    // 3. QUẢN LÝ ĐƠN HÀNG THEO TỪNG USER_ID
    // =========================================================================

    /**
     * Thêm đơn hàng mới và toàn bộ món chi tiết vào SQLite theo userId.
     */
    fun insertOrder(userId: Long, order: Order): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        try {
            ensureOrdersTable(db)
            ensureOrderItemsTable(db)

            db.beginTransaction()
            try {
                val orderValues = ContentValues().apply {
                    put("id", order.id)
                    put("order_id", order.id)
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
                    put("step", order.processStep)
                    put(COL_ORDER_NOTE, order.orderNote)
                    put(COL_ORDER_VOUCHER, order.voucherCode)
                    put(COL_ORDER_DISCOUNT, order.discount)
                }
                val res = db.insertWithOnConflict(TABLE_ORDERS, null, orderValues, SQLiteDatabase.CONFLICT_REPLACE)
                if (res == -1L) return false

                order.items.forEach { item ->
                    val actualImageRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
                    val finalImageUri = if (!item.imageUri.isNullOrEmpty()) item.imageUri else (SampleData.resolveFoodImageUri(item.foodId) ?: "")
                    val itemValues = ContentValues().apply {
                        put(COL_OI_ORDER_ID, order.id)
                        put(COL_OI_FOOD_ID, item.foodId)
                        put(COL_OI_NAME, item.name)
                        put(COL_OI_RESTAURANT, item.restaurant)
                        put(COL_OI_EMOJI, item.emoji)
                        put(COL_OI_PRICE, item.price)
                        put(COL_OI_QUANTITY, item.quantity)
                        put(COL_OI_IMAGE_RES, actualImageRes)
                        put(COL_OI_IMAGE_URI, finalImageUri)
                        put(COL_OI_NOTE, item.note)
                    }
                    db.insert(TABLE_ORDER_ITEMS, null, itemValues)
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

    private fun cursorToOrder(cursor: Cursor): Order {
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
            val db = readableDatabase
            ensureOrdersTable(db)
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
            val db = readableDatabase
            ensureOrdersTable(db)
            val cleanId = orderId.removePrefix("#")
            val cursor = db.query(
                TABLE_ORDERS,
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
            val db = readableDatabase
            ensureOrderItemsTable(db)

            val cleanId = orderId.removePrefix("#")
            val cursor = db.query(
                TABLE_ORDER_ITEMS,
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
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ORDER_STATUS, status.name)
            put(COL_ORDER_STEP, step)
            put("step", step)
        }
        val cleanId = orderId.removePrefix("#")
        db.update(TABLE_ORDERS, values, "id = ? OR order_id = ? OR id = ? OR order_id = ?", arrayOf(orderId, orderId, cleanId, cleanId))
    }

    fun cancelOrder(orderId: String) {
        updateOrderStatus(orderId, OrderStatus.CANCELLED, -1)
    }

    // =========================================================================
    // 4. QUẢN LÝ THÔNG BÁO THEO TỪNG USER_ID
    // =========================================================================

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

    fun getNotifications(userId: Long): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        if (userId <= 0) return list
        try {
            val db = readableDatabase
            db.execSQL(createNotifTable())
            val cursor = db.query(
                TABLE_NOTIFICATIONS,
                null,
                "$COL_NOTIF_USER_ID = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "$COL_NOTIF_TIME DESC"
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
                    NotificationType.PROMO
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun markNotificationAsRead(userId: Long, notifId: Long) {
        if (userId <= 0) return
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_READ, 1)
        }
        db.update(
            TABLE_NOTIFICATIONS,
            values,
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_ID = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
    }

    fun markAllNotificationsAsRead(userId: Long, type: NotificationType? = null) {
        if (userId <= 0) return
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIF_IS_READ, 1)
        }
        val where = if (type == null) {
            "$COL_NOTIF_USER_ID = ?"
        } else {
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_TYPE = '${type.name}'"
        }
        db.update(
            TABLE_NOTIFICATIONS,
            values,
            where,
            arrayOf(userId.toString())
        )
    }

    fun cleanNotificationsOlderThan(days: Int, userId: Long) {
        if (userId <= 0) return
        val db = writableDatabase
        val cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
        db.delete(
            TABLE_NOTIFICATIONS,
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_TIME < ?",
            arrayOf(userId.toString(), cutoff.toString())
        )
    }

    fun deleteNotification(userId: Long, notifId: Long) {
        if (userId <= 0) return
        val db = writableDatabase
        db.delete(
            TABLE_NOTIFICATIONS,
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_ID = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
    }

    fun deleteAllNotifications(userId: Long, type: NotificationType? = null) {
        if (userId <= 0) return
        val db = writableDatabase
        val where = if (type == null) {
            "$COL_NOTIF_USER_ID = ?"
        } else {
            "$COL_NOTIF_USER_ID = ? AND $COL_NOTIF_TYPE = '${type.name}'"
        }
        db.delete(TABLE_NOTIFICATIONS, where, arrayOf(userId.toString()))
    }

    fun getUnreadNotificationCount(userId: Long): Int {
        if (userId <= 0) return 0
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_NOTIFICATIONS WHERE $COL_NOTIF_USER_ID = ? AND $COL_NOTIF_IS_READ = 0",
            arrayOf(userId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // =========================================================================
    // 5. QUẢN LÝ SỔ ĐỊA CHỈ GIAO HÀNG (ADDRESSES)
    // =========================================================================

    fun getAddresses(userId: Long): List<SavedAddress> {
        val list = mutableListOf<SavedAddress>()
        if (userId <= 0) return list
        try {
            val db = readableDatabase
            db.execSQL(createAddressesTable())
            val cursor = db.query(
                TABLE_ADDRESSES,
                null,
                "$COL_ADDR_USER_ID = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "$COL_ADDR_IS_DEFAULT DESC, $COL_ADDR_ID DESC"
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ADDR_ID))
                val label = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDR_LABEL))
                val receiver = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDR_RECEIVER))
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDR_PHONE))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDR_ADDRESS))
                val isDefault = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ADDR_IS_DEFAULT)) == 1

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
        val db = writableDatabase
        db.execSQL(createAddressesTable())
        if (address.isDefault) {
            val resetValues = ContentValues().apply { put(COL_ADDR_IS_DEFAULT, 0) }
            db.update(TABLE_ADDRESSES, resetValues, "$COL_ADDR_USER_ID = ?", arrayOf(userId.toString()))
        }
        val cv = ContentValues().apply {
            put(COL_ADDR_USER_ID, userId)
            put(COL_ADDR_LABEL, address.label)
            put(COL_ADDR_RECEIVER, address.receiver)
            put(COL_ADDR_PHONE, address.phone)
            put(COL_ADDR_ADDRESS, address.address)
            put(COL_ADDR_IS_DEFAULT, if (address.isDefault) 1 else 0)
        }
        return db.insert(TABLE_ADDRESSES, null, cv)
    }

    fun updateAddress(userId: Long, address: SavedAddress): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        db.execSQL(createAddressesTable())
        if (address.isDefault) {
            val resetValues = ContentValues().apply { put(COL_ADDR_IS_DEFAULT, 0) }
            db.update(TABLE_ADDRESSES, resetValues, "$COL_ADDR_USER_ID = ?", arrayOf(userId.toString()))
        }
        val cv = ContentValues().apply {
            put(COL_ADDR_LABEL, address.label)
            put(COL_ADDR_RECEIVER, address.receiver)
            put(COL_ADDR_PHONE, address.phone)
            put(COL_ADDR_ADDRESS, address.address)
            put(COL_ADDR_IS_DEFAULT, if (address.isDefault) 1 else 0)
        }
        val rows = db.update(
            TABLE_ADDRESSES,
            cv,
            "$COL_ADDR_USER_ID = ? AND $COL_ADDR_ID = ?",
            arrayOf(userId.toString(), address.id.toString())
        )
        return rows > 0
    }

    fun setDefaultAddress(userId: Long, addressId: Long): Boolean {
        if (userId <= 0) return false
        val db = writableDatabase
        db.execSQL(createAddressesTable())
        db.beginTransaction()
        try {
            val resetValues = ContentValues().apply { put(COL_ADDR_IS_DEFAULT, 0) }
            db.update(TABLE_ADDRESSES, resetValues, "$COL_ADDR_USER_ID = ?", arrayOf(userId.toString()))

            val setValues = ContentValues().apply { put(COL_ADDR_IS_DEFAULT, 1) }
            val rows = db.update(
                TABLE_ADDRESSES,
                setValues,
                "$COL_ADDR_USER_ID = ? AND $COL_ADDR_ID = ?",
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
        val db = writableDatabase
        val rows = db.delete(
            TABLE_ADDRESSES,
            "$COL_ADDR_USER_ID = ? AND $COL_ADDR_ID = ?",
            arrayOf(userId.toString(), addressId.toString())
        )
        return rows > 0
    }

    // =========================================================================
    // 6. QUẢN LÝ ĐÁNH GIÁ MÓN ĂN & QUÁN (REVIEWS)
    // =========================================================================

    fun insertReview(userId: Long, review: Review): Long {
        if (userId <= 0) return -1L
        val db = writableDatabase
        db.execSQL(createReviewsTable())
        val cv = ContentValues().apply {
            put(COL_REVIEW_USER_ID, userId)
            put(COL_REVIEW_ORDER_ID, review.orderId)
            put(COL_REVIEW_RESTAURANT, review.restaurant)
            put(COL_REVIEW_STARS, review.stars)
            put(COL_REVIEW_COMMENT, review.comment)
            put(COL_REVIEW_CREATED_AT, review.createdAt)
        }
        return db.insert(TABLE_REVIEWS, null, cv)
    }

    fun isOrderReviewed(userId: Long, orderId: String): Boolean {
        if (userId <= 0) return false
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REVIEWS,
            arrayOf(COL_REVIEW_ID),
            "$COL_REVIEW_USER_ID = ? AND $COL_REVIEW_ORDER_ID = ?",
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
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REVIEWS,
            null,
            "$COL_REVIEW_USER_ID = ? AND $COL_REVIEW_ORDER_ID = ?",
            arrayOf(userId.toString(), orderId),
            null,
            null,
            null
        )
        var review: Review? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REVIEW_ID))
            val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_RESTAURANT))
            val stars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REVIEW_STARS))
            val comment = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_COMMENT))
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REVIEW_CREATED_AT))
            review = Review(id, orderId, restaurant, stars, comment, createdAt)
        }
        cursor.close()
        return review
    }

    fun avgRatingForRestaurant(restaurant: String): Pair<Double, Int> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT AVG($COL_REVIEW_STARS), COUNT(*) FROM $TABLE_REVIEWS WHERE $COL_REVIEW_RESTAURANT = ?",
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
            val db = readableDatabase
            db.execSQL(createReviewsTable())
            val cursor = db.query(
                TABLE_REVIEWS,
                null,
                "$COL_REVIEW_USER_ID = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "$COL_REVIEW_CREATED_AT DESC"
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REVIEW_ID))
                val orderId = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_ORDER_ID))
                val restaurant = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_RESTAURANT))
                val stars = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REVIEW_STARS))
                val comment = cursor.getString(cursor.getColumnIndexOrThrow(COL_REVIEW_COMMENT))
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_REVIEW_CREATED_AT))

                list.add(
                    Review(
                        id = id,
                        orderId = orderId,
                        restaurant = restaurant,
                        stars = stars,
                        comment = comment,
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

    // =========================================================================
    // 7. QUẢN TRỊ ADMIN - TẤT CẢ ĐƠN HÀNG HỆ THỐNG
    // =========================================================================

    /** Admin: Lấy toàn bộ đơn hàng của tất cả người dùng trong hệ thống */
    fun getAllOrders(): List<Order> {
        val list = mutableListOf<Order>()
        try {
            val db = readableDatabase
            ensureOrdersTable(db)
            val cursor = db.query(
                TABLE_ORDERS,
                null,
                null,
                null,
                null,
                null,
                "$COL_ORDER_CREATED_AT DESC"
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

    // =========================================================================
    // 8. QUẢN TRỊ ADMIN - DANH MỤC MÓN ĂN (CATEGORIES)
    // =========================================================================

    /** Lấy toàn bộ tên danh mục hiện có từ SQLite. */
    fun getAllCategories(): List<String> {
        val list = mutableListOf<String>()
        try {
            val db = readableDatabase
            db.execSQL(createCategoriesTable())
            addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(TABLE_CATEGORIES, arrayOf(COL_CAT_NAME), null, null, null, null, "$COL_CAT_ID ASC")
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            list.addAll(listOf("Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống"))
        }
        return list
    }

    /** Lấy toàn bộ danh mục dạng Category (bao gồm id, tên và ảnh upload) từ SQLite. */
    fun getAllCategoryObjects(): List<Category> {
        val list = mutableListOf<Category>()
        try {
            val db = readableDatabase
            db.execSQL(createCategoriesTable())
            addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(TABLE_CATEGORIES, arrayOf(COL_CAT_ID, COL_CAT_NAME, COL_CAT_IMAGE_URI), null, null, null, null, "$COL_CAT_ID ASC")
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val imgUri = cursor.getString(2)?.takeIf { it.isNotBlank() }
                list.add(Category(id, name, imgUri))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (list.isEmpty()) {
            listOf("Cơm", "Phở", "Bánh mì", "Gà rán", "Mì", "Đồ uống").forEachIndexed { index, name ->
                list.add(Category(index + 1, name, null))
            }
        }
        return list
    }

    /** Lấy đường dẫn ảnh upload của danh mục theo tên. */
    fun getCategoryImageUri(name: String): String? {
        try {
            val db = readableDatabase
            db.execSQL(createCategoriesTable())
            addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(TABLE_CATEGORIES, arrayOf(COL_CAT_IMAGE_URI), "$COL_CAT_NAME = ?", arrayOf(name), null, null, null)
            val uri = if (cursor.moveToFirst()) cursor.getString(0) else null
            cursor.close()
            return uri?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            return null
        }
    }

    /** Thêm danh mục mới vào hệ thống (hỗ trợ kèm ảnh). */
    fun insertCategory(name: String, imageUri: String? = null): Long {
        val clean = name.trim()
        if (clean.isEmpty()) return -1L
        val db = writableDatabase
        db.execSQL(createCategoriesTable())
        addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, clean)
            put(COL_CAT_IMAGE_URI, imageUri ?: "")
        }
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** Sửa tên và ảnh danh mục và cập nhật luôn toàn bộ món đang thuộc danh mục đó. */
    fun updateCategory(oldName: String, newName: String, imageUri: String? = null): Boolean {
        val clean = newName.trim()
        if (clean.isEmpty()) return false
        val db = writableDatabase
        db.execSQL(createCategoriesTable())
        addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
        db.beginTransaction()
        try {
            val cvCat = ContentValues().apply {
                put(COL_CAT_NAME, clean)
                if (imageUri != null) {
                    put(COL_CAT_IMAGE_URI, imageUri)
                }
            }
            val rows = db.update(TABLE_CATEGORIES, cvCat, "$COL_CAT_NAME = ?", arrayOf(oldName))
            if (rows > 0) {
                val cvFood = ContentValues().apply { put(COL_FOOD_CATEGORY, clean) }
                db.update(TABLE_FOODS, cvFood, "$COL_FOOD_CATEGORY = ?", arrayOf(oldName))
                db.setTransactionSuccessful()
                return true
            }
            return false
        } finally {
            db.endTransaction()
        }
    }

    /** Xóa danh mục và chuyển các món thuộc danh mục đó về 'Khác'. */
    fun deleteCategory(name: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val rows = db.delete(TABLE_CATEGORIES, "$COL_CAT_NAME = ?", arrayOf(name))
            if (rows > 0) {
                val cvFood = ContentValues().apply { put(COL_FOOD_CATEGORY, "Khác") }
                db.update(TABLE_FOODS, cvFood, "$COL_FOOD_CATEGORY = ?", arrayOf(name))
                db.setTransactionSuccessful()
                return true
            }
            return false
        } finally {
            db.endTransaction()
        }
    }

    // =========================================================================
    // 9. QUẢN TRỊ ADMIN - MÓN ĂN HỆ THỐNG (FOODS)
    // =========================================================================

    /** Lấy toàn bộ món ăn hệ thống từ SQLite (ưu tiên món mới thêm lên đầu). */
    fun getAllFoods(): List<Food> {
        val list = mutableListOf<Food>()
        try {
            val db = writableDatabase
            ensureFoodsTable(db)
            seedInitialFoods(db)
            val orderBy = "CASE WHEN $COL_FOOD_ID >= 100 THEN 0 ELSE 1 END, $COL_FOOD_ID DESC"
            val cursor = db.query(TABLE_FOODS, null, null, null, null, null, orderBy)
            while (cursor.moveToNext()) {
                val idCol = cursor.getColumnIndex(COL_FOOD_ID)
                val nameCol = cursor.getColumnIndex(COL_FOOD_NAME)
                val restCol = cursor.getColumnIndex(COL_FOOD_RESTAURANT)
                val ratingCol = cursor.getColumnIndex(COL_FOOD_RATING)
                val reviewsCol = cursor.getColumnIndex(COL_FOOD_REVIEWS)
                val priceCol = cursor.getColumnIndex(COL_FOOD_PRICE)
                val catCol = cursor.getColumnIndex(COL_FOOD_CATEGORY)
                val emojiCol = cursor.getColumnIndex(COL_FOOD_EMOJI)
                val bgStartCol = cursor.getColumnIndex(COL_FOOD_BG_START)
                val bgEndCol = cursor.getColumnIndex(COL_FOOD_BG_END)
                val imgCol = cursor.getColumnIndex(COL_FOOD_IMAGE_RES)
                val uriCol = cursor.getColumnIndex(COL_FOOD_IMAGE_URI)
                val sizesCol = cursor.getColumnIndex(COL_FOOD_SIZES)
                val toppingsCol = cursor.getColumnIndex(COL_FOOD_TOPPINGS)
                val availCol = cursor.getColumnIndex(COL_FOOD_IS_AVAILABLE)

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
                val sizesJson = if (sizesCol >= 0) cursor.getString(sizesCol)?.takeIf { it.isNotBlank() } else null
                val toppingsJson = if (toppingsCol >= 0) cursor.getString(toppingsCol)?.takeIf { it.isNotBlank() } else null
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
            val db = writableDatabase
            ensureFoodsTable(db)
            val cursor = db.rawQuery("SELECT MAX($COL_FOOD_ID) FROM $TABLE_FOODS", null)
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
            val db = writableDatabase
            ensureFoodsTable(db)
            val actualImageRes = SampleData.resolveFoodImage(food.id, food.name, food.imageRes, food.category)
            val cv = ContentValues().apply {
                put(COL_FOOD_ID, food.id)
                put(COL_FOOD_NAME, food.name)
                put(COL_FOOD_RESTAURANT, food.restaurant)
                put(COL_FOOD_RATING, food.rating)
                put(COL_FOOD_REVIEWS, food.reviews)
                put(COL_FOOD_PRICE, food.price)
                put(COL_FOOD_CATEGORY, food.category)
                put(COL_FOOD_EMOJI, food.emoji)
                put(COL_FOOD_BG_START, food.bgColorStart)
                put(COL_FOOD_BG_END, food.bgColorEnd)
                put(COL_FOOD_IMAGE_RES, actualImageRes)
                put(COL_FOOD_IMAGE_URI, food.imageUri ?: "")
                put(COL_FOOD_SIZES, food.sizesJson ?: "")
                put(COL_FOOD_TOPPINGS, food.toppingsJson ?: "")
                put(COL_FOOD_IS_AVAILABLE, if (food.isAvailable) 1 else 0)
            }
            db.insertWithOnConflict(TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /** Cập nhật thông tin món ăn (upsert: nếu chưa có thì insert luôn). */
    fun updateFood(food: Food): Boolean {
        return try {
            val db = writableDatabase
            ensureFoodsTable(db)
            val actualImageRes = SampleData.resolveFoodImage(food.id, food.name, food.imageRes, food.category)
            val cv = ContentValues().apply {
                put(COL_FOOD_ID, food.id)
                put(COL_FOOD_NAME, food.name)
                put(COL_FOOD_RESTAURANT, food.restaurant)
                put(COL_FOOD_RATING, food.rating)
                put(COL_FOOD_REVIEWS, food.reviews)
                put(COL_FOOD_PRICE, food.price)
                put(COL_FOOD_CATEGORY, food.category)
                put(COL_FOOD_EMOJI, food.emoji)
                put(COL_FOOD_BG_START, food.bgColorStart)
                put(COL_FOOD_BG_END, food.bgColorEnd)
                put(COL_FOOD_IMAGE_RES, actualImageRes)
                put(COL_FOOD_IMAGE_URI, food.imageUri ?: "")
                put(COL_FOOD_SIZES, food.sizesJson ?: "")
                put(COL_FOOD_TOPPINGS, food.toppingsJson ?: "")
                put(COL_FOOD_IS_AVAILABLE, if (food.isAvailable) 1 else 0)
            }
            val rows = db.update(TABLE_FOODS, cv, "$COL_FOOD_ID = ?", arrayOf(food.id.toString()))
            if (rows <= 0) {
                val inserted = db.insertWithOnConflict(TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
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
            val db = writableDatabase
            ensureFoodsTable(db)
            val cv = ContentValues().apply {
                put(COL_FOOD_IS_AVAILABLE, if (isAvailable) 1 else 0)
            }
            val rows = db.update(TABLE_FOODS, cv, "$COL_FOOD_ID = ?", arrayOf(foodId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Xóa món ăn khỏi hệ thống. */
    fun deleteFood(foodId: Int): Boolean {
        return try {
            val db = writableDatabase
            ensureFoodsTable(db)
            val rows = db.delete(TABLE_FOODS, "$COL_FOOD_ID = ?", arrayOf(foodId.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // =========================================================================
    // 10. QUẢN TRỊ ADMIN - NHÀ HÀNG HỆ THỐNG (RESTAURANTS)
    // =========================================================================

    /** Lấy toàn bộ danh sách quán ăn / nhà hàng từ SQLite. */
    fun getAllRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        try {
            val db = writableDatabase
            ensureRestaurantsTable(db)
            seedInitialRestaurants(db)
            val cursor = db.query(TABLE_RESTAURANTS, null, null, null, null, null, "$COL_REST_ID ASC")
            while (cursor.moveToNext()) {
                val idCol = cursor.getColumnIndex(COL_REST_ID)
                val nameCol = cursor.getColumnIndex(COL_REST_NAME)
                val descCol = cursor.getColumnIndex(COL_REST_DESC)
                val ratingCol = cursor.getColumnIndex(COL_REST_RATING)
                val timeCol = cursor.getColumnIndex(COL_REST_TIME)
                val distCol = cursor.getColumnIndex(COL_REST_DIST)
                val badgeCol = cursor.getColumnIndex(COL_REST_BADGE)
                val bgStartCol = cursor.getColumnIndex(COL_REST_BG_START)
                val bgEndCol = cursor.getColumnIndex(COL_REST_BG_END)
                val imgCol = cursor.getColumnIndex(COL_REST_IMAGE_RES)
                val uriCol = cursor.getColumnIndex(COL_REST_IMAGE_URI)

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
            val db = writableDatabase
            ensureRestaurantsTable(db)
            val cursor = db.rawQuery("SELECT MAX($COL_REST_ID) FROM $TABLE_RESTAURANTS", null)
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
            val db = writableDatabase
            ensureRestaurantsTable(db)
            val cv = ContentValues().apply {
                put(COL_REST_ID, restaurant.id)
                put(COL_REST_NAME, restaurant.name)
                put(COL_REST_DESC, restaurant.desc)
                put(COL_REST_RATING, restaurant.rating)
                put(COL_REST_TIME, restaurant.time)
                put(COL_REST_DIST, restaurant.dist)
                put(COL_REST_BADGE, restaurant.badge)
                put(COL_REST_BG_START, restaurant.bgColorStart)
                put(COL_REST_BG_END, restaurant.bgColorEnd)
                put(COL_REST_IMAGE_RES, restaurant.imageRes)
                put(COL_REST_IMAGE_URI, restaurant.imageUri ?: "")
            }
            db.insertWithOnConflict(TABLE_RESTAURANTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /** Cập nhật thông tin nhà hàng. */
    fun updateRestaurant(restaurant: Restaurant): Boolean {
        return try {
            val db = writableDatabase
            ensureRestaurantsTable(db)
            val cv = ContentValues().apply {
                put(COL_REST_NAME, restaurant.name)
                put(COL_REST_DESC, restaurant.desc)
                put(COL_REST_RATING, restaurant.rating)
                put(COL_REST_TIME, restaurant.time)
                put(COL_REST_DIST, restaurant.dist)
                put(COL_REST_BADGE, restaurant.badge)
                put(COL_REST_BG_START, restaurant.bgColorStart)
                put(COL_REST_BG_END, restaurant.bgColorEnd)
                put(COL_REST_IMAGE_RES, restaurant.imageRes)
                put(COL_REST_IMAGE_URI, restaurant.imageUri ?: "")
            }
            val rows = db.update(TABLE_RESTAURANTS, cv, "$COL_REST_ID = ?", arrayOf(restaurant.id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Xóa nhà hàng theo id. */
    fun deleteRestaurant(id: Int): Boolean {
        return try {
            val db = writableDatabase
            ensureRestaurantsTable(db)
            val rows = db.delete(TABLE_RESTAURANTS, "$COL_REST_ID = ?", arrayOf(id.toString()))
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveOrUpdateCartItem(userId: Long, item: CartItem) {
        addToCart(userId, item)
    }
}
