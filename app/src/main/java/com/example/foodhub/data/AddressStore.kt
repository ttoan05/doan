package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.SavedAddress

/**
 * Lớp trung gian cho địa chỉ giao hàng quen thuộc.
 * Mọi thao tác đều ủy thác xuống [DatabaseHelper] theo user_id hiện tại.
 */
object AddressStore {

    fun getAll(context: Context): List<SavedAddress> {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return emptyList()
        return DatabaseHelper.getInstance(context).getAddresses(uid)
    }

    fun getDefault(context: Context): SavedAddress? =
        getAll(context).find { it.isDefault } ?: getAll(context).firstOrNull()

    fun insert(context: Context, address: SavedAddress): Long {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return -1L
        return DatabaseHelper.getInstance(context).insertAddress(uid, address)
    }

    fun update(context: Context, address: SavedAddress): Boolean {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return false
        return DatabaseHelper.getInstance(context).updateAddress(uid, address)
    }

    fun delete(context: Context, id: Long): Boolean {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return false
        return DatabaseHelper.getInstance(context).deleteAddress(uid, id)
    }

    fun setDefault(context: Context, id: Long): Boolean {
        val uid = UserProfileManager.currentUserId
        if (uid <= 0) return false
        return DatabaseHelper.getInstance(context).setDefaultAddress(uid, id)
    }
}
