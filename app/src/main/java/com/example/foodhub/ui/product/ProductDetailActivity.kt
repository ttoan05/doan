package com.example.foodhub.ui.product

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

/**
 * Trang chi tiết món ăn: xem thông tin, thả tim, chọn số lượng và thêm vào giỏ.
 * Nhận id món qua EXTRA_FOOD_ID.
 */
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOOD_ID = "food_id"
        private const val MAX_QUANTITY = 20
    }

    private lateinit var food: Food
    private var quantity = 1

    private lateinit var ivHeart: ImageView
    private lateinit var tvQty: TextView
    private lateinit var btnAddToCart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Phòng khi app bị hệ thống khởi động lại thẳng vào trang này
        FavoriteManager.init(this)
        CartManager.ensureLoaded(this)

        setContentView(R.layout.activity_product_detail)

        val id = intent.getIntExtra(EXTRA_FOOD_ID, -1)
        val found = SampleData.FOODS.find { it.id == id }
        if (found == null) {
            Toast.makeText(this, "Không tìm thấy món ăn", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        food = found

        ivHeart = findViewById(R.id.ivProdHeart)
        tvQty = findViewById(R.id.tvProdQty)
        btnAddToCart = findViewById(R.id.btnProdAddToCart)

        bindFood()
        updateHeart()
        updateQuantity()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        ivHeart.setOnClickListener {
            val nowFavorite = FavoriteManager.toggleFood(food)
            updateHeart()
            Toast.makeText(
                this,
                if (nowFavorite) "Đã thêm vào Yêu thích ❤️" else "Đã bỏ khỏi Yêu thích",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<TextView>(R.id.btnProdMinus).setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantity()
            }
        }
        findViewById<TextView>(R.id.btnProdPlus).setOnClickListener {
            if (quantity < MAX_QUANTITY) {
                quantity++
                updateQuantity()
            } else {
                Toast.makeText(this, "Tối đa $MAX_QUANTITY phần mỗi lần", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddToCart.setOnClickListener {
            repeat(quantity) { CartManager.addFood(food) }
            Toast.makeText(
                this,
                "Đã thêm $quantity ${food.name} vào giỏ hàng 🛒",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeart()
    }

    private fun bindFood() {
        val ivImage = findViewById<ImageView>(R.id.ivProdImage)
        val tvEmoji = findViewById<TextView>(R.id.tvProdEmoji)

        if (food.imageRes != 0) {
            ivImage.setImageResource(food.imageRes)
            ivImage.visibility = View.VISIBLE
            tvEmoji.visibility = View.GONE
        } else {
            ivImage.visibility = View.GONE
            tvEmoji.visibility = View.VISIBLE
            tvEmoji.text = food.emoji
            tvEmoji.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(food.bgColorStart, food.bgColorEnd)
            )
        }

        findViewById<TextView>(R.id.tvProdName).text = food.name
        findViewById<TextView>(R.id.tvProdRestaurant).text = food.restaurant
        findViewById<TextView>(R.id.tvProdRating).text = "${food.rating} (${food.reviews} đánh giá)"
        findViewById<TextView>(R.id.tvProdPrice).text = food.price
        findViewById<TextView>(R.id.tvProdDesc).text = descriptionFor(food)
        findViewById<TextView>(R.id.tvProdInfo).text =
            "🕒 Giao trong 20-30 phút   •   🚚 Freeship đơn từ 50.000đ"
    }

    private fun updateHeart() {
        ivHeart.setImageResource(
            if (FavoriteManager.isFood(food.id)) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )
    }

    private fun updateQuantity() {
        tvQty.text = quantity.toString()
        val total = FormatUtils.parsePrice(food.price) * quantity
        btnAddToCart.text = "Thêm vào giỏ • ${FormatUtils.money(total)}"
    }

    // Dữ liệu mẫu chưa có mô tả nên viết tạm theo danh mục
    private fun descriptionFor(food: Food): String = when (food.category) {
        "Phở" -> "Nước dùng hầm từ xương bò nhiều giờ, ngọt thanh, bánh phở mềm dai cùng thịt bò tươi thái mỏng. Ăn kèm rau thơm, giá, chanh và ớt."
        "Gà rán" -> "Gà tẩm ướp đậm vị, chiên vàng giòn bên ngoài, thịt mềm ngọt bên trong, cay nhẹ vừa miệng. Ăn kèm sốt chấm."
        "Bánh mì" -> "Vỏ bánh giòn rụm, nhân đầy đặn với thịt nguội, pate, dưa leo, đồ chua và rau mùi. Ăn nóng là ngon nhất."
        "Cơm" -> "Cơm tấm dẻo thơm ăn cùng sườn nướng than thơm lừng, thêm mỡ hành, đồ chua và chén nước mắm pha đậm đà."
        "Mì" -> "Sợi mì vàng dai ăn cùng tôm, thịt, đậu phộng rang và bánh tráng nướng, nước nhân sền sệt đậm đà kiểu Quảng."
        "Đồ uống" -> "Nước chanh tươi vắt mát lạnh, chua ngọt vừa phải, giải khát rất hợp cho ngày nóng."
        else -> "Món ngon được chế biến từ nguyên liệu tươi mỗi ngày, giao nhanh tận nơi."
    }
}
