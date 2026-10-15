package com.example.foodhub.ui.restaurant

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.ReviewStore
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.ui.home.FoodAdapter
import com.example.foodhub.ui.product.ProductDetailActivity
import com.example.foodhub.utils.SampleData

class RestaurantDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESTAURANT_ID = "restaurant_id"
    }

    private lateinit var restaurant: Restaurant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FavoriteManager.init(this)
        CartManager.ensureLoaded(this)
        setContentView(R.layout.activity_restaurant_detail)

        val id = intent.getIntExtra(EXTRA_RESTAURANT_ID, -1)
        val found = SampleData.RESTAURANTS.find { it.id == id }
        if (found == null) {
            Toast.makeText(this, "Không tìm thấy thông tin nhà hàng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        restaurant = found

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        bindRestaurant()
        bindMenu()
    }

    private fun bindRestaurant() {
        val ivImage = findViewById<ImageView>(R.id.ivRestaurantImage)
        val tvName = findViewById<TextView>(R.id.tvRestaurantName)
        val tvDesc = findViewById<TextView>(R.id.tvRestaurantDesc)
        val tvRating = findViewById<TextView>(R.id.tvRestaurantRating)
        val tvTime = findViewById<TextView>(R.id.tvRestaurantTime)
        val tvDist = findViewById<TextView>(R.id.tvRestaurantDist)
        val llPromoRow = findViewById<View>(R.id.llPromoRow)
        val tvBadge = findViewById<TextView>(R.id.tvRestaurantBadge)

        if (restaurant.imageRes != 0) {
            ivImage.setImageResource(restaurant.imageRes)
        } else {
            ivImage.setImageDrawable(
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(restaurant.bgColorStart, restaurant.bgColorEnd)
                )
            )
        }

        tvName.text = restaurant.name
        tvDesc.text = restaurant.desc

        // Điểm đánh giá
        val (avgScore, count) = ReviewStore.avgForRestaurant(this, restaurant.name)
        val score = if (count > 0) String.format("%.1f", avgScore) else restaurant.rating.toString()
        tvRating.text = "⭐ $score"

        // Thời gian & khoảng cách
        tvTime.text = "🕒 ${restaurant.time}"
        tvDist.text = "📍 ${restaurant.dist}"

        // Ưu đãi
        if (restaurant.badge.isNotEmpty()) {
            llPromoRow.visibility = View.VISIBLE
            tvBadge.text = restaurant.badge
        } else {
            llPromoRow.visibility = View.GONE
        }
    }

    private fun bindMenu() {
        val rvFoods = findViewById<RecyclerView>(R.id.rvRestaurantFoods)
        val tvHeader = findViewById<TextView>(R.id.tvMenuHeader)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyMenu)

        val foods = SampleData.FOODS.filter { it.restaurant == restaurant.name }
        tvHeader.text = "Thực đơn món ăn (${foods.size} món)"

        if (foods.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvFoods.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvFoods.visibility = View.VISIBLE

            val adapter = FoodAdapter(
                initialFoods = foods,
                onItemClick = { food ->
                    val intent = Intent(this, ProductDetailActivity::class.java).apply {
                        putExtra(ProductDetailActivity.EXTRA_FOOD_ID, food.id)
                    }
                    startActivity(intent)
                },
                onFavoriteChanged = { food, isFav, _ ->
                    Toast.makeText(
                        this,
                        if (isFav) "Đã thêm ${food.name} vào Yêu thích ❤️" else "Đã bỏ ${food.name} khỏi Yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onAddToCart = { food ->
                    CartManager.addFood(food)
                    Toast.makeText(this, "Đã thêm ${food.name} vào giỏ hàng 🛒", Toast.LENGTH_SHORT).show()
                }
            )

            rvFoods.layoutManager = GridLayoutManager(this, 2)
            rvFoods.adapter = adapter
        }
    }
}
