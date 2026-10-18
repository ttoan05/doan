package com.example.foodhub.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.SampleData

class FoodAdapter(
    initialFoods: List<Food>,
    private val onItemClick: ((Food) -> Unit)? = null,
    private val onFavoriteChanged: ((Food, Boolean, Long) -> Unit)? = null,
    private val onAddToCart: (Food) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private var foods: List<Food> = initialFoods

    fun submit(list: List<Food>) {
        foods = list
        notifyDataSetChanged()
    }

    fun currentCount(): Int = foods.size

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivFoodImage)
        val tvEmoji: TextView = itemView.findViewById(R.id.tvFoodEmoji)
        val ivHeart: ImageView = itemView.findViewById(R.id.ivFoodHeart)
        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvFoodRestaurant)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvReviews: TextView = itemView.findViewById(R.id.tvReviews)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnAdd: Button = itemView.findViewById(R.id.btnAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun getItemCount(): Int = foods.size

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]

        holder.tvName.text = food.name
        holder.tvRestaurant.text = food.restaurant
        holder.tvRating.text = food.rating.toString()
        holder.tvReviews.text = "(${food.reviews})"
        holder.tvPrice.text = food.price

        SampleData.loadFoodImage(holder.ivImage, holder.tvEmoji, food)

        holder.btnAdd.setOnClickListener {
            onAddToCart(food)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(food)
        }

        holder.ivHeart.setImageResource(
            if (FavoriteManager.isFood(food.id)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        holder.ivHeart.setOnClickListener {
            val oldTime = FavoriteManager.foodAddedAt(food.id)
            val nowFavorite = FavoriteManager.toggleFood(food)
            holder.ivHeart.setImageResource(
                if (nowFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            onFavoriteChanged?.invoke(food, nowFavorite, oldTime)
        }
    }
}
