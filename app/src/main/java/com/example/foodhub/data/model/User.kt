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
    val role: String = "user" // "user" hoặc "admin"
)
