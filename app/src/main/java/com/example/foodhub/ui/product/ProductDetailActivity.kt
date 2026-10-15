package com.example.foodhub.ui.product

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
 * Trang chi tiết món ăn: xem thông tin, thả tim, chọn số lượng, thêm ghi chú và thêm vào giỏ.
 * Nhận id món qua EXTRA_FOOD_ID.
 */
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOOD_ID = "extra_food_id"
    }

    private lateinit var food: Food
    private var quantity: Int = 1
    private var isFav: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        FavoriteManager.init(this)

        val foodId = intent.getIntExtra(EXTRA_FOOD_ID, -1)
        val found = SampleData.FOODS.find { it.id == foodId }
        if (found == null) {
            Toast.makeText(this, "Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        food = found
        isFav = FavoriteManager.isFood(food.id)

        bindFood()
        setupListeners()
        setupQuickNoteChips()
        updateQuantityDisplay()
        updateHeartIcon()
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.ivProdHeart).setOnClickListener {
            val newFav = FavoriteManager.toggleFood(food)
            isFav = newFav
            updateHeartIcon()
            val msg = if (newFav) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnProdMinus).setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityDisplay()
            }
        }

        findViewById<TextView>(R.id.btnProdPlus).setOnClickListener {
            if (quantity < 99) {
                quantity++
                updateQuantityDisplay()
            }
        }

        findViewById<Button>(R.id.btnProdAddToCart).setOnClickListener {
            val etNote = findViewById<EditText>(R.id.etProdNote)
            val note = etNote.text.toString().trim()

            CartManager.addFood(food, quantity = quantity, note = note)

            val noteMsg = if (note.isNotBlank()) " (Ghi chú: $note)" else ""
            Toast.makeText(
                this,
                "Đã thêm $quantity x ${food.name} vào giỏ hàng$noteMsg!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun setupQuickNoteChips() {
        val container = findViewById<LinearLayout>(R.id.llQuickNotes) ?: return
        val etNote = findViewById<EditText>(R.id.etProdNote)

        val chips = listOf("Ít ngọt / Ít đường", "Không cay", "Cay nhiều", "Thêm ớt", "Nhiều đá", "Không đá", "Để nước riêng")
        chips.forEach { chipText ->
            val tvChip = TextView(this).apply {
                text = chipText
                textSize = 12f
                setTextColor(Color.parseColor("#4B5563"))
                setBackgroundResource(R.drawable.bg_btn_gray)
                setPadding(24, 12, 24, 12)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 16, 0)
                layoutParams = params
                setOnClickListener {
                    val current = etNote.text.toString().trim()
                    if (current.isEmpty()) {
                        etNote.setText(chipText)
                    } else if (!current.contains(chipText)) {
                        etNote.setText("$current, $chipText")
                    }
                    etNote.setSelection(etNote.text.length)
                }
            }
            container.addView(tvChip)
        }
    }

    private fun bindFood() {
        val ivImage = findViewById<ImageView>(R.id.ivProdImage)
        val tvEmoji = findViewById<TextView>(R.id.tvProdEmoji)

        val actualImgRes = SampleData.resolveFoodImage(food.id, food.name, food.imageRes)
        if (actualImgRes != 0) {
            ivImage.setImageResource(actualImgRes)
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

    private fun updateQuantityDisplay() {
        findViewById<TextView>(R.id.tvProdQty).text = quantity.toString()

        val unitPrice = FormatUtils.parsePrice(food.price)
        val total = unitPrice * quantity
        findViewById<Button>(R.id.btnProdAddToCart).text =
            "Thêm vào giỏ • ${FormatUtils.money(total)}"
    }

    private fun updateHeartIcon() {
        val ivHeart = findViewById<ImageView>(R.id.ivProdHeart)
        if (isFav) {
            ivHeart.setImageResource(R.drawable.ic_heart_filled)
        } else {
            ivHeart.setImageResource(R.drawable.ic_heart_outline)
        }
    }

    private fun descriptionFor(f: Food): String {
        return when (f.id) {
            1 -> "Phở bò truyền thống thơm lừng, nước dùng hầm từ xương bò tươi nguyên chất hơn 10 tiếng kết hợp các loại thảo mộc quế, hồi, thảo quả. Bánh phở tươi mềm mướt, thịt bò tươi ngon kèm hành lá và rau thơm tươi sạch."
            2 -> "Gà rán giòn rụm bên ngoài, mọng nước đậm vị bên trong với lớp vỏ ướp 11 loại gia vị bí truyền. Từng miếng gà vàng ruộm, cay the the kích thích vị giác, thưởng thức nóng giòn tuyệt hảo."
            3 -> "Bánh mì giòn rụm đặc ruột với nhân pate béo ngậy gia truyền, thịt nguội thơm ngon, chả lụa hảo hạng, kèm dưa leo, đồ chua giòn giòn và xốt ớt cay nồng đậm đà hương vị Sài Gòn."
            4 -> "Cơm tấm hạt tấm dẻo thơm, sườn heo ướp mật ong nướng than hoa thơm nức mũi, chả trứng bùi béo và bì heo giòn dai. Ăn kèm mỡ hành thơm ngậy, đồ chua và nước mắm chua ngọt đặc sánh."
            5 -> "Mì Quảng sợi vàng dẻo dai hòa quyện cùng nước nhưn tôm thịt rim đậm đà sánh mịn, trứng cút bùi béo, rắc đậu phộng rang giòn và ăn kèm bánh đa nướng giòn tan cùng rau sống tươi non."
            6 -> "Nước chanh tươi vắt nguyên chất giải nhiệt cực sảng khoái, pha cùng đường mía và đá mát lạnh, thơm nhẹ vỏ chanh tự nhiên, xua tan ngay cơn khát và nạp lại năng lượng ngày dài."
            else -> "Món ăn thơm ngon, chuẩn vị với nguyên liệu tươi sạch được chọn lọc kỹ càng mỗi ngày bởi đầu bếp tay nghề cao. Đảm bảo an toàn vệ sinh thực phẩm và dinh dưỡng cho bạn."
        }
    }
}
