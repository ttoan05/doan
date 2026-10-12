package com.example.foodhub.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

class FoodAdapter(
    initialFoods: List<Food>,
    // Bấm vào cả thẻ món -> mở chi tiết (không bắt buộc)
    private val onItemClick: ((Food) -> Unit)? = null,
    // Bấm tim: (món, trạng thái yêu thích MỚI, thời điểm thích cũ) - dùng để hiện thông báo/hoàn tác
    private val onFavoriteChanged: ((Food, Boolean, Long) -> Unit)? = null,
    private val onAddToCart: (Food) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // Danh sách đang hiển thị (đã được lọc theo danh mục và/hoặc từ khóa tìm kiếm).
    private var foods: List<Food> = initialFoods

    /** Thay danh sách món đang hiển thị bằng danh sách mới (đã được lọc sẵn bên ngoài). */
    fun submit(list: List<Food>) {
        foods = list
        notifyDataSetChanged()
    }

    /** Số món đang hiển thị sau khi lọc — dùng để hiện thông báo "Không tìm thấy...". */
    fun currentCount(): Int = foods.size

    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val ivImage: ImageView =
            itemView.findViewById(R.id.ivFoodImage)

        val tvEmoji: TextView =
            itemView.findViewById(R.id.tvFoodEmoji)

        val tvName: TextView =
            itemView.findViewById(R.id.tvFoodName)

        val tvRestaurant: TextView =
            itemView.findViewById(R.id.tvFoodRestaurant)

        val tvRating: TextView =
            itemView.findViewById(R.id.tvRating)

        val tvReviews: TextView =
            itemView.findViewById(R.id.tvReviews)

        val tvPrice: TextView =
            itemView.findViewById(R.id.tvPrice)

        val btnAdd: Button =
            itemView.findViewById(R.id.btnAdd)

        val ivHeart: ImageView =
            itemView.findViewById(R.id.ivFoodHeart)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FoodViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_food,
                parent,
                false
            )

        return FoodViewHolder(view)
    }


    override fun onBindViewHolder(
        holder: FoodViewHolder,
        position: Int
    ) {

        val food = foods[position]


        // ==========================================
        // THÔNG TIN MÓN ĂN
        // ==========================================

        holder.tvName.text = food.name

        holder.tvRestaurant.text = food.restaurant

        holder.tvRating.text = food.rating.toString()

        holder.tvReviews.text = "(${food.reviews})"

        holder.tvPrice.text = food.price


        // ==========================================
        // HÌNH ẢNH MÓN ĂN
        // ==========================================

        if (food.imageRes != 0) {
            holder.ivImage.setImageResource(food.imageRes)
            holder.ivImage.visibility = View.VISIBLE
            holder.tvEmoji.visibility = View.GONE
        } else {
            holder.ivImage.visibility = View.GONE
            holder.tvEmoji.visibility = View.VISIBLE
            holder.tvEmoji.text = food.emoji

            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    food.bgColorStart,
                    food.bgColorEnd
                )
            )
            holder.tvEmoji.background = gradient
        }


        // ==========================================
        // NÚT + THÊM
        // ==========================================

        // Ép lại nội dung chữ
        holder.btnAdd.text = "+ Thêm"

        // Màu chữ đỏ
        holder.btnAdd.setTextColor(
            Color.parseColor("#E5394F")
        )

        // Cỡ chữ
        holder.btnAdd.textSize = 13f

        // Không viết hoa tự động
        holder.btnAdd.isAllCaps = false

        // Không để Android tự tạo hiệu ứng nền
        holder.btnAdd.backgroundTintList = null

        // Padding
        holder.btnAdd.setPadding(
            0,
            0,
            0,
            0
        )


        // ==========================================
        // TẠO NỀN NÚT
        // ==========================================

        val addButtonBackground = GradientDrawable()

        // Nền trắng hơi hồng
        addButtonBackground.setColor(
            Color.parseColor("#FFF8F8")
        )

        // Viền hồng nhạt
        addButtonBackground.setStroke(
            2,
            Color.parseColor("#F3C7C7")
        )

        // Bo góc
        addButtonBackground.cornerRadius = 10f


        holder.btnAdd.background =
            addButtonBackground


        // ==========================================
        // SỰ KIỆN THÊM MÓN
        // ==========================================

        holder.btnAdd.setOnClickListener {

            onAddToCart(food)
        }


        // ==========================================
        // NÚT TIM (YÊU THÍCH) + BẤM VÀO THẺ MÓN
        // ==========================================

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

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(food)
        }
    }


    override fun getItemCount(): Int {

        return foods.size
    }
}