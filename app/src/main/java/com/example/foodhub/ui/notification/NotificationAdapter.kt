package com.example.foodhub.ui.notification

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.utils.FormatUtils

/**
 * Danh sách thông báo, chia nhóm "Hôm nay" / "Trước đó".
 * Mỗi nhóm có 1 dòng tiêu đề (header) nên adapter có 2 loại view.
 */
class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit,
    private val onLongClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        class Header(val title: String) : Row()
        class Item(val notification: AppNotification) : Row()
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private var rows: List<Row> = emptyList()

    fun submit(list: List<AppNotification>) {
        val result = mutableListOf<Row>()
        val today = list.filter { FormatUtils.isToday(it.time) }
        val earlier = list.filter { !FormatUtils.isToday(it.time) }

        if (today.isNotEmpty()) {
            result.add(Row.Header("Hôm nay"))
            today.forEach { result.add(Row.Item(it)) }
        }
        if (earlier.isNotEmpty()) {
            result.add(Row.Header("Trước đó"))
            earlier.forEach { result.add(Row.Item(it)) }
        }
        rows = result
        notifyDataSetChanged()
    }

    /** Vị trí không hợp lệ cũng coi như header để không cho vuốt xóa. */
    fun isHeader(position: Int): Boolean =
        position !in rows.indices || rows[position] is Row.Header

    fun notificationAt(position: Int): AppNotification? =
        (rows.getOrNull(position) as? Row.Item)?.notification

    // ─── ViewHolder ────────────────────────────────────
    inner class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView as TextView
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcon: TextView = itemView.findViewById(R.id.tvNotifIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)
        val vDot: View = itemView.findViewById(R.id.vNotifDot)
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_notification_header, parent, false))
        } else {
            ItemHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderHolder).tvTitle.text = row.title
            is Row.Item -> bindItem(holder as ItemHolder, row.notification)
        }
    }

    private fun bindItem(holder: ItemHolder, n: AppNotification) {
        val ctx = holder.itemView.context

        holder.tvIcon.text = n.emoji
        holder.tvIcon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(n.type.iconBg))
        }

        holder.tvTitle.text = n.title
        holder.tvMessage.text = n.message
        holder.tvTime.text = FormatUtils.timeAgo(n.time)

        if (n.isRead) {
            // Đã đọc: nền trắng, chữ nhạt, không có chấm đỏ
            holder.itemView.setBackgroundResource(R.drawable.bg_card)
            holder.tvTitle.setTypeface(null, Typeface.NORMAL)
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_grey2))
            holder.vDot.visibility = View.GONE
        } else {
            // Chưa đọc: nền hồng nhạt, tiêu đề đậm, có chấm đỏ
            holder.itemView.setBackgroundResource(R.drawable.bg_notif_unread)
            holder.tvTitle.setTypeface(null, Typeface.BOLD)
            holder.tvTitle.setTextColor(ctx.getColor(R.color.text_dark))
            holder.vDot.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener { onClick(n) }
        holder.itemView.setOnLongClickListener {
            onLongClick(n)
            true
        }
    }

    override fun getItemCount(): Int = rows.size
}
