package com.example.foodhub.ui.voucher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.VoucherStore

class VoucherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voucher)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        renderVouchers()
    }

    override fun onResume() {
        super.onResume()
        renderVouchers()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun renderVouchers() {
        val container = findViewById<LinearLayout>(R.id.llVoucherContainer)
        val tvSubtitle = findViewById<TextView>(R.id.tvVoucherSubtitle)
        container.removeAllViews()

        // Chỉ lấy những voucher còn khả dụng (chưa sử dụng), voucher đã dùng sẽ biến mất hoàn toàn
        val availableVouchers = VoucherStore.getAvailableVouchers(this)

        if (availableVouchers.isEmpty()) {
            tvSubtitle?.visibility = View.GONE

            val emptyContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(80), dpToPx(24), dpToPx(40))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val tvIcon = TextView(this).apply {
                text = "🎟️"
                textSize = 50f
                gravity = Gravity.CENTER
            }

            val tvEmptyTitle = TextView(this).apply {
                text = "Bạn chưa có voucher khả dụng nào"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1F2937"))
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(14), 0, dpToPx(6))
            }

            val tvEmptyDesc = TextView(this).apply {
                text = "Các mã voucher đã dùng sẽ được tự động dọn sạch.\nHãy đón chờ các chương trình khuyến mãi tiếp theo của FoodHub nhé!"
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                gravity = Gravity.CENTER
                setLineSpacing(dpToPx(4).toFloat(), 1.1f)
            }

            emptyContainer.addView(tvIcon)
            emptyContainer.addView(tvEmptyTitle)
            emptyContainer.addView(tvEmptyDesc)
            container.addView(emptyContainer)
            return
        }

        tvSubtitle?.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        availableVouchers.forEach { voucher ->
            val itemView = inflater.inflate(R.layout.item_voucher_card, container, false)

            val tvIcon = itemView.findViewById<TextView>(R.id.tvIcon)
            val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            val tvDesc = itemView.findViewById<TextView>(R.id.tvDesc)
            val tvCode = itemView.findViewById<TextView>(R.id.tvCode)
            val btnCopy = itemView.findViewById<Button>(R.id.btnCopy)

            tvIcon.text = voucher.emoji
            tvTitle.text = voucher.title
            tvDesc.text = voucher.desc
            tvCode.text = voucher.code

            btnCopy.text = "Sao chép"
            btnCopy.setBackgroundResource(R.drawable.bg_btn_red)
            btnCopy.setTextColor(Color.WHITE)
            btnCopy.isEnabled = true

            val copyAction = View.OnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Voucher Code", voucher.code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Đã sao chép mã: ${voucher.code}", Toast.LENGTH_SHORT).show()
            }

            btnCopy.setOnClickListener(copyAction)
            tvCode.setOnClickListener(copyAction)

            container.addView(itemView)
        }
    }
}
