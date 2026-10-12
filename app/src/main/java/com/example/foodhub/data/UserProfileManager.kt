package com.example.foodhub.data

import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.data.model.User

/**
 * Lưu thông tin cá nhân của người dùng đang đăng nhập trong bộ nhớ (dùng chung cho toàn app).
 */
object UserProfileManager {

    var currentUserId: Long = 1L
        private set

    var username: String = "foodee_uwfdcvsm"
        private set

    var name: String = "Trường Thanh Toàn"
        private set
    var phone: String = "+84 123 456 789"
        private set
    var email: String = "truong@email.com"
        private set
    var gender: String = ""
        private set
    var birthday: String = ""
        private set
    var job: String = ""
        private set

    val avatarLetter: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "U"

    var onChanged: (() -> Unit)? = null

    fun setUser(user: User) {
        this.currentUserId = user.id
        this.name = user.name
        this.email = user.email
        this.phone = user.phone
        this.gender = user.gender
        this.birthday = user.birthday
        this.job = user.job
        this.username = if (user.email.contains("@")) user.email.substringBefore("@") else user.name.lowercase().replace(" ", "_")
        onChanged?.invoke()
    }

    fun clear() {
        this.currentUserId = -1L
        this.name = ""
        this.email = ""
        this.phone = ""
        this.gender = ""
        this.birthday = ""
        this.job = ""
        this.username = ""
        onChanged?.invoke()
    }

    fun update(
        name: String,
        phone: String,
        email: String,
        gender: String,
        birthday: String,
        job: String
    ) {
        this.name = name
        this.phone = phone
        this.email = email
        this.gender = gender
        this.birthday = birthday
        this.job = job
        NotificationStore.add(
            NotificationType.SYSTEM, "✏️", "Cập nhật thông tin thành công",
            "Thông tin cá nhân của bạn đã được lưu."
        )
        onChanged?.invoke()
    }
}
