package com.example.foodhub.data.model

data class Restaurant(
    val id: Int,
    val name: String,
    val desc: String,
    val rating: Double,
    val time: String,
    val dist: String,
    val badge: String,
    val bgColorStart: Int,
    val bgColorEnd: Int,
    val imageRes: Int = 0
)
