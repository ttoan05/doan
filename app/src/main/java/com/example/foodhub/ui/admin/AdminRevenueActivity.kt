package com.example.foodhub.ui.admin

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.DatabaseHelper
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.ui.order.OrderDetailActivity
import com.example.foodhub.utils.FormatUtils
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
        findViewById<View>(R.id.btnExport).setOnClickListener {
            AdminRevenueExportHelper.exportCsv(this, filtered(), range.text.toString())
        }
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
            val card = AdminRevenueViewHelper.buildTopFoodCard(this, idx, name, qty, total, sample?.imageRes)
            val lpCard = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lpCard.setMargins(0, if (idx == 0) 0 else dp(8), 0, 0)
            top.addView(card, lpCard)
        }
    }

    private fun renderTransactions(orders: List<Order>) {
        transactions.removeAllViews()

        val displayList = if (selectedDayKey != null) {
            orders.filter { dayKey(it.createdAt) == selectedDayKey }
        } else {
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
            val preview = TextView(this).apply {
                text = "Gợi ý: bấm vào cột biểu đồ hoặc chip ngày để lọc. Bấm \"Tất cả ngày\" sẽ hiện toàn bộ."
                textSize = 12.5f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(dp(4), dp(6), dp(4), dp(6))
                gravity = Gravity.CENTER
            }
            transactions.addView(preview)
            val hint2 = TextView(this).apply {
                text = "Xem nhanh ${minOf(8, orders.size)} giao dịch gần nhất:"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                setPadding(dp(4), dp(10), dp(4), dp(4))
            }
            transactions.addView(hint2)
            orders.sortedByDescending { it.createdAt }.take(8).forEach { order ->
                transactions.addView(AdminRevenueViewHelper.buildTransactionCard(this, order) { clicked ->
                    startActivity(Intent(this, OrderDetailActivity::class.java).putExtra(OrderDetailActivity.EXTRA_ORDER_ID, clicked.id))
                })
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
            transactions.addView(AdminRevenueViewHelper.buildTransactionCard(this, order) { clicked ->
                startActivity(Intent(this, OrderDetailActivity::class.java).putExtra(OrderDetailActivity.EXTRA_ORDER_ID, clicked.id))
            })
        }
    }
}
