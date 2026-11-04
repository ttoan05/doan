package com.example.foodhub.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.NotificationStore
import com.example.foodhub.data.OrderManager
import com.example.foodhub.data.model.AppNotification
import com.example.foodhub.data.model.NotificationType
import com.example.foodhub.ui.notification.NotificationAdapter
import com.example.foodhub.ui.notification.SwipeToDeleteCallback

/**
 * Tách riêng toàn bộ phần render và logic của Tab Thông báo (Notifications Content)
 * Bao gồm danh sách thông báo, bộ lọc, vuốt xóa, đánh dấu đã đọc và dialog thông báo.
 */
class MainActivityNotificationsView(private val activity: MainActivity) {

    private var selectedNotifFilter: NotificationType? = null
    var onRefreshRequested: (() -> Unit)? = null

    fun showNotificationsContent() {
        val container = activity.contentContainer
        container.removeAllViews()
        activity.cartBadge = null
        activity.layoutInflater.inflate(R.layout.layout_notifications, container, true)

        val tvSummary = activity.findViewById<TextView>(R.id.tvNotifSummary)
        val btnMarkAll = activity.findViewById<TextView>(R.id.btnMarkAllRead)
        val btnDeleteAll = activity.findViewById<TextView>(R.id.btnDeleteAll)
        val llFilters = activity.findViewById<LinearLayout>(R.id.llNotifFilters)
        val rv = activity.findViewById<RecyclerView>(R.id.rvNotifications)
        val llEmpty = activity.findViewById<View>(R.id.llNotifEmpty)
        val tvEmpty = activity.findViewById<TextView>(R.id.tvNotifEmpty)

        val filters: List<Pair<String, NotificationType?>> = listOf(
            "Tất cả" to null,
            "Đơn hàng" to NotificationType.ORDER,
            "Khuyến mãi" to NotificationType.PROMO,
            "Hệ thống" to NotificationType.SYSTEM
        )

        val adapter = NotificationAdapter(
            onClick = { n -> onNotificationClick(n) },
            onLongClick = { n -> confirmDeleteNotification(n) }
        )
        rv.layoutManager = LinearLayoutManager(activity)
        rv.isNestedScrollingEnabled = false
        rv.adapter = adapter

        ItemTouchHelper(
            SwipeToDeleteCallback(
                canSwipe = { pos -> !adapter.isHeader(pos) },
                onDelete = { pos ->
                    val n = adapter.notificationAt(pos)
                    if (n != null) {
                        NotificationStore.delete(n.id)
                        activity.showToast("Đã xóa thông báo")
                    }
                }
            )
        ).attachToRecyclerView(rv)

        val filterButtons = mutableListOf<Button>()

        fun refresh() {
            val list = NotificationStore.getAll(selectedNotifFilter)
            adapter.submit(list)
            llEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            rv.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            tvEmpty.text = when (selectedNotifFilter) {
                null -> "Bạn chưa có thông báo nào"
                else -> "Chưa có thông báo ${selectedNotifFilter!!.label.lowercase()}"
            }

            filterButtons.forEachIndexed { i, btn ->
                val (label, type) = filters[i]
                val unread = NotificationStore.unreadCount(type)
                btn.text = if (unread > 0) "$label ($unread)" else label
                val isActive = type == selectedNotifFilter
                btn.setBackgroundResource(if (isActive) R.drawable.bg_cat_active else R.drawable.bg_cat_normal)
                btn.setTextColor(if (isActive) Color.WHITE else activity.getColor(R.color.text_grey2))
                btn.setTypeface(null, if (isActive) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            val totalUnread = NotificationStore.unreadCount(null)
            tvSummary.text =
                if (totalUnread > 0) "Bạn có $totalUnread thông báo chưa đọc"
                else "Bạn đã đọc hết thông báo"

            val hasUnread = NotificationStore.unreadCount(selectedNotifFilter) > 0
            btnMarkAll.setTextColor(activity.getColor(if (hasUnread) R.color.red_primary else R.color.text_grey))
        }

        filters.forEach { (label, type) ->
            val btn = Button(activity).apply {
                text = label
                textSize = 13f
                isAllCaps = false
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, activity.dp(8), 0)
                layoutParams = lp
                setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
                setOnClickListener {
                    selectedNotifFilter = type
                    refresh()
                }
            }
            filterButtons.add(btn)
            llFilters.addView(btn)
        }

        btnMarkAll.setOnClickListener {
            if (NotificationStore.unreadCount(selectedNotifFilter) == 0) {
                activity.showToast("Không có thông báo chưa đọc")
            } else {
                NotificationStore.markAllRead(selectedNotifFilter)
                activity.showToast("Đã đánh dấu tất cả là đã đọc")
            }
        }

        btnDeleteAll.setOnClickListener {
            if (NotificationStore.getAll(selectedNotifFilter).isEmpty()) {
                activity.showToast("Không có thông báo để xóa")
            } else {
                confirmDeleteAllNotifications()
            }
        }

        onRefreshRequested = { refresh() }
        refresh()
    }

    private fun onNotificationClick(n: AppNotification) {
        NotificationStore.markRead(n.id)

        val orderId = n.orderId
        when {
            n.type == NotificationType.ORDER && orderId != null &&
                    OrderManager.findById(orderId) != null -> activity.openOrderDetail(orderId)
            n.type == NotificationType.PROMO -> showPromoDialog(n)
            else -> showNotificationDialog(n)
        }
    }

    private fun showPromoDialog(n: AppNotification) {
        val code = n.promoCode
        val builder = AlertDialog.Builder(activity)
            .setTitle(n.title)
            .setMessage(n.message)
            .setNegativeButton("Đóng", null)

        if (code != null) {
            builder.setPositiveButton("Sao chép mã") { _, _ ->
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("promo_code", code))
                activity.showToast("Đã sao chép mã $code")
            }
            builder.setNeutralButton("Đặt món ngay") { _, _ -> activity.selectTab("home") }
        } else {
            builder.setPositiveButton("Đặt món ngay") { _, _ -> activity.selectTab("home") }
        }
        builder.show()
    }

    private fun showNotificationDialog(n: AppNotification) {
        AlertDialog.Builder(activity)
            .setTitle(n.title)
            .setMessage(n.message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun confirmDeleteNotification(n: AppNotification) {
        AlertDialog.Builder(activity)
            .setTitle("Xóa thông báo")
            .setMessage("Bạn muốn xóa thông báo này?")
            .setPositiveButton("Xóa") { _, _ ->
                NotificationStore.delete(n.id)
                activity.showToast("Đã xóa thông báo")
            }
            .setNegativeButton("Không", null)
            .show()
    }

    private fun confirmDeleteAllNotifications() {
        val filter = selectedNotifFilter
        val msg =
            if (filter == null) "Bạn muốn xóa tất cả thông báo?"
            else "Bạn muốn xóa tất cả thông báo ${filter.label.lowercase()}?"

        AlertDialog.Builder(activity)
            .setTitle("Xóa thông báo")
            .setMessage(msg)
            .setPositiveButton("Xóa hết") { _, _ ->
                NotificationStore.deleteAll(filter)
                activity.showToast("Đã xóa thông báo")
            }
            .setNegativeButton("Không", null)
            .show()
    }
}
