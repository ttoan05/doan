package com.example.foodhub.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Lưu ảnh từ Uri của hệ thống vào bộ nhớ trong của ứng dụng.
     * Đọc stream duy nhất 1 lần để tránh lỗi SecurityException/Stream Closed khi mở lần 2,
     * đồng thời không nạp toàn bộ ảnh lớn vào RAM giúp tránh tràn bộ nhớ (OutOfMemoryError).
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri, prefix: String = "img"): String? {
        return try {
            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (file.exists() && file.length() > 0) {
                file.absolutePath
            } else {
                null
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Giải mã ảnh từ file với kích thước thu nhỏ phù hợp (inSampleSize),
     * chống tràn bộ nhớ (OutOfMemoryError) khi người dùng chọn ảnh độ phân giải cao.
     */
    fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            var inSampleSize = 1
            val height = options.outHeight
            val width = options.outWidth

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Tiết kiệm 50% RAM
            }

            BitmapFactory.decodeFile(filePath, decodeOptions)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    /**
     * Nạp và cắt ảnh đại diện tròn gọn nhẹ (mặc định 200x200 px),
     * giải phóng ngay bitmap thô để tránh rò rỉ và tràn bộ nhớ RAM.
     */
    fun getCircularAvatar(filePath: String, targetSize: Int = 200): Bitmap? {
        val sampled = decodeSampledBitmap(filePath, targetSize, targetSize) ?: return null
        return try {
            val circular = getCircularBitmap(sampled)
            if (circular != sampled) {
                sampled.recycle()
            }
            circular
        } catch (t: Throwable) {
            t.printStackTrace()
            sampled
        }
    }

    /**
     * Cắt ảnh thành hình tròn hoàn hảo (Circular Avatar) để hiển thị đồng bộ trên mọi phiên bản Android
     */
    fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        return try {
            val size = minOf(bitmap.width, bitmap.height)
            if (size <= 0) return bitmap

            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }

            val left = (bitmap.width - size) / 2
            val top = (bitmap.height - size) / 2
            val srcRect = Rect(left, top, left + size, top + size)
            val dstRect = Rect(0, 0, size, size)
            val rectF = RectF(dstRect)

            canvas.drawARGB(0, 0, 0, 0)
            canvas.drawOval(rectF, paint)

            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

            output
        } catch (t: Throwable) {
            t.printStackTrace()
            bitmap
        }
    }
}
