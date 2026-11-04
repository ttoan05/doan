package com.example.foodhub.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    /** 45000 -> "45.000đ" */
    fun money(value: Int): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
        return DecimalFormat("#,##0", symbols).format(value) + "đ"
    }

    /** "45.000đ" -> 45000 */
    fun parsePrice(text: String): Int {
        return text.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    /** millis -> "28/09/2026 14:30" */
    fun dateTime(millis: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date(millis))
    }

    /** millis -> "Vừa xong", "5 phút trước", "2 giờ trước", "Hôm qua", "3 ngày trước", "20/09/2026" */
    fun timeAgo(millis: Long): String {
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < minute -> "Vừa xong"
            diff < hour -> "${diff / minute} phút trước"
            diff < day -> "${diff / hour} giờ trước"
            diff < 2 * day -> "Hôm qua"
            diff < 7 * day -> "${diff / day} ngày trước"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(millis))
        }
    }

    /** millis có nằm trong ngày hôm nay không */
    fun isToday(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val other = Calendar.getInstance().apply { timeInMillis = millis }
        return now.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Chuyển đổi chuỗi tiếng Việt có dấu thành không dấu, viết thường và chuẩn hóa để tìm kiếm linh hoạt.
     * Ví dụ: "Cơm sườn" -> "com suon", "Phở Bò" -> "pho bo", "Trà sữa" -> "tra sua"
     */
    fun removeDiacritics(str: String?): String {
        if (str.isNullOrBlank()) return ""
        val normalized = java.text.Normalizer.normalize(str.trim().lowercase(), java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
    }

    /**
     * Kiểm tra xem chuỗi văn bản [target] có chứa hoặc khớp với từ khóa tìm kiếm [query] hay không.
     * Hỗ trợ tìm kiếm ngay từ 1-2 chữ cái đầu (VD: gõ "c" hay "co" đều khớp với "Cơm").
     * Hỗ trợ tìm cả khi gõ có dấu ("cơm") lẫn gõ không dấu ("com").
     */
    fun matchesSearch(target: String?, query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        if (target.isNullOrBlank()) return false

        val cleanQuery = query.trim().lowercase()
        val cleanTarget = target.trim().lowercase()

        // 1. So khớp trực tiếp (có dấu)
        if (cleanTarget.contains(cleanQuery)) return true

        // 2. So khớp không dấu linh hoạt (ví dụ: gõ "c", "co", "com" đều khớp "Cơm")
        val noToneTarget = removeDiacritics(cleanTarget)
        val noToneQuery = removeDiacritics(cleanQuery)

        return noToneTarget.contains(noToneQuery)
    }
}
