package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.Category

// =========================================================================
// 8. QUẢN TRỊ ADMIN - DANH MỤC MÓN ĂN (CATEGORIES)
// =========================================================================

interface CategoryDao : BaseDao {
    fun ensureCategoriesTable(db: SQLiteDatabase)

    /** Lấy toàn bộ tên danh mục hiện có từ SQLite. */
    fun getAllCategories(): List<String> {
        val list = mutableListOf<String>()
        try {
            val db = dbReadable
            ensureCategoriesTable(db)
            addColumnIfMissing(db, DatabaseHelper.TABLE_CATEGORIES, DatabaseHelper.COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, arrayOf(DatabaseHelper.COL_CAT_NAME), null, null, null, null, "${DatabaseHelper.COL_CAT_ID} ASC")
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
            val db = dbReadable
            ensureCategoriesTable(db)
            addColumnIfMissing(db, DatabaseHelper.TABLE_CATEGORIES, DatabaseHelper.COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, arrayOf(DatabaseHelper.COL_CAT_ID, DatabaseHelper.COL_CAT_NAME, DatabaseHelper.COL_CAT_IMAGE_URI), null, null, null, null, "${DatabaseHelper.COL_CAT_ID} ASC")
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
            val db = dbReadable
            ensureCategoriesTable(db)
            addColumnIfMissing(db, DatabaseHelper.TABLE_CATEGORIES, DatabaseHelper.COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
            val cursor = db.query(DatabaseHelper.TABLE_CATEGORIES, arrayOf(DatabaseHelper.COL_CAT_IMAGE_URI), "${DatabaseHelper.COL_CAT_NAME} = ?", arrayOf(name), null, null, null)
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
        val db = dbWritable
        ensureCategoriesTable(db)
        addColumnIfMissing(db, DatabaseHelper.TABLE_CATEGORIES, DatabaseHelper.COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_CAT_NAME, clean)
            put(DatabaseHelper.COL_CAT_IMAGE_URI, imageUri ?: "")
        }
        return db.insertWithOnConflict(DatabaseHelper.TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** Sửa tên và ảnh danh mục và cập nhật luôn toàn bộ món đang thuộc danh mục đó. */
    fun updateCategory(oldName: String, newName: String, imageUri: String? = null): Boolean {
        val clean = newName.trim()
        if (clean.isEmpty()) return false
        val db = dbWritable
        ensureCategoriesTable(db)
        addColumnIfMissing(db, DatabaseHelper.TABLE_CATEGORIES, DatabaseHelper.COL_CAT_IMAGE_URI, "TEXT DEFAULT ''")
        db.beginTransaction()
        try {
            val cvCat = ContentValues().apply {
                put(DatabaseHelper.COL_CAT_NAME, clean)
                if (imageUri != null) {
                    put(DatabaseHelper.COL_CAT_IMAGE_URI, imageUri)
                }
            }
            val rows = db.update(DatabaseHelper.TABLE_CATEGORIES, cvCat, "${DatabaseHelper.COL_CAT_NAME} = ?", arrayOf(oldName))
            if (rows > 0) {
                val cvFood = ContentValues().apply { put(DatabaseHelper.COL_FOOD_CATEGORY, clean) }
                db.update(DatabaseHelper.TABLE_FOODS, cvFood, "${DatabaseHelper.COL_FOOD_CATEGORY} = ?", arrayOf(oldName))
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
        val db = dbWritable
        db.beginTransaction()
        try {
            val rows = db.delete(DatabaseHelper.TABLE_CATEGORIES, "${DatabaseHelper.COL_CAT_NAME} = ?", arrayOf(name))
            if (rows > 0) {
                val cvFood = ContentValues().apply { put(DatabaseHelper.COL_FOOD_CATEGORY, "Khác") }
                db.update(DatabaseHelper.TABLE_FOODS, cvFood, "${DatabaseHelper.COL_FOOD_CATEGORY} = ?", arrayOf(name))
                db.setTransactionSuccessful()
                return true
            }
            return false
        } finally {
            db.endTransaction()
        }
    }
}
