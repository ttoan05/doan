package com.example.foodhub.ui.product

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.FavoriteManager
import com.example.foodhub.data.model.Food
import com.example.foodhub.utils.FoodSizeHelper
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

/**
 * Trang chi tiết món ăn: xem thông tin, thả tim, chọn size, topping, số lượng, thêm ghi chú và thêm vào giỏ.
 * Có cảnh báo khi đặt món khác quán để bảo vệ giỏ hàng.
 */
class ProductDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOOD_ID = "extra_food_id"
    }

    data class ToppingOption(val name: String, val price: Int)

    private lateinit var food: Food
    private var quantity: Int = 1
    private var isFav: Boolean = false

    private var sizePriceExtra: Int = 0
    private var selectedSizeName: String = ""

    private val availableToppings = mutableListOf<ToppingOption>()
    private val selectedToppings = mutableSetOf<ToppingOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        FavoriteManager.init(this)

        val foodId = intent.getIntExtra(EXTRA_FOOD_ID, -1)
        val found = DatabaseHelper.getInstance(this).getAllFoods().find { it.id == foodId }
            ?: SampleData.FOODS.find { it.id == foodId }

        if (found == null) {
            Toast.makeText(this, "Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        food = found
        isFav = FavoriteManager.isFood(food.id)

        bindFood()
        setupSizeSection()
        setupToppingsSection()
        setupListeners()
        setupQuickNoteChips()
        updateQuantityAndPriceDisplay()
        updateHeartIcon()
    }

    private fun setupSizeSection() {
        val container = findViewById<LinearLayout>(R.id.rgSizeOptions) ?: return
        container.removeAllViews()

        val sizeOptions = FoodSizeHelper.getSizesForFood(food)
        if (sizeOptions.isEmpty()) {
            findViewById<View>(R.id.llSizeSection)?.visibility = View.GONE
            selectedSizeName = ""
            sizePriceExtra = 0
            return
        }

        findViewById<View>(R.id.llSizeSection)?.visibility = View.VISIBLE

        val radioButtons = mutableListOf<RadioButton>()
        val dp1 = resources.displayMetrics.density.toInt()

        fun selectSizeIndex(index: Int) {
            radioButtons.forEachIndexed { i, rb ->
                rb.isChecked = (i == index)
            }
            val chosen = sizeOptions[index]
            selectedSizeName = chosen.name
            sizePriceExtra = chosen.priceDelta
            updateQuantityAndPriceDisplay()
        }

        sizeOptions.forEachIndexed { index, option ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    44 * dp1
                )
                isClickable = true
                isFocusable = true
            }

            val rb = RadioButton(this).apply {
                text = option.name
                textSize = 14f
                setTextColor(Color.parseColor("#111111"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isChecked = (index == 0)
                isClickable = false
            }
            radioButtons.add(rb)

            val tvPrice = TextView(this).apply {
                if (option.priceDelta > 0) {
                    text = "+${FormatUtils.money(option.priceDelta)}"
                    setTextColor(Color.parseColor("#E63946"))
                    setTypeface(null, Typeface.BOLD)
                } else {
                    text = "+0đ"
                    setTextColor(Color.parseColor("#888888"))
                }
                textSize = 13f
            }

            row.setOnClickListener { selectSizeIndex(index) }

            row.addView(rb)
            row.addView(tvPrice)
            container.addView(row)

            if (index < sizeOptions.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        maxOf(1, dp1 / 2)
                    )
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
                container.addView(divider)
            }
        }

        selectSizeIndex(0)
    }

    private fun setupToppingsSection() {
        val container = findViewById<LinearLayout>(R.id.llToppingsContainer) ?: return
        container.removeAllViews()
        availableToppings.clear()
        selectedToppings.clear()

        val list = FoodSizeHelper.getToppingsForFood(food).map {
            ToppingOption(it.name, it.price)
        }

        if (list.isEmpty()) {
            findViewById<View>(R.id.llToppingsSection)?.visibility = View.GONE
            return
        }
        findViewById<View>(R.id.llToppingsSection)?.visibility = View.VISIBLE

        availableToppings.addAll(list)

        val dp1 = (resources.displayMetrics.density).toInt()
        val dp8 = 8 * dp1

        list.forEachIndexed { index, option ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, dp8, 0, dp8)
                isClickable = true
                isFocusable = true
            }

            val cb = CheckBox(this).apply {
                text = option.name
                textSize = 14f
                setTextColor(Color.parseColor("#111111"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvPrice = TextView(this).apply {
                text = "+${FormatUtils.money(option.price)}"
                textSize = 13f
                setTextColor(Color.parseColor("#E63946"))
                setTypeface(null, Typeface.BOLD)
                setPadding(dp8, 0, 0, 0)
            }

            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedToppings.add(option)
                } else {
                    selectedToppings.remove(option)
                }
                updateQuantityAndPriceDisplay()
            }

            row.setOnClickListener {
                cb.isChecked = !cb.isChecked
            }

            row.addView(cb)
            row.addView(tvPrice)
            container.addView(row)

            if (index < list.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        maxOf(1, dp1 / 2)
                    )
                    setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
                container.addView(divider)
            }
        }
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
                updateQuantityAndPriceDisplay()
            }
        }

        findViewById<TextView>(R.id.btnProdPlus).setOnClickListener {
            if (quantity < 99) {
                quantity++
                updateQuantityAndPriceDisplay()
            }
        }

        findViewById<Button>(R.id.btnProdAddToCart).setOnClickListener {
            if (!food.isAvailable) {
                Toast.makeText(this, "Món ăn này hiện đang tạm hết, vui lòng chọn món khác!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val etNote = findViewById<EditText>(R.id.etProdNote)
            val userNote = etNote.text.toString().trim()

            val basePrice = FormatUtils.parsePrice(food.price)
            val currentUnitPrice = basePrice + sizePriceExtra + selectedToppings.sumOf { it.price }

            val noteParts = mutableListOf<String>()
            val cleanSize = selectedSizeName.replace(Regex("\\(.*\\)"), "").trim()
            if (cleanSize.isNotEmpty()) {
                noteParts.add(cleanSize)
            }
            if (selectedToppings.isNotEmpty()) {
                val toppingStr = selectedToppings.joinToString(", ") { "+${it.name}" }
                noteParts.add(toppingStr)
            }
            if (userNote.isNotEmpty()) {
                noteParts.add(userNote)
            }
            val finalNote = noteParts.joinToString(" • ")

            CartManager.addFood(food, quantity = quantity, note = finalNote, customUnitPrice = currentUnitPrice)
            val noteMsg = if (finalNote.isNotBlank()) " ($finalNote)" else ""
            Toast.makeText(
                this,
                "Đã thêm $quantity x ${food.name} vào giỏ hàng$noteMsg! 🛒",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun setupQuickNoteChips() {
        val container = findViewById<LinearLayout>(R.id.llQuickNotes) ?: return
        val etNote = findViewById<EditText>(R.id.etProdNote)

        val chips = listOf("Ít ngọt / Ít đường", "Không cay", "Cay nhiều", "Thêm ớt", "Nhiều đá", "Không đá", "Để nước riêng", "Không hành lá")
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

        SampleData.loadFoodImage(ivImage, tvEmoji, food)

        findViewById<TextView>(R.id.tvProdName).text = food.name
        findViewById<TextView>(R.id.tvProdRestaurant).text = food.restaurant
        findViewById<TextView>(R.id.tvProdRating).text = "${food.rating} (${food.reviews} đánh giá)"
        findViewById<TextView>(R.id.tvProdPrice).text = food.price
        findViewById<TextView>(R.id.tvProdDesc).text = descriptionFor(food)
        findViewById<TextView>(R.id.tvProdInfo).text =
            "🕒 Giao trong 20-30 phút   •   🚚 Freeship đơn từ 50.000đ"
    }

    private fun updateQuantityAndPriceDisplay() {
        findViewById<TextView>(R.id.tvProdQty).text = quantity.toString()

        val btnAdd = findViewById<Button>(R.id.btnProdAddToCart)
        if (!food.isAvailable) {
            btnAdd.isEnabled = false
            btnAdd.text = "Món này hiện đang tạm hết"
            btnAdd.setBackgroundResource(R.drawable.bg_btn_disabled_soft)
            btnAdd.setTextColor(Color.parseColor("#94A3B8"))
            findViewById<ImageView>(R.id.ivProdImage).alpha = 0.6f
            findViewById<TextView>(R.id.btnProdMinus).isEnabled = false
            findViewById<TextView>(R.id.btnProdPlus).isEnabled = false
            return
        }

        val basePrice = FormatUtils.parsePrice(food.price)
        val unitPrice = basePrice + sizePriceExtra + selectedToppings.sumOf { it.price }
        val total = unitPrice * quantity
        btnAdd.isEnabled = true
        btnAdd.setBackgroundResource(R.drawable.bg_btn_red)
        btnAdd.setTextColor(Color.WHITE)
        btnAdd.text = "Thêm vào giỏ • ${FormatUtils.money(total)}"
        findViewById<ImageView>(R.id.ivProdImage).alpha = 1.0f
        findViewById<TextView>(R.id.btnProdMinus).isEnabled = true
        findViewById<TextView>(R.id.btnProdPlus).isEnabled = true
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
