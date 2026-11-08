package com.example.foodhub.data.model

data class User(
    val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val gender: String = "",
    val birthday: String = "",
    val job: String = "",
    val role: String = "user", // "user" hoặc "admin"
    val avatarUri: String = "", // Đường dẫn ảnh đại diện cá nhân (file URI hoặc file path)
    val address: String = "", // Địa chỉ khách hàng
    val createdAt: String = "", // Ngày đăng ký / tạo tài khoản
    val status: String = "ACTIVE" // "ACTIVE" (Hoạt động) hoặc "LOCKED" (Đã khóa)
)
