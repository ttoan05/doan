package com.example.foodhub.ui.cart

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.utils.FormatUtils

class CartAdapter(
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivCartImage)
        val tvEmoji: TextView = itemView.findViewById(R.id.tvCartEmoji)
        val tvName: TextView = itemView.findViewById(R.id.tvCartName)
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvCartRestaurant)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartPrice)
        val tvQty: TextView = itemView.findViewById(R.id.tvCartQty)
        val btnMinus: TextView = itemView.findViewById(R.id.btnMinus)
        val btnPlus: TextView = itemView.findViewById(R.id.btnPlus)
        val btnRemove: TextView = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = CartManager.getItems()[position]

        if (item.imageRes != 0) {
            holder.ivImage.setImageResource(item.imageRes)
            holder.ivImage.visibility = View.VISIBLE
            holder.tvEmoji.visibility = View.GONE
        } else {
            holder.ivImage.visibility = View.GONE
            holder.tvEmoji.visibility = View.VISIBLE
            holder.tvEmoji.text = item.emoji
            holder.tvEmoji.background = GradientDrawable().apply {
                setColor(Color.parseColor("#FDECEC"))
                cornerRadius = 12f * holder.itemView.resources.displayMetrics.density
            }
        }
        holder.tvName.text = item.name
        holder.tvRestaurant.text = item.restaurant
        holder.tvPrice.text = FormatUtils.money(item.price)
        holder.tvQty.text = item.quantity.toString()

        holder.btnPlus.setOnClickListener {
            CartManager.increase(item)
            notifyDataSetChanged()
            onChanged()
        }
        holder.btnMinus.setOnClickListener {
            CartManager.decrease(item)
            notifyDataSetChanged()
            onChanged()
        }
        holder.btnRemove.setOnClickListener {
            CartManager.remove(item)
            notifyDataSetChanged()
            onChanged()
        }
    }

    override fun getItemCount(): Int = CartManager.getItems().size
}
