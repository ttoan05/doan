package com.example.foodhub.data.dao

import android.content.ContentValues
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.NotificationType

// =========================================================================
// 4. QUẢN LÝ THÔNG BÁO THEO TỪNG USER_ID
// =========================================================================

interface NotificationDao : BaseDao {
    fun createNotifTable(): String

    fun insertNotification(userId: Long, notif: AppNotification): Long {
        if (userId <= 0) return -1L
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTIF_USER_ID, userId)
            put(DatabaseHelper.COL_NOTIF_TYPE, notif.type.name)
            put(DatabaseHelper.COL_NOTIF_EMOJI, notif.emoji)
            put(DatabaseHelper.COL_NOTIF_TITLE, notif.title)
            put(DatabaseHelper.COL_NOTIF_MESSAGE, notif.message)
            put(DatabaseHelper.COL_NOTIF_TIME, notif.time)
            put(DatabaseHelper.COL_NOTIF_IS_READ, if (notif.isRead) 1 else 0)
            put(DatabaseHelper.COL_NOTIF_ORDER_ID, notif.orderId)
            put(DatabaseHelper.COL_NOTIF_PROMO_CODE, notif.promoCode)
        }
        return db.insert(DatabaseHelper.TABLE_NOTIFICATIONS, null, values)
    }

    fun getNotifications(userId: Long): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        if (userId <= 0) return list
        try {
            val db = dbReadable
            db.execSQL(createNotifTable())
            val cursor = db.query(
                DatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                "${DatabaseHelper.COL_NOTIF_USER_ID} = ?",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseHelper.COL_NOTIF_TIME} DESC"
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_ID))
                val typeStr = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_TYPE))
                val emoji = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_EMOJI))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_TITLE))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_MESSAGE))
                val time = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_TIME))
                val isRead = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_IS_READ)) == 1
                val orderId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_ORDER_ID))
                val promoCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_PROMO_CODE))

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
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTIF_IS_READ, 1)
        }
        db.update(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_ID} = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
    }

    fun markAllNotificationsAsRead(userId: Long, type: NotificationType? = null) {
        if (userId <= 0) return
        val db = dbWritable
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTIF_IS_READ, 1)
        }
        val where = if (type == null) {
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ?"
        } else {
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_TYPE} = '${type.name}'"
        }
        db.update(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            where,
            arrayOf(userId.toString())
        )
    }

    fun cleanNotificationsOlderThan(days: Int, userId: Long) {
        if (userId <= 0) return
        val db = dbWritable
        val cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
        db.delete(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_TIME} < ?",
            arrayOf(userId.toString(), cutoff.toString())
        )
    }

    fun deleteNotification(userId: Long, notifId: Long) {
        if (userId <= 0) return
        val db = dbWritable
        db.delete(
            DatabaseHelper.TABLE_NOTIFICATIONS,
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_ID} = ?",
            arrayOf(userId.toString(), notifId.toString())
        )
    }

    fun deleteAllNotifications(userId: Long, type: NotificationType? = null) {
        if (userId <= 0) return
        val db = dbWritable
        val where = if (type == null) {
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ?"
        } else {
            "${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_TYPE} = '${type.name}'"
        }
        db.delete(DatabaseHelper.TABLE_NOTIFICATIONS, where, arrayOf(userId.toString()))
    }

    fun getUnreadNotificationCount(userId: Long): Int {
        if (userId <= 0) return 0
        val db = dbReadable
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_NOTIFICATIONS} WHERE ${DatabaseHelper.COL_NOTIF_USER_ID} = ? AND ${DatabaseHelper.COL_NOTIF_IS_READ} = 0",
            arrayOf(userId.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }
}
