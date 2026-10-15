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
import com.example.foodhub.utils.SampleData

class CartAdapter(
    private val onChanged: () -> Unit
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

        val actualImgRes = SampleData.resolveFoodImage(item.foodId, item.name, item.imageRes)
        if (actualImgRes != 0) {
            holder.ivImage.setImageResource(actualImgRes)
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
        holder.tvQuantity.text = item.quantity.toString()

        if (item.note.isNotBlank()) {
            holder.tvNote.visibility = View.VISIBLE
            holder.tvNote.text = "📝 ${item.note}"
        } else {
            holder.tvNote.visibility = View.GONE
        }

        holder.btnMinus.setOnClickListener {
            CartManager.decrease(item)
            onChanged()
        }

        holder.btnPlus.setOnClickListener {
            CartManager.increase(item)
            onChanged()
        }

        holder.btnDelete.setOnClickListener {
            CartManager.remove(item)
            onChanged()
        }
    }

    override fun getItemCount(): Int = CartManager.getItems().size
}
