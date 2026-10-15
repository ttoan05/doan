package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.Review

/**
 * Lớp trung gian cho đánh giá / chấm sao. Ủy thác xuống [DatabaseHelper].
 */
object ReviewStore {

    fun hasReviewed(context: Context, orderId: String): Boolean {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return false
        return DatabaseHelper.getInstance(context).hasReview(uid, orderId)
    }

    fun addReview(context: Context, review: Review): Long {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return -1L
        return DatabaseHelper.getInstance(context).insertReview(uid, review)
    }

    fun getForOrder(context: Context, orderId: String): Review? {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return null
        return DatabaseHelper.getInstance(context).getReviewForOrder(uid, orderId)
    }

    fun avgForRestaurant(context: Context, restaurant: String): Pair<Double, Int> {
        val uid = UserProfileManager.currentUserId
        // Trung bình toàn app cho dễ demo (không lọc user); đếm mọi review của quán
        // Nếu muốn riêng từng user thì truyền uid, ở đây giữ đơn giản
        return DatabaseHelper.getInstance(context).avgRatingForRestaurant(restaurant)
    }
}
