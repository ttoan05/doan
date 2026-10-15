package com.example.foodhub.ui.home

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.model.Restaurant

class RestaurantAdapter(
    initialRestaurants: List<Restaurant>,
    // Bấm tim: (nhà hàng, trạng thái yêu thích MỚI, thời điểm thích cũ)
    private val onFavoriteChanged: ((Restaurant, Boolean, Long) -> Unit)? = null,
    private val onViewMenu: (Restaurant) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    // Danh sách đang hiển thị (đã được lọc theo từ khóa tìm kiếm).
    private var restaurants: List<Restaurant> = initialRestaurants

    /** Thay danh sách nhà hàng đang hiển thị bằng danh sách mới (đã được lọc sẵn bên ngoài). */
    fun submit(list: List<Restaurant>) {
        restaurants = list
        notifyDataSetChanged()
    }

    /** Số nhà hàng đang hiển thị sau khi lọc — dùng để hiện thông báo "Không tìm thấy...". */
    fun currentCount(): Int = restaurants.size

    inner class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flBanner: FrameLayout  = itemView.findViewById(R.id.flBanner)
        val ivBanner: ImageView    = itemView.findViewById(R.id.ivRestaurantBanner)
        val tvBadge: TextView      = itemView.findViewById(R.id.tvBadge)
        val tvName: TextView       = itemView.findViewById(R.id.tvRestaurantName)
        val tvDesc: TextView       = itemView.findViewById(R.id.tvRestaurantDesc)
        val tvRating: TextView     = itemView.findViewById(R.id.tvRestaurantRating)
        val tvTime: TextView       = itemView.findViewById(R.id.tvTime)
        val tvDist: TextView       = itemView.findViewById(R.id.tvDist)
        val btnMenu: Button        = itemView.findViewById(R.id.btnViewMenu)
        val ivHeart: ImageView     = itemView.findViewById(R.id.ivRestaurantHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val r = restaurants[position]
        holder.tvBadge.text   = r.badge
        holder.tvName.text    = r.name
        holder.tvDesc.text    = r.desc
        holder.tvRating.text  = r.rating.toString()
        holder.tvTime.text    = "🕒 ${r.time}"
        holder.tvDist.text    = "📍 ${r.dist}"

        if (r.imageRes != 0) {
            holder.ivBanner.setImageResource(r.imageRes)
            holder.ivBanner.visibility = View.VISIBLE
        } else {
            holder.ivBanner.visibility = View.GONE
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(r.bgColorStart, r.bgColorEnd)
            )
            holder.flBanner.background = gradient
        }

        holder.btnMenu.setOnClickListener { onViewMenu(r) }
        holder.itemView.setOnClickListener { onViewMenu(r) }

        // Nút tim (yêu thích)
        holder.ivHeart.setImageResource(
            if (FavoriteManager.isRestaurant(r.id)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
        holder.ivHeart.setOnClickListener {
            val oldTime = FavoriteManager.restaurantAddedAt(r.id)
            val nowFavorite = FavoriteManager.toggleRestaurant(r)
            holder.ivHeart.setImageResource(
                if (nowFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            onFavoriteChanged?.invoke(r, nowFavorite, oldTime)
        }
    }

    override fun getItemCount() = restaurants.size
}
