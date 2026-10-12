package com.example.foodhub.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.model.Order
import com.example.foodhub.utils.FormatUtils

class OrderAdapter(
    private val onDetail: (Order) -> Unit,
    private val onReorder: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private var orders: List<Order> = emptyList()

    // Card chỉ hiện tối đa 2 món, còn lại hiển thị "+ N món khác"
    private val maxLines = 2

    fun submit(list: List<Order>) {
        orders = list
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvOrderRestaurant)
        val tvStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val tvCode: TextView = itemView.findViewById(R.id.tvOrderCode)
        val tvDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val llItems: LinearLayout = itemView.findViewById(R.id.llOrderItems)
        val tvMore: TextView = itemView.findViewById(R.id.tvOrderMore)
        val tvTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        val btnReorder: Button = itemView.findViewById(R.id.btnReorder)
        val btnDetail: Button = itemView.findViewById(R.id.btnOrderDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvRestaurant.text = order.restaurant
        holder.tvCode.text = "Mã đơn: ${order.code}"
        holder.tvDate.text = "Đặt lúc: ${FormatUtils.dateTime(order.createdAt)}"
        OrderUi.styleStatus(holder.tvStatus, order.status)

        holder.llItems.removeAllViews()
        order.items.take(maxLines).forEach { OrderUi.addItemLine(holder.llItems, it) }

        val remaining = order.items.size - maxLines
        if (remaining > 0) {
            holder.tvMore.visibility = View.VISIBLE
            holder.tvMore.text = "+ $remaining món khác"
        } else {
            holder.tvMore.visibility = View.GONE
        }

        holder.tvTotal.text = FormatUtils.money(order.total)

        holder.btnReorder.visibility = if (order.canReorder()) View.VISIBLE else View.GONE
        holder.btnReorder.setOnClickListener { onReorder(order) }
        holder.btnDetail.setOnClickListener { onDetail(order) }
        holder.itemView.setOnClickListener { onDetail(order) }
    }

    override fun getItemCount(): Int = orders.size
}
