package com.example.foodhub.ui.admin

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.foodhub.data.model.Order
import com.example.foodhub.data.model.OrderStatus
import com.example.foodhub.utils.FormatUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper hỗ trợ xuất và chia sẻ báo cáo doanh thu dưới định dạng CSV cho AdminRevenueActivity.
 */
object AdminRevenueExportHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))

    fun csvEscape(s: String): String = "\"" + s.replace("\"", "\"\"").replace("\n", " ") + "\""

    fun exportCsv(activity: Activity, orders: List<Order>, rangeText: String) {
        val title = "FoodHub - Báo cáo doanh thu"
        val sb = StringBuilder("\uFEFF$title\nKhoảng thời gian,${csvEscape(rangeText)}\nNgày xuất,${csvEscape(dateFormat.format(Date()))}\n\n")
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
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download")
                }
                uri = activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                activity.contentResolver.openOutputStream(uri)!!.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
            } else {
                val file = File(activity.getExternalFilesDir("Download"), name)
                file.parentFile?.mkdirs()
                file.writeText(sb.toString(), Charsets.UTF_8)
                uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            }
            Toast.makeText(activity, "Đã xuất báo cáo: $name", Toast.LENGTH_SHORT).show()
            activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Chia sẻ báo cáo CSV"))
        } catch (e: Exception) {
            Toast.makeText(activity, "Không thể xuất báo cáo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
