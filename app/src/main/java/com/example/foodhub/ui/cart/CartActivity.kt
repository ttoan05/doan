package com.example.foodhub.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.ui.checkout.CheckoutActivity
import com.example.foodhub.utils.FoodSizeHelper
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

class CartActivity : AppCompatActivity() {

    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CartManager.ensureLoaded(this)
        setContentView(R.layout.activity_cart)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        adapter = CartAdapter(
            onChanged = { refresh() },
            onSelectSize = { item -> showSelectSizeDialog(item, fromCheckoutClick = false) }
        )
        findViewById<RecyclerView>(R.id.rvCart).apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = this@CartActivity.adapter
        }

        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            val unselectedItem = CartManager.getItems().find { !FoodSizeHelper.hasSize(it) }
            if (unselectedItem != null) {
                showSelectSizeDialog(unselectedItem, fromCheckoutClick = true)
                return@setOnClickListener
            }
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        CartManager.ensureLoaded(this)
        refresh()
    }

    private fun showSelectSizeDialog(item: CartItem, fromCheckoutClick: Boolean) {
        val sizes = FoodSizeHelper.getSizesForCartItem(item)
        if (sizes.isEmpty()) {
            Toast.makeText(this, "Món này hiện chỉ có 1 kích cỡ chuẩn", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_cart_select_size, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivFood = dialogView.findViewById<ImageView>(R.id.ivDialogFoodImage)
        val tvEmoji = dialogView.findViewById<TextView>(R.id.tvDialogFoodEmoji)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogFoodName)
        val tvRestaurant = dialogView.findViewById<TextView>(R.id.tvDialogFoodRestaurant)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogClose)
        val llContainer = dialogView.findViewById<LinearLayout>(R.id.llDialogSizesContainer)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmSize)

        SampleData.loadCartItemImage(ivFood, tvEmoji, item)
        tvName.text = item.name
        tvRestaurant.text = item.restaurant

        // Find initial selected index if any matches current note
        var selectedIndex = 0
        for (i in sizes.indices) {
            val s = sizes[i]
            if (item.note.isNotBlank() && (item.note.contains(s.name, ignoreCase = true) || s.name.contains(item.note, ignoreCase = true))) {
                selectedIndex = i
                break
            }
        }

        val rowViews = mutableListOf<View>()

        fun updateConfirmButton() {
            val selected = sizes[selectedIndex]
            btnConfirm.text = "Xác nhận: ${selected.name} • ${FormatUtils.money(selected.fullPrice)}"
        }

        fun renderSelection() {
            for (i in rowViews.indices) {
                val view = rowViews[i]
                val ivRadio = view.findViewById<ImageView>(R.id.ivSizeRadio)
                val isSelected = (i == selectedIndex)
                view.setBackgroundResource(
                    if (isSelected) R.drawable.bg_payment_card_selected else R.drawable.bg_payment_card_normal
                )
                ivRadio.setImageResource(
                    if (isSelected) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                )
            }
            updateConfirmButton()
        }

        llContainer.removeAllViews()
        rowViews.clear()

        sizes.forEachIndexed { index, size ->
            val itemView = layoutInflater.inflate(R.layout.item_dialog_size_option, llContainer, false)
            val tvSizeName = itemView.findViewById<TextView>(R.id.tvSizeName)
            val tvSizePrice = itemView.findViewById<TextView>(R.id.tvSizePrice)
            val tvDelta = itemView.findViewById<TextView>(R.id.tvSizeDeltaBadge)

            tvSizeName.text = size.name
            tvSizePrice.text = FormatUtils.money(size.fullPrice)

            if (size.priceDelta > 0) {
                tvDelta.text = "+${FormatUtils.money(size.priceDelta)}"
                tvDelta.visibility = View.VISIBLE
            } else {
                tvDelta.visibility = View.GONE
            }

            itemView.setOnClickListener {
                selectedIndex = index
                renderSelection()
            }

            llContainer.addView(itemView)
            rowViews.add(itemView)
        }

        renderSelection()

        btnClose.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val selected = sizes[selectedIndex]
            CartManager.updateItemSizeAndPrice(item, selected.name, selected.fullPrice)
            refresh()
            Toast.makeText(
                this,
                "Đã chọn ${selected.name} (${FormatUtils.money(selected.fullPrice)})",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()

            if (fromCheckoutClick) {
                val nextUnselected = CartManager.getItems().find { !FoodSizeHelper.hasSize(it) }
                if (nextUnselected != null) {
                    showSelectSizeDialog(nextUnselected, fromCheckoutClick = true)
                } else {
                    Toast.makeText(this, "Đã chọn đủ kích cỡ! Bấm Đặt hàng để thanh toán.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
        val dm = resources.displayMetrics
        dialog.window?.setLayout((dm.widthPixels * 0.92).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun refresh() {
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
        val empty = CartManager.isEmpty()
        findViewById<RecyclerView>(R.id.rvCart).visibility = if (empty) View.GONE else View.VISIBLE
        findViewById<LinearLayout>(R.id.llCartEmpty).visibility = if (empty) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.llCartBottom).visibility = if (empty) View.GONE else View.VISIBLE

        findViewById<TextView>(R.id.tvCartCount).text = "${CartManager.totalQuantity()} món"
        findViewById<TextView>(R.id.tvCartTotal).text = FormatUtils.money(CartManager.totalPrice())
    }
}
