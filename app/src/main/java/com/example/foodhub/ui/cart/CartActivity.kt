package com.example.foodhub.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.ui.checkout.CheckoutActivity
import com.example.foodhub.utils.FormatUtils

class CartActivity : AppCompatActivity() {

    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CartManager.ensureLoaded(this)
        setContentView(R.layout.activity_cart)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        adapter = CartAdapter { refresh() }
        findViewById<RecyclerView>(R.id.rvCart).apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = this@CartActivity.adapter
        }

        findViewById<Button>(R.id.btnCheckout).setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        CartManager.ensureLoaded(this)
        adapter.notifyDataSetChanged()
        refresh()
    }

    private fun refresh() {
        val empty = CartManager.isEmpty()
        findViewById<RecyclerView>(R.id.rvCart).visibility = if (empty) View.GONE else View.VISIBLE
        findViewById<LinearLayout>(R.id.llCartEmpty).visibility = if (empty) View.VISIBLE else View.GONE
        findViewById<LinearLayout>(R.id.llCartBottom).visibility = if (empty) View.GONE else View.VISIBLE

        findViewById<TextView>(R.id.tvCartCount).text = "${CartManager.totalQuantity()} món"
        findViewById<TextView>(R.id.tvCartTotal).text = FormatUtils.money(CartManager.totalPrice())
    }
}
