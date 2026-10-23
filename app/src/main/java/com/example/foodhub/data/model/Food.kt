package com.example.foodhub.data.model

data class Food(
    val id: Int,
    val name: String,
    val restaurant: String,
    val rating: Double,
    val reviews: Int,
    val price: String,
    val category: String,
    val emoji: String,
    val bgColorStart: Int,
    val bgColorEnd: Int,
    val imageRes: Int = 0,
    val imageUri: String? = null,
    val sizesJson: String? = null,
    val toppingsJson: String? = null,
    val isAvailable: Boolean = true
)
