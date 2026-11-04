package com.example.foodhub.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.foodhub.data.dao.*
import com.example.foodhub.utils.SampleData

/**
 * Quản lý cơ sở dữ liệu SQLite cho ứng dụng FoodHub.
 * Các hàm truy vấn được tách module hóa thành các DAO chuyên trách trong package `com.example.foodhub.data.dao`:
 * - UserDao: Tài khoản & xác thực
 * - FoodDao: Món ăn hệ thống & còn hàng
 * - OrderDao: Đơn hàng, chi tiết đơn, trạng thái & lịch sử
 * - CategoryDao: Danh mục món ăn
 * - RestaurantDao: Nhà hàng & quán ăn
 * - CartDao: Giỏ hàng người dùng
 * - NotificationDao: Thông báo hệ thống
 * - AddressReviewDao: Sổ địa chỉ & đánh giá sao
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION),
    UserDao,
    CartDao,
    OrderDao,
    NotificationDao,
    AddressReviewDao,
    CategoryDao,
    FoodDao,
    RestaurantDao {

    override val dbReadable: SQLiteDatabase
        get() = readableDatabase

    override val dbWritable: SQLiteDatabase
        get() = writableDatabase

    companion object {
        private const val DATABASE_NAME = "foodhub_app.db"
        private const val DATABASE_VERSION = 15

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
        const val COL_AVATAR_URI = "avatar_uri"

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
        const val COL_REVIEW_IMAGE_URI = "image_uri"
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
            put(COL_BIRTHDAY, "01/01/2005")
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
            addColumnIfMissing(db, TABLE_USERS, COL_AVATAR_URI, "TEXT DEFAULT ''")
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
            addColumnIfMissing(db, TABLE_REVIEWS, COL_REVIEW_IMAGE_URI, "TEXT DEFAULT ''")
            ensureFoodsTable(db)
            ensureRestaurantsTable(db)
            ensureOrdersTable(db)
            ensureOrderItemsTable(db)
            ensureReviewsTable(db)

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

    fun createUsersTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_USERS (\
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NAME TEXT NOT NULL,
            $COL_EMAIL TEXT UNIQUE NOT NULL,
            $COL_PHONE TEXT NOT NULL,
            $COL_PASSWORD TEXT NOT NULL,
            $COL_GENDER TEXT DEFAULT '',
            $COL_BIRTHDAY TEXT DEFAULT '',
            $COL_JOB TEXT DEFAULT '',
            $COL_ROLE TEXT DEFAULT 'user',
            $COL_AVATAR_URI TEXT DEFAULT ''
        )
    """.trimIndent()

    override fun createCartTable(): String = """
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

    fun createOrdersTable(): String = """
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

    fun createOrderItemsTable(): String = """
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

    override fun createNotifTable(): String = """
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

    override fun createAddressesTable(): String = """
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

    override fun createReviewsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_REVIEWS (
            $COL_REVIEW_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_REVIEW_USER_ID INTEGER NOT NULL,
            $COL_REVIEW_ORDER_ID TEXT NOT NULL,
            $COL_REVIEW_RESTAURANT TEXT NOT NULL,
            $COL_REVIEW_STARS INTEGER NOT NULL,
            $COL_REVIEW_COMMENT TEXT DEFAULT '',
            $COL_REVIEW_IMAGE_URI TEXT DEFAULT '',
            $COL_REVIEW_CREATED_AT INTEGER NOT NULL
        )
    """.trimIndent()

    fun createCategoriesTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
            $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_CAT_NAME TEXT UNIQUE NOT NULL,
            $COL_CAT_IMAGE_URI TEXT DEFAULT ''
        )
    """.trimIndent()

    fun createFoodsTable(): String = """
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

    fun createRestaurantsTable(): String = """
        CREATE TABLE IF NOT EXISTS $TABLE_RESTAURANTS (
            $COL_REST_ID INTEGER PRIMARY KEY,
            $COL_REST_NAME TEXT NOT NULL,
            $COL_REST_DESC TEXT DEFAULT '',
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

    fun seedInitialCategories(db: SQLiteDatabase) {
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

    override fun ensureFoodsTable(db: SQLiteDatabase) {
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

    override fun ensureRestaurantsTable(db: SQLiteDatabase) {
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

    override fun ensureCategoriesTable(db: SQLiteDatabase) {
        db.execSQL(createCategoriesTable())
        addColumnIfMissing(db, TABLE_CATEGORIES, COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
    }

    override fun ensureOrdersTable(db: SQLiteDatabase) {
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

    override fun ensureOrderItemsTable(db: SQLiteDatabase) {
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

    override fun ensureReviewsTable(db: SQLiteDatabase) {
        db.execSQL(createReviewsTable())
        addColumnIfMissing(db, TABLE_REVIEWS, COL_REVIEW_IMAGE_URI, "TEXT DEFAULT ''")
    }

    override fun seedInitialFoods(db: SQLiteDatabase) {
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
                        put(COL_FOOD_SIZES, f.sizesJson ?: "")
                        put(COL_FOOD_TOPPINGS, f.toppingsJson ?: "")
                        put(COL_FOOD_IS_AVAILABLE, if (f.isAvailable) 1 else 0)
                    }
                    db.insertWithOnConflict(TABLE_FOODS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun seedInitialRestaurants(db: SQLiteDatabase) {
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

    override fun addColumnIfMissing(
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
}
