package com.example.foodhub.ui.order

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodhub.R
import com.example.foodhub.data.ChatStore
import com.example.foodhub.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_ORDER_CODE = "extra_order_code"
    }

    private var orderId: String = ""
    private var orderCode: String = ""

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var llQuickReplies: LinearLayout
    private lateinit var chatAdapter: ChatAdapter

    private val messages = mutableListOf<ChatMessage>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_chat)

        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        orderCode = intent.getStringExtra(EXTRA_ORDER_CODE) ?: ""

        val shipper = ChatStore.getShipperInfo(orderId)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvShipperName).text = shipper.name
        findViewById<TextView>(R.id.tvShipperBike).text = "${shipper.bikeInfo} • ${shipper.rating}"
        findViewById<TextView>(R.id.tvChatOrderBanner).text = "Đơn hàng #$orderCode • Tài xế đang trên đường giao"

        findViewById<ImageView>(R.id.btnCallShipper).setOnClickListener {
            dialPhone(shipper.phone)
        }

        rvMessages = findViewById(R.id.rvChatMessages)
        etMessage = findViewById(R.id.etChatMessage)
        btnSend = findViewById(R.id.btnSendMessage)
        llQuickReplies = findViewById(R.id.llQuickReplies)

        setupRecyclerView()
        setupQuickReplies()

        btnSend.setOnClickListener { sendCurrentMessage() }

        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage()
                true
            } else false
        }

        loadMessages()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        val lm = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.layoutManager = lm
        rvMessages.adapter = chatAdapter
    }

    private fun loadMessages() {
        messages.clear()
        messages.addAll(ChatStore.getMessages(this, orderId))
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()
    }

    private fun setupQuickReplies() {
        val quickReplies = listOf(
            "Tôi đang xuống sảnh",
            "Để trước cửa nhà giúp tôi",
            "Gọi trước khi đến 5p",
            "Không bấm chuông nhé",
            "Cảm ơn bác tài nhiều ❤️"
        )
        llQuickReplies.removeAllViews()
        val dp1 = resources.displayMetrics.density.toInt()

        quickReplies.forEach { text ->
            val tv = TextView(this).apply {
                this.text = text
                textSize = 12.5f
                setTextColor(Color.parseColor("#334155"))
                setBackgroundResource(R.drawable.bg_filter_chip_normal)
                setPadding(12 * dp1, 6 * dp1, 12 * dp1, 6 * dp1)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 8 * dp1, 0)
                layoutParams = lp
                setOnClickListener {
                    sendMessage(text)
                }
            }
            llQuickReplies.addView(tv)
        }
    }

    private fun sendCurrentMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        etMessage.setText("")
        sendMessage(text)
    }

    private fun sendMessage(text: String) {
        val msg = ChatMessage(
            orderId = orderId,
            sender = "CUSTOMER",
            text = text,
            time = System.currentTimeMillis()
        )
        ChatStore.addMessage(this, orderId, msg)
        messages.add(msg)
        chatAdapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()

        // Mô phỏng Shipper trả lời tự động thông minh sau 1.2 giây
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) {
                val replyText = ChatStore.generateShipperReply(text)
                val replyMsg = ChatMessage(
                    orderId = orderId,
                    sender = "SHIPPER",
                    text = replyText,
                    time = System.currentTimeMillis()
                )
                ChatStore.addMessage(this@OrderChatActivity, orderId, replyMsg)
                messages.add(replyMsg)
                chatAdapter.notifyItemInserted(messages.size - 1)
                scrollToBottom()
            }
        }, 1200)
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            rvMessages.smoothScrollToPosition(messages.size - 1)
        }
    }

    private fun dialPhone(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể mở ứng dụng gọi điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    inner class ChatAdapter(private val items: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val llShipper = itemView.findViewById<View>(R.id.llShipperMessage)
            val tvShipperText = itemView.findViewById<TextView>(R.id.tvShipperText)
            val tvShipperTime = itemView.findViewById<TextView>(R.id.tvShipperTime)

            val llCustomer = itemView.findViewById<View>(R.id.llCustomerMessage)
            val tvCustomerText = itemView.findViewById<TextView>(R.id.tvCustomerText)
            val tvCustomerTime = itemView.findViewById<TextView>(R.id.tvCustomerTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val item = items[position]
            val timeStr = timeFormat.format(Date(item.time))

            if (item.sender == "CUSTOMER") {
                holder.llCustomer.visibility = View.VISIBLE
                holder.llShipper.visibility = View.GONE
                holder.tvCustomerText.text = item.text
                holder.tvCustomerTime.text = timeStr
            } else {
                holder.llCustomer.visibility = View.GONE
                holder.llShipper.visibility = View.VISIBLE
                holder.tvShipperText.text = item.text
                holder.tvShipperTime.text = timeStr
            }
        }
    }
}
