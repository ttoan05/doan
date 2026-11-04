package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.User

// =========================================================================
// 1. QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG (USERS)
// =========================================================================

interface UserDao : BaseDao {

    fun registerUser(user: User): Long {
        if (isEmailExists(user.email)) return -1L
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NAME, user.name)
            put(DatabaseHelper.COL_EMAIL, user.email.trim().lowercase())
            put(DatabaseHelper.COL_PHONE, user.phone.trim())
            put(DatabaseHelper.COL_PASSWORD, user.password)
            put(DatabaseHelper.COL_GENDER, user.gender)
            put(DatabaseHelper.COL_BIRTHDAY, user.birthday)
            put(DatabaseHelper.COL_JOB, user.job)
            put(DatabaseHelper.COL_ROLE, if (user.role.isNotEmpty()) user.role else "user")
        }
        return db.insert(DatabaseHelper.TABLE_USERS, null, values)
    }

    fun isEmailExists(email: String): Boolean {
        val db = dbReadable
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            arrayOf(DatabaseHelper.COL_ID),
            "LOWER(${DatabaseHelper.COL_EMAIL}) = ?",
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
        val db = dbReadable
        val cleanAccount = account.trim().lowercase()
        val query = """
            SELECT * FROM ${DatabaseHelper.TABLE_USERS} 
            WHERE (LOWER(${DatabaseHelper.COL_EMAIL}) = ? OR ${DatabaseHelper.COL_PHONE} = ?) 
              AND ${DatabaseHelper.COL_PASSWORD} = ? 
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
        val db = dbReadable
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "LOWER(${DatabaseHelper.COL_EMAIL}) = ?",
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

    fun getUserById(id: Long): User? {
        val db = dbReadable
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_USERS,
            null,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString()),
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
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NAME, user.name)
            put(DatabaseHelper.COL_EMAIL, user.email.trim().lowercase())
            put(DatabaseHelper.COL_PHONE, user.phone.trim())
            put(DatabaseHelper.COL_PASSWORD, user.password)
            put(DatabaseHelper.COL_GENDER, user.gender)
            put(DatabaseHelper.COL_BIRTHDAY, user.birthday)
            put(DatabaseHelper.COL_JOB, user.job)
            if (user.role.isNotEmpty()) {
                put(DatabaseHelper.COL_ROLE, user.role)
            }
        }
        return db.update(DatabaseHelper.TABLE_USERS, values, "${DatabaseHelper.COL_ID} = ?", arrayOf(user.id.toString()))
    }

    fun updateUserProfile(user: User): Boolean {
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NAME, user.name)
            put(DatabaseHelper.COL_EMAIL, user.email.trim().lowercase())
            put(DatabaseHelper.COL_PHONE, user.phone.trim())
            put(DatabaseHelper.COL_GENDER, user.gender)
            put(DatabaseHelper.COL_BIRTHDAY, user.birthday)
            put(DatabaseHelper.COL_JOB, user.job)
            put(DatabaseHelper.COL_AVATAR_URI, user.avatarUri)
        }
        val rows = db.update(DatabaseHelper.TABLE_USERS, values, "${DatabaseHelper.COL_ID} = ?", arrayOf(user.id.toString()))
        return rows > 0
    }

    fun updateUserAvatar(userId: Long, avatarUri: String): Boolean {
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_AVATAR_URI, avatarUri)
        }
        val rows = db.update(DatabaseHelper.TABLE_USERS, values, "${DatabaseHelper.COL_ID} = ?", arrayOf(userId.toString()))
        return rows > 0
    }

    fun updateUserPassword(userId: Long, newPassword: String): Boolean {
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PASSWORD, newPassword)
        }
        val rows = db.update(DatabaseHelper.TABLE_USERS, values, "${DatabaseHelper.COL_ID} = ?", arrayOf(userId.toString()))
        return rows > 0
    }

    fun cursorToUser(cursor: Cursor): User {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME))
        val email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EMAIL))
        val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHONE))
        val password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PASSWORD))
        val gender = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GENDER)) ?: ""
        val birthday = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BIRTHDAY)) ?: ""
        val job = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_JOB)) ?: ""
        val roleColIndex = cursor.getColumnIndex(DatabaseHelper.COL_ROLE)
        val role = if (roleColIndex >= 0) cursor.getString(roleColIndex) ?: "user" else "user"
        val avatarColIndex = cursor.getColumnIndex(DatabaseHelper.COL_AVATAR_URI)
        val avatarUri = if (avatarColIndex >= 0) cursor.getString(avatarColIndex) ?: "" else ""
        return User(id, name, email, phone, password, gender, birthday, job, role, avatarUri)
    }
}
