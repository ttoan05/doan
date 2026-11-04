package com.example.foodhub.ui.product

import android.content.Context
import android.graphics.Color
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.example.foodhub.R
import com.example.foodhub.data.model.Food

/**
 * Helper hỗ trợ mô tả món ăn, topping và ghi chú nhanh cho ProductDetailActivity.
 */
object ProductDetailHelper {

    data class ToppingOption(val name: String, val price: Int)

    val QUICK_NOTES = listOf(
        "Ít ngọt / Ít đường",
        "Không cay",
        "Cay nhiều",
        "Thêm ớt",
        "Nhiều đá",
        "Không đá",
        "Để nước riêng",
        "Không hành lá"
    )

    fun setupQuickNoteChips(context: Context, container: LinearLayout, etNote: EditText) {
        container.removeAllViews()
        QUICK_NOTES.forEach { chipText ->
            val tvChip = TextView(context).apply {
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

    fun descriptionFor(f: Food): String {
        return when (f.id) {
            1 -> "Phở bò truyền thống thơm lừng, nước dùng hầm từ xương bò tươi nguyên chất hơn 10 tiếng kết hợp các loại thảo mộc quế, hồi, thảo quả. Bánh phở tươi mềm mướt, thịt bò tươi ngon kèm hành lá và rau thơm tươi sạch."
            2 -> "Gà rán giòn rụm bên ngoài, mọng nước đậm v ị bên trong với lớp vỏ ướp 11 loại gia vị bí truyền. Từng miếng gà vàng ruộm, cay the the kích thích vị giác, thưởng thức nóng giòn tuyệt hảo."
            3 -> "Bánh mì giòn rụm đặc ruột với nhân pate béo ngậy gia truyền, thịt nguội thơm ngon, chả lụa hảo hạng, kèm dưa leo, đồ chua giòn giòn và xốt ớt cay nồng đậm đà hương vị Sài Gòn."
            4 -> "Cơm tấm hạt tấm dẻo thơm, sườn heo ướp mật ong nướng than hoa thơm nức mũi, chả trứng bùi béo và bì heo giòn dai. Ăn kèm mỡ hành thơm ngậy, đồ chua và nước mắm chua ngọt đặc sánh."
            5 -> "Mì Quảng sợi vàng dẻo dai hòa quyện cùng nước nhưn tôm thịt rim đậm đà sánh mịn, trứng cút bùi béo, rắc đậu phộng rang giòn và ăn kèm bánh đa nướng giòn tan cùng rau sống tươi non."
            6 -> "Nước chanh tươi vắt nguyên chất giải nhiệt cực sảng khoái, pha cùng đường mía và đá mát lạnh, thơm nhẹ vỏ chanh tự nhiên, xua tan ngay cơn khát và nạp lại năng lượng ngày dài."
            else -> "Món ăn thơm ngon, chuẩn vị với nguyên liệu tươi sạch được chọn lọc kỹ càng mỗi ngày bởi đầu bếp tay nghề cao. Đảm bảo an toàn vệ sinh thực phẩm và dinh dưỡng cho bạn."
        }
    }
}
