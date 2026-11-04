package com.example.foodhub.data.dao

import android.database.sqlite.SQLiteDatabase

interface BaseDao {
    val dbReadable: SQLiteDatabase
    val dbWritable: SQLiteDatabase

    fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, definition: String) {
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
