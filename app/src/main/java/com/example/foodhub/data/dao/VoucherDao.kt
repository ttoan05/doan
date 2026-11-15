
package com.example.foodhub.data.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.foodhub.data.model.Voucher
import com.example.foodhub.data.model.VoucherType

/**
 * DAO quản lý voucher trong SQLite.
 * Các chức năng dành cho Admin và người dùng.
 */
interface VoucherDao : BaseDao {

    fun ensureVouchersTable(db: SQLiteDatabase)
    fun seedInitialVouchers(db: SQLiteDatabase)

    companion object {
        const val TABLE_VOUCHERS = "vouchers"
        const val TABLE_VOUCHER_USAGE = "voucher_usage"
    }

    /**
     * Đọc một voucher từ Cursor.
     */
    private fun cursorToVoucher(cursor: android.database.Cursor): Voucher {
        return Voucher(
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            code = cursor.getString(cursor.getColumnIndexOrThrow("code")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            desc = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            type = try {
                VoucherType.valueOf(
                    cursor.getString(cursor.getColumnIndexOrThrow("type"))
                )
            } catch (_: Exception) {
                VoucherType.PERCENT
            },
            value = cursor.getInt(cursor.getColumnIndexOrThrow("value")),
            maxDiscount = cursor.getInt(cursor.getColumnIndexOrThrow("max_discount")),
            minOrder = cursor.getInt(cursor.getColumnIndexOrThrow("min_order")),
            emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")) ?: "🎟️",
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity")),
            remainingQuantity = cursor.getInt(
                cursor.getColumnIndexOrThrow("remaining_quantity")
            ),
            startAt = cursor.getLong(cursor.getColumnIndexOrThrow("start_at")),
            endAt = cursor.getLong(cursor.getColumnIndexOrThrow("end_at")),
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1
        )
    }

    /**
     * Chuyển Voucher thành ContentValues để lưu SQLite.
     */
    private fun voucherToContentValues(voucher: Voucher): ContentValues {
        return ContentValues().apply {
            put("code", voucher.code.trim().uppercase())
            put("title", voucher.title.trim())
            put("description", voucher.desc)
            put("type", voucher.type.name)
            put("value", voucher.value)
            put("max_discount", voucher.maxDiscount)
            put("min_order", voucher.minOrder)
            put("emoji", voucher.emoji)
            put("quantity", voucher.quantity)
            put("remaining_quantity", voucher.remainingQuantity)
            put("start_at", voucher.startAt)
            put("end_at", voucher.endAt)
            put("is_active", if (voucher.isActive) 1 else 0)
        }
    }

    /**
     * Thêm voucher mới.
     * Trả về ID nếu thành công, -1 nếu thất bại hoặc trùng mã.
     */
    fun insertVoucher(voucher: Voucher): Long {
        val db = dbWritable

        return try {
            val values = voucherToContentValues(voucher)

            db.insertWithOnConflict(
                TABLE_VOUCHERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /**
     * Lấy toàn bộ voucher.
     */
    fun getAllVouchers(): List<Voucher> {
        val result = mutableListOf<Voucher>()

        try {
            val db = dbWritable
            ensureVouchersTable(db)
            seedInitialVouchers(db)

            val cursor = db.query(
                TABLE_VOUCHERS,
                null,
                null,
                null,
                null,
                null,
                "id DESC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    result.add(cursorToVoucher(it))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    /**
     * Tìm voucher theo mã.
     * Không phân biệt chữ hoa và chữ thường.
     */
    fun getVoucherByCode(code: String): Voucher? {
        if (code.isBlank()) return null

        return try {
            val db = dbWritable
            ensureVouchersTable(db)
            seedInitialVouchers(db)

            val cursor = db.query(
                TABLE_VOUCHERS,
                null,
                "UPPER(code) = ?",
                arrayOf(code.trim().uppercase()),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) cursorToVoucher(it) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Tìm voucher theo ID.
     */
    fun getVoucherById(id: Int): Voucher? {
        return try {
            val cursor = dbReadable.query(
                TABLE_VOUCHERS,
                null,
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )

            cursor.use {
                if (it.moveToFirst()) cursorToVoucher(it) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cập nhật thông tin voucher.
     */
    fun updateVoucher(voucher: Voucher): Boolean {
        if (voucher.id <= 0) return false

        return try {
            val rows = dbWritable.update(
                TABLE_VOUCHERS,
                voucherToContentValues(voucher),
                "id = ?",
                arrayOf(voucher.id.toString())
            )

            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Bật hoặc tắt voucher.
     */
    fun updateVoucherStatus(id: Int, isActive: Boolean): Boolean {
        return try {
            val values = ContentValues().apply {
                put("is_active", if (isActive) 1 else 0)
            }

            dbWritable.update(
                TABLE_VOUCHERS,
                values,
                "id = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Xóa voucher theo ID.
     */
    fun deleteVoucher(id: Int): Boolean {
        return try {
            val db = dbWritable

            db.beginTransaction()
            try {
                db.delete(
                    TABLE_VOUCHER_USAGE,
                    "voucher_id = ?",
                    arrayOf(id.toString())
                )

                val rows = db.delete(
                    TABLE_VOUCHERS,
                    "id = ?",
                    arrayOf(id.toString())
                )

                db.setTransactionSuccessful()
                rows > 0
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Kiểm tra người dùng đã sử dụng voucher chưa.
     */
    fun hasUserUsedVoucher(voucherId: Int, userId: Int): Boolean {
        if (voucherId <= 0 || userId <= 0) return false

        return try {
            val db = dbWritable
            ensureVouchersTable(db)

            val cursor = db.query(
                TABLE_VOUCHER_USAGE,
                arrayOf("id"),
                "voucher_id = ? AND user_id = ?",
                arrayOf(voucherId.toString(), userId.toString()),
                null,
                null,
                null
            )

            cursor.use {
                it.moveToFirst()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Đếm số tài khoản đã sử dụng voucher này.
     */
    fun getVoucherUsageCount(voucherId: Int): Int {
        return try {
            val db = dbWritable
            ensureVouchersTable(db)

            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_VOUCHER_USAGE WHERE voucher_id = ?",
                arrayOf(voucherId.toString())
            )
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Ghi nhận người dùng đã sử dụng voucher (theo từng tài khoản độc lập).
     */
    fun markVoucherAsUsed(voucherId: Int, userId: Int): Boolean {
        if (voucherId <= 0 || userId <= 0) return false

        val db = dbWritable
        ensureVouchersTable(db)

        return try {
            val usageValues = ContentValues().apply {
                put("voucher_id", voucherId)
                put("user_id", userId)
                put("used_at", System.currentTimeMillis())
            }

            val usageId = db.insertWithOnConflict(
                TABLE_VOUCHER_USAGE,
                null,
                usageValues,
                SQLiteDatabase.CONFLICT_IGNORE
            )

            if (usageId != -1L) {
                // Cập nhật số lượng còn lại cho mục đích thống kê hiển thị
                try {
                    db.execSQL(
                        "UPDATE $TABLE_VOUCHERS SET remaining_quantity = CASE WHEN remaining_quantity > 0 THEN remaining_quantity - 1 ELSE 0 END WHERE id = ?",
                        arrayOf(voucherId)
                    )
                } catch (_: Throwable) {}
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
