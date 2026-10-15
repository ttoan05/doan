package com.example.foodhub.ui.voucher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
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

    private fun renderVouchers() {
        val container = findViewById<LinearLayout>(R.id.llVoucherContainer)
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val vouchers = VoucherStore.VOUCHERS

        vouchers.forEach { voucher ->
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

            val isUsed = VoucherStore.isUsed(this, voucher.code)
            if (isUsed) {
                btnCopy.text = "Đã dùng"
                btnCopy.setBackgroundResource(R.drawable.bg_btn_gray)
                btnCopy.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E5E7EB"))
                btnCopy.setTextColor(Color.parseColor("#9CA3AF"))
                btnCopy.isEnabled = false
            } else {
                btnCopy.text = "Sao chép"
                btnCopy.setBackgroundResource(R.drawable.bg_btn_red)
                btnCopy.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E63946"))
                btnCopy.setTextColor(Color.WHITE)
                btnCopy.isEnabled = true
            }

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
