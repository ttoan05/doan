package com.example.foodhub.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.utils.FormatUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AdminRevenueActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var chart: RevenueChartView
    private lateinit var range: TextView
    private lateinit var top: LinearLayout
    private lateinit var transactions: LinearLayout
    private lateinit var dayChips: LinearLayout
    private lateinit var tvHint: TextView
    private lateinit var btnResetDay: View

    private var allOrders = emptyList<Order>()
    private var from: Long? = null
    private var to: Long? = null
    private var selectedDayKey: String? = null
    private var currentFilterLabel: String = "Tất cả"

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val dayLabelFormat = SimpleDateFormat("dd/MM", Locale("vi", "VN"))
    private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val chipSelectedBg = R.drawable.bg_cat_active
    private val chipNormalBg = R.drawable.bg_chip_normal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!UserProfileManager.isAdmin) { finish(); return }
        setContentView(R.layout.activity_admin_revenue)
        db = DatabaseHelper.getInstance(this)
        chart = findViewById(R.id.revenueChart)
        range = findViewById(R.id.tvRange)
        top = findViewById(R.id.llTopFoods)
        transactions = findViewById(R.id.llTransactions)
        dayChips = findViewById(R.id.llDayChips)
        tvHint = findViewById(R.id.tvTransactionHint)
        btnResetDay = findViewById(R.id.btnResetDay)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnExport).setOnClickListener { exportCsv() }
        btnResetDay.setOnClickListener {
            selectedDayKey = null
            chart.selectedKey = null
            chart.invalidate()
            renderDayChips(filtered())
            renderTransactions(filtered())
        }

        chart.onDaySelected = { key ->
            selectedDayKey = key
            renderDayChips(filtered())
            renderTransactions(filtered())
        }

        allOrders = db.getAllOrders()
        renderFilters()
        select("Tất cả")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun renderFilters() {
        val box = findViewById<LinearLayout>(R.id.llFilters)
        box.removeAllViews()
        val filters = listOf("Hôm nay", "7 ngày qua", "Tháng này", "Tất cả", "Tùy chọn")
        filters.forEach { label ->
            val isSelected = label == currentFilterLabel
            val tv = TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(9), dp(14), dp(9))
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) chipSelectedBg else chipNormalBg)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#475569"))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (label == "Tùy chọn") pickFrom() else select(label)
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, dp(8), 0)
            box.addView(tv, lp)
        }
    }

    private fun select(label: String) {
        currentFilterLabel = label
        selectedDayKey = null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        when (label) {
            "Hôm nay" -> {
                from = cal.timeInMillis
                to = from!! + 86400000L - 1
            }
            "7 ngày qua" -> {
                from = cal.timeInMillis - 6 * 86400000L
                to = cal.timeInMillis + 86400000L - 1
            }
            "Tháng này" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                from = cal.timeInMillis
                to = System.currentTimeMillis()
            }
            "Tất cả" -> {
                from = null
                to = null
            }
        }
        renderFilters()
        render()
    }

    private fun pickFrom() { pickDate(null) }

    private fun pickDate(start: Long?) {
        val c = Calendar.getInstance()
        if (start != null) c.timeInMillis = start
        DatePickerDialog(this, { _, y, m, d ->
            val picked = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (start == null) {
                from = picked
                pickDate(picked)
            } else {
                val a = from ?: picked
                val b = picked
                from = minOf(a, b)
                to = maxOf(a, b) + 86400000L - 1
                currentFilterLabel = "Tùy chọn"
                renderFilters()
                selectedDayKey = null
                render()
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun filtered(): List<Order> = allOrders.filter { (from == null || it.createdAt >= from!!) && (to == null || it.createdAt <= to!!) }

    private fun render() {
        val orders = filtered()
        val delivered = orders.filter { it.status == OrderStatus.DELIVERED }
        val cancelled = orders.count { it.status == OrderStatus.CANCELLED }
        val revenue = delivered.sumOf { it.total }
        val successful = delivered.size
        val totalDecided = successful + cancelled

        findViewById<TextView>(R.id.tvRevenue).text = FormatUtils.money(revenue)
        findViewById<TextView>(R.id.tvOrders).text = "$successful / ${orders.size}"
        findViewById<TextView>(R.id.tvAov).text = FormatUtils.money(if (successful == 0) 0 else revenue / successful)
        val cancelRate = if (totalDecided == 0) 0 else cancelled * 100 / totalDecided
        findViewById<TextView>(R.id.tvCancel).text = "$cancelled ($cancelRate%)"

        range.text = if (from == null) "Khoảng thời gian: Tất cả"
        else "Khoảng thời gian: ${dateFormat.format(Date(from!!))} - ${dateFormat.format(Date(to!!))}"

        val dailyMap = delivered.groupBy { dayKey(it.createdAt) }.toSortedMap()
        val dayKeys = dailyMap.keys.toList()
        val dayLabels = dayKeys.map { k ->
            val p = dayKeyFormat.parse(k)
            if (p == null) k else dayLabelFormat.format(p)
        }
        val dailyVals = dailyMap.values.map { it.sumOf(Order::total).toLong() }
        chart.setData(dayKeys, dayLabels, dailyVals, selectedDayKey)

        val cod = delivered.filter { it.paymentMethod.contains("COD", ignoreCase = true) }.sumOf { it.total }
        val momo = delivered.filter { it.paymentMethod.contains("MoMo", ignoreCase = true) }.sumOf { it.total }
        val bank = delivered.filter { !it.paymentMethod.contains("COD", ignoreCase = true) && !it.paymentMethod.contains("MoMo", ignoreCase = true) }.sumOf { it.total }
        val totalPay = (cod + momo + bank).coerceAtLeast(1)
        findViewById<TextView>(R.id.tvPayments).text =
            "Chỉ tính đơn Đã giao  •  Tổng: ${FormatUtils.money(cod + momo + bank)}\n" +
            "COD: ${FormatUtils.money(cod)} (${cod * 100 / totalPay}%)  •  MoMo: ${FormatUtils.money(momo)} (${momo * 100 / totalPay}%)  •  Bank QR: ${FormatUtils.money(bank)} (${bank * 100 / totalPay}%)"
        findViewById<PaymentChartView>(R.id.paymentChart).setValues(listOf(cod, momo, bank))

        renderTop(delivered)
        renderDayChips(orders)
        renderTransactions(orders)
    }

    private fun dayKey(t: Long): String = dayKeyFormat.format(Date(t))

    private fun renderDayChips(orders: List<Order>) {
        dayChips.removeAllViews()
        val groups = orders.groupBy { dayKey(it.createdAt) }.toSortedMap()
        if (groups.isEmpty()) return
        // Limit chips to at most 14 most recent days for usability
        val entries = groups.entries.toList()
        val toShow = if (entries.size > 14) entries.takeLast(14) else entries
        toShow.forEach { (key, list) ->
            val p = dayKeyFormat.parse(key)
            val label = if (p == null) key else dayLabelFormat.format(p)
            val count = list.size
            val isSelected = key == selectedDayKey
            val chip = TextView(this).apply {
                text = "$label ($count)"
                textSize = 12.5f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(7), dp(12), dp(7))
                setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                setBackgroundResource(if (isSelected) chipSelectedBg else chipNormalBg)
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#475569"))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedDayKey = if (selectedDayKey == key) null else key
                    chart.selectedKey = selectedDayKey
                    chart.invalidate()
                    renderDayChips(orders)
                    renderTransactions(orders)
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, dp(8), 0)
            dayChips.addView(chip, lp)
        }
    }

    private fun renderTop(orders: List<Order>) {
        top.removeAllViews()
        val grouped = orders.flatMap { it.items }
            .groupBy { it.foodId to it.name }
            .map { (_, items) -> Triple(items.first().name, items.sumOf { it.quantity }, items.sumOf { it.total }) }
            .sortedByDescending { it.second }
            .take(5)

        if (grouped.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Chưa có dữ liệu món bán chạy trong khoảng này"
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(dp(4), dp(10), dp(4), dp(6))
            }
            top.addView(empty)
            return
        }

        grouped.forEachIndexed { idx, (name, qty, total) ->
            val sample = orders.flatMap { it.items }.firstOrNull { it.name == name }
            val card = androidx.cardview.widget.CardView(this).apply {
                radius = dp(12).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#F8FAFC"))
                useCompatPadding = false
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }
            val rank = TextView(this).apply {
                text = "#${idx + 1}"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
                background = gradientRankBg(idx)
                setPadding(0, 0, 0, 0)
            }
            val image = ImageView(this).apply {
                val lp2 = LinearLayout.LayoutParams(dp(48), dp(48))
                lp2.setMargins(dp(10), 0, 0, 0)
                layoutParams = lp2
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = null
                if (sample?.imageRes != null && sample.imageRes != 0) setImageResource(sample.imageRes)
                else setImageResource(R.drawable.img_banh_mi)
            }
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val lp3 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp3.setMargins(dp(12), 0, 0, 0)
                layoutParams = lp3
            }
            val tvName = TextView(this).apply {
                text = name
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val tvMeta = TextView(this).apply {
                text = "Đã bán: $qty phần  •  Doanh thu: ${FormatUtils.money(total)}"
                textSize = 12.5f
                setTextColor(Color.parseColor("#475569"))
                setPadding(0, dp(2), 0, 0)
            }
            texts.addView(tvName)
            texts.addView(tvMeta)
            row.addView(rank)
            row.addView(image)
            row.addView(texts)
            card.addView(row)
            val lpCard = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lpCard.setMargins(0, if (idx == 0) 0 else dp(8), 0, 0)
            top.addView(card, lpCard)
        }
    }

    private fun gradientRankBg(idx: Int): android.graphics.drawable.GradientDrawable {
        val c = when (idx) {
            0 -> Color.parseColor("#F59E0B")
            1 -> Color.parseColor("#94A3B8")
            2 -> Color.parseColor("#EA580C")
            else -> Color.parseColor("#CBD5E1")
        }
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(c)
        }
    }

    private fun renderTransactions(orders: List<Order>) {
        transactions.removeAllViews()

        val displayList = if (selectedDayKey != null) {
            orders.filter { dayKey(it.createdAt) == selectedDayKey }
        } else {
            // Khi chưa chọn ngày và danh sách dài, chỉ gợi ý chọn ngày thay vì đổ hết gây dài
            if (orders.size > 12 && currentFilterLabel == "Tất cả") {
                emptyList()
            } else if (orders.size > 20) {
                orders.take(20)
            } else orders
        }

        val isFilteredByDay = selectedDayKey != null
        btnResetDay.visibility = if (isFilteredByDay) View.VISIBLE else View.GONE

        if (selectedDayKey == null && orders.size > 12 && currentFilterLabel == "Tất cả") {
            tvHint.text = "Đang hiển thị ${orders.size} giao dịch. Chọn một ngày ở biểu đồ hoặc chip bên trên để xem chi tiết từng ngày."
            // show a compact preview count instead of full flood
            val preview = TextView(this).apply {
                text = "Gợi ý: bấm vào cột biểu đồ hoặc chip ngày để lọc. Bấm \"Tất cả ngày\" sẽ hiện toàn bộ."
                textSize = 12.5f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(dp(4), dp(6), dp(4), dp(6))
                gravity = Gravity.CENTER
            }
            transactions.addView(preview)
            // still allow quick view of latest 8 as collapsed preview
            val hint2 = TextView(this).apply {
                text = "Xem nhanh ${minOf(8, orders.size)} giao dịch gần nhất:"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                setPadding(dp(4), dp(10), dp(4), dp(4))
            }
            transactions.addView(hint2)
            orders.sortedByDescending { it.createdAt }.take(8).forEach { order ->
                transactions.addView(buildTransactionCard(order))
            }
            val more = TextView(this).apply {
                text = "Và ${orders.size - 8} giao dịch khác — chọn ngày để xem hết"
                textSize = 12f
                setTextColor(Color.parseColor("#2563EB"))
                setPadding(dp(4), dp(8), dp(4), dp(2))
                gravity = Gravity.CENTER
            }
            if (orders.size > 8) transactions.addView(more)
            return
        }

        if (selectedDayKey != null) {
            val p = dayKeyFormat.parse(selectedDayKey!!)
            val label = if (p == null) selectedDayKey!! else dateFormat.format(p)
            tvHint.text = "Đang xem ${displayList.size} giao dịch ngày $label  •  Doanh thu ngày: ${FormatUtils.money(displayList.filter { it.status == OrderStatus.DELIVERED }.sumOf { it.total })}"
        } else {
            if (orders.isEmpty()) tvHint.text = "Chưa có giao dịch trong khoảng này"
            else if (orders.size > 20) tvHint.text = "Đang hiển thị 20 / ${orders.size} giao dịch gần nhất — chọn ngày để xem chính xác hơn"
            else tvHint.text = "Hiển thị ${orders.size} giao dịch trong khoảng đã chọn  •  Bấm vào đơn để xem chi tiết"
        }

        if (displayList.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (orders.isEmpty()) "Không có giao dịch nào" else "Không có giao dịch nào cho ngày đã chọn"
                textSize = 13f
                setTextColor(Color.parseColor("#64748B"))
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(18), dp(12), dp(18))
            }
            transactions.addView(empty)
            return
        }

        val toShow = displayList.sortedByDescending { it.createdAt }
        toShow.forEach { order ->
            transactions.addView(buildTransactionCard(order))
        }
    }

    private fun buildTransactionCard(order: Order): View {
        val card = androidx.cardview.widget.CardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = 1f
            setCardBackgroundColor(Color.WHITE)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvCode = TextView(this).apply {
            text = order.code
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvStatus = TextView(this).apply {
            text = order.status.label
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setTextColor(Color.WHITE)
            background = statusBg(order.status)
        }
        header.addView(tvCode)
        header.addView(tvStatus)

        val tvTime = TextView(this).apply {
            text = "${FormatUtils.dateTime(order.createdAt)}  •  ${order.restaurant}"
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, dp(4), 0, 0)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvCustomer = TextView(this).apply {
            text = "${order.receiverName} • ${order.phone}"
            textSize = 12.5f
            setTextColor(Color.parseColor("#1E293B"))
            setPadding(0, dp(6), 0, 0)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvItems = TextView(this).apply {
            text = order.items.joinToString(", ") { "${it.name} x${it.quantity}" }
            textSize = 12.5f
            setTextColor(Color.parseColor("#334155"))
            setPadding(0, dp(4), 0, 0)
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val tvPay = TextView(this).apply {
            text = order.paymentMethod
            textSize = 11.5f
            setTextColor(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvTotal = TextView(this).apply {
            text = FormatUtils.money(order.total)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (order.status == OrderStatus.DELIVERED) Color.parseColor("#047857") else Color.parseColor("#0F172A"))
        }
        footer.addView(tvPay)
        footer.addView(tvTotal)

        col.addView(header)
        col.addView(tvTime)
        col.addView(tvCustomer)
        col.addView(tvItems)
        col.addView(footer)
        card.addView(col)
        card.isClickable = true
        card.isFocusable = true
        card.foreground = getDrawable(android.R.drawable.list_selector_background)
        card.setOnClickListener {
            startActivity(Intent(this, OrderDetailActivity::class.java).putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id))
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(0, dp(8), 0, 0)
        card.layoutParams = lp
        return card
    }

    private fun statusBg(status: OrderStatus): android.graphics.drawable.GradientDrawable {
        val color = when (status) {
            OrderStatus.DELIVERED -> "#16A34A"
            OrderStatus.SHIPPING -> "#2563EB"
            OrderStatus.PROCESSING -> "#F59E0B"
            OrderStatus.CANCELLED -> "#DC2626"
        }
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.parseColor(color))
        }
    }

    private fun csvEscape(s: String) = "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""

    private fun exportCsv() {
        val orders = filtered()
        val title = "FoodHub - Báo cáo doanh thu"
        val sb = StringBuilder("\uFEFF$title\nKhoảng thời gian,${csvEscape(range.text.toString())}\nNgày xuất,${csvEscape(dateFormat.format(Date()))}\n\n")
        sb.append("Mã đơn,Ngày giờ,Khách hàng,SĐT,Địa chỉ,Danh sách món,Tiền món,Phí ship,Giảm giá,Tổng tiền,Phương thức thanh toán,Trạng thái\n")
        orders.sortedBy { it.createdAt }.forEach { o ->
            val itemsStr = o.items.joinToString(" | ") { i -> "${i.name} x${i.quantity}" }
            val cols = listOf(
                o.code, FormatUtils.dateTime(o.createdAt), o.receiverName, o.phone, o.address,
                itemsStr, o.subtotal.toString(), o.shippingFee.toString(), o.discount.toString(), o.total.toString(), o.paymentMethod, o.status.label
            ).map { v -> csvEscape(v) }
            sb.append(cols.joinToString(",")).append('\n')
        }
        val delivered = orders.filter { it.status == OrderStatus.DELIVERED }
        sb.append("\nTỔNG DOANH THU (Đã giao),${delivered.sumOf { it.total }}\n")
        sb.append("ĐƠN THÀNH CÔNG,${delivered.size}\n")
        sb.append("ĐƠN HỦY,${orders.count { it.status == OrderStatus.CANCELLED }}\n")
        sb.append("TỔNG SỐ ĐƠN,${orders.size}\n")
        sb.append("AOV,${if (delivered.isEmpty()) 0 else delivered.sumOf { it.total } / delivered.size}\n")
        try {
            val name = "foodhub_revenue_${System.currentTimeMillis()}.csv"
            val uri: Uri
            if (Build.VERSION.SDK_INT >= 29) {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download")
                }
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                contentResolver.openOutputStream(uri)!!.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
            } else {
                val file = File(getExternalFilesDir("Download"), name)
                file.parentFile?.mkdirs()
                file.writeText(sb.toString(), Charsets.UTF_8)
                uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            }
            Toast.makeText(this, "Đã xuất báo cáo: $name", Toast.LENGTH_SHORT).show()
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Chia sẻ báo cáo CSV"))
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể xuất báo cáo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
