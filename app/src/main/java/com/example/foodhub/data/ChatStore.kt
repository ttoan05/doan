package com.example.foodhub.data

import android.content.Context
import com.example.foodhub.data.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Quản lý lưu trữ và phản hồi tin nhắn chat giữa Khách hàng và Shipper.
 */
object ChatStore {

    private const val PREFS_NAME = "foodhub_order_chats"

    fun getShipperInfo(orderId: String): ShipperInfo {
        // Tạo thông tin tài xế sinh động, nhất quán theo mã đơn
        val hash = kotlin.math.abs(orderId.hashCode())
        val names = listOf("Nguyễn Văn Hùng", "Trần Quốc Toàn", "Lê Minh Tuấn", "Phạm Hoàng Nam", "Võ Văn Đức")
        val plates = listOf("59-X1 234.56", "59-G2 678.90", "59-P1 889.12", "59-B3 556.78", "59-E1 992.34")
        val bikes = listOf("Honda Wave Alpha", "Yamaha Sirius", "Honda Air Blade", "Honda Vision", "Yamaha Exciter")
        val name = names[hash % names.size]
        val plate = plates[hash % plates.size]
        val bike = bikes[hash % bikes.size]
        return ShipperInfo(
            name = name,
            phone = "0987654321",
            bikeInfo = "$bike • $plate",
            rating = "4.9 ⭐ (1.250 chuyến)"
        )
    }

    data class ShipperInfo(
        val name: String,
        val phone: String,
        val bikeInfo: String,
        val rating: String
    )

    fun getMessages(context: Context, orderId: String): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("chat_$orderId", null)
        if (raw == null) {
            val shipper = getShipperInfo(orderId)
            val defaultMsg = listOf(
                ChatMessage(
                    orderId = orderId,
                    sender = "SHIPPER",
                    text = "Chào bạn! Tôi là tài xế ${shipper.name}. Tôi đang di chuyển đến quán để nhận món cho bạn nhé! 🛵",
                    time = System.currentTimeMillis() - 45000
                )
            )
            saveMessages(context, orderId, defaultMsg)
            return defaultMsg
        }
        val list = mutableListOf<ChatMessage>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        orderId = obj.optString("orderId"),
                        sender = obj.optString("sender"),
                        text = obj.optString("text"),
                        time = obj.optLong("time")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addMessage(context: Context, orderId: String, message: ChatMessage) {
        val current = getMessages(context, orderId).toMutableList()
        current.add(message)
        saveMessages(context, orderId, current)
    }

    private fun saveMessages(context: Context, orderId: String, list: List<ChatMessage>) {
        val arr = JSONArray()
        list.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("orderId", m.orderId)
                put("sender", m.sender)
                put("text", m.text)
                put("time", m.time)
            }
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("chat_$orderId", arr.toString()).apply()
    }

    /**
     * Tự động tạo câu trả lời thông minh của tài xế dựa trên tin nhắn khách gửi
     */
    fun generateShipperReply(customerText: String): String {
        val lower = customerText.lowercase()
        return when {
            lower.contains("xuống") || lower.contains("sảnh") ->
                "Dạ vâng bạn ơi, tôi đang đứng đợi trước sảnh rồi nhé ạ! 👍"
            lower.contains("cửa") || lower.contains("cổng") || lower.contains("bảo vệ") ->
                "Dạ được ạ, khi tới nơi tôi sẽ đặt đồ gọn gàng và chụp ảnh gửi bạn nhé!"
            lower.contains("chuông") ->
                "Dạ vâng, tôi sẽ gọi điện thoại chứ không bấm chuông nhé bạn."
            lower.contains("gọi") || lower.contains("alo") ->
                "Dạ vâng, trước khi tới khoảng 5 phút tôi sẽ alo trước cho bạn nhé ạ."
            lower.contains("lâu") || lower.contains("chưa") || lower.contains("ở đâu") ->
                "Quán vừa chuẩn bị món xong, tôi đang chạy xe tới ngay đây ạ, khoảng 5-10 phút nữa tới bạn nhé! 🛵"
            lower.contains("cảm ơn") || lower.contains("thanks") || lower.contains("ok") ->
                "Dạ không có gì ạ, chúc bạn ngon miệng nhé! ❤️"
            else ->
                "Dạ tôi đã nhận được tin nhắn của bạn rồi nhé, tôi sẽ lưu ý ạ!"
        }
    }
}
