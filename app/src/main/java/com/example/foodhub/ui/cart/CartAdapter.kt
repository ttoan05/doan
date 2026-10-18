package com.example.foodhub.ui.cart

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.CartManager
import com.example.foodhub.data.model.CartItem
import com.example.foodhub.utils.FoodSizeHelper
import com.example.foodhub.utils.FormatUtils
import com.example.foodhub.utils.SampleData

class CartAdapter(
    private val onChanged: () -> Unit,
    private val onSelectSize: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivCartImage)
        val tvEmoji: TextView = itemView.findViewById(R.id.tvCartEmoji)
        val tvName: TextView = itemView.findViewById(R.id.tvCartName)
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvCartRestaurant)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvCartQty)
        val btnMinus: TextView = itemView.findViewById(R.id.btnMinus)
        val btnPlus: TextView = itemView.findViewById(R.id.btnPlus)
        val btnDelete: TextView = itemView.findViewById(R.id.btnRemove)
        val tvNote: TextView = itemView.findViewById(R.id.tvCartNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = CartManager.getItems()[position]

        SampleData.loadCartItemImage(holder.ivImage, holder.tvEmoji, item)

        holder.tvName.text = item.name
        holder.tvRestaurant.text = item.restaurant
        holder.tvPrice.text = FormatUtils.money(item.price)
        holder.tvQuantity.text = item.quantity.toString()

        val hasChosenSize = FoodSizeHelper.hasSize(item)
        holder.tvNote.visibility = View.VISIBLE

        val density = holder.itemView.resources.displayMetrics.density
        val padH = (8 * density).toInt()
        val padV = (3 * density).toInt()

        if (!hasChosenSize) {
            holder.tvNote.setBackgroundResource(R.drawable.bg_cart_size_missing)
            holder.tvNote.text = "Chưa chọn size • Chọn ngay ›"
            holder.tvNote.setTextColor(Color.parseColor("#DC2626"))
            holder.tvNote.setTypeface(null, Typeface.BOLD)
        } else {
            holder.tvNote.setBackgroundResource(R.drawable.bg_cart_size_selected)
            holder.tvNote.text = "${item.note} • Đổi ›"
            holder.tvNote.setTextColor(Color.parseColor("#475569"))
            holder.tvNote.setTypeface(null, Typeface.NORMAL)
        }
        holder.tvNote.setPadding(padH, padV, padH, padV)

        holder.tvNote.setOnClickListener {
            onSelectSize(item)
        }

        holder.btnMinus.setOnClickListener {
            CartManager.decrease(item)
            notifyDataSetChanged()
            onChanged()
        }

        holder.btnPlus.setOnClickListener {
            CartManager.increase(item)
            notifyDataSetChanged()
            onChanged()
        }

        holder.btnDelete.setOnClickListener {
            CartManager.remove(item)
            notifyDataSetChanged()
            onChanged()
        }
    }

    override fun getItemCount(): Int = CartManager.getItems().size
}
