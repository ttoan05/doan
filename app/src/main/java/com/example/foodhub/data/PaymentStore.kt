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
    const val METHOD_CARD = "CARD"

    private const val PREFS_NAME = "foodhub_payment_prefs"
    private const val KEY_PREFIX = "default_payment_method_"

    fun getDefaultMethod(context: Context): String {
        val uid = UserProfileManager.currentUserId
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("$KEY_PREFIX$uid", METHOD_COD) ?: METHOD_COD
    }

    fun setDefaultMethod(context: Context, methodCode: String) {
        val uid = UserProfileManager.currentUserId
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_PREFIX$uid", methodCode).apply()
    }
}
