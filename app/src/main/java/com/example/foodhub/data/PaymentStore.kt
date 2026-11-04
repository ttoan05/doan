package com.example.foodhub.data

import android.content.Context

/**
 * Quản lý phương thức thanh toán mặc định của từng người dùng.
 * Được lưu trữ an toàn trong SharedPreferences theo user_id.
 */
object PaymentStore {

    const val METHOD_COD = "COD"
    const val METHOD_MOMO = "MOMO"
    const val METHOD_BANK = "BANK"

    private const val PREFS_NAME = "foodhub_payment_prefs"
    private const val KEY_PREFIX = "default_payment_method_"

    fun getDefaultMethod(context: Context): String {
        val uid = UserProfileManager.currentUserId
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val method = prefs.getString("$KEY_PREFIX$uid", METHOD_COD) ?: METHOD_COD
        // Tự động chuyển về COD nếu trước đó đã từng chọn CARD
        return if (method == "CARD") METHOD_COD else method
    }

    fun setDefaultMethod(context: Context, methodCode: String) {
        val uid = UserProfileManager.currentUserId
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val safeMethod = if (methodCode == "CARD") METHOD_COD else methodCode
        prefs.edit().putString("$KEY_PREFIX$uid", safeMethod).apply()
    }
}
