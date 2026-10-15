package com.example.foodhub.ui.address

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.AddressStore
import com.example.foodhub.data.UserProfileManager
import com.example.foodhub.data.model.SavedAddress

class AddressActivity : AppCompatActivity() {

    private lateinit var llAddressContainer: LinearLayout
    private lateinit var llEmptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address)

        llAddressContainer = findViewById(R.id.llAddressContainer)
        llEmptyState = findViewById(R.id.llEmptyState)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnAddTop).setOnClickListener {
            showAddAddressDialog()
        }

        findViewById<View>(R.id.btnEmptyAdd).setOnClickListener {
            showAddAddressDialog()
        }

        renderAddressList()
    }

    override fun onResume() {
        super.onResume()
        renderAddressList()
    }

    private fun renderAddressList() {
        llAddressContainer.removeAllViews()
        val addresses = AddressStore.getAll(this)

        if (addresses.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            llAddressContainer.visibility = View.GONE
            return
        }

        llEmptyState.visibility = View.GONE
        llAddressContainer.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        addresses.forEach { addr ->
            val itemView = inflater.inflate(R.layout.item_address_card, llAddressContainer, false)

            val ivIcon = itemView.findViewById<ImageView>(R.id.ivAddressIcon)
            val tvLabel = itemView.findViewById<TextView>(R.id.tvAddressLabel)
            val tvDetail = itemView.findViewById<TextView>(R.id.tvAddressDetail)
            val tvReceiver = itemView.findViewById<TextView>(R.id.tvReceiverInfo)
            val btnDelete = itemView.findViewById<ImageView>(R.id.btnDeleteAddress)

            tvLabel.text = addr.label.ifEmpty { "Nhà" }
            tvDetail.text = addr.address
            tvReceiver.text = "${addr.receiver} • ${addr.phone}"

            ivIcon.setImageResource(R.drawable.ic_house)

            // Delete action
            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa địa chỉ \"${addr.label} - ${addr.address}\"?")
                    .setPositiveButton("Xóa") { _, _ ->
                        AddressStore.delete(this, addr.id)
                        Toast.makeText(this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show()
                        renderAddressList()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }

            // Click card to set as default or view
            itemView.setOnClickListener {
                if (!addr.isDefault) {
                    AddressStore.setDefault(this, addr.id)
                    Toast.makeText(this, "Đã chọn làm địa chỉ mặc định: ${addr.label}", Toast.LENGTH_SHORT).show()
                    renderAddressList()
                } else {
                    Toast.makeText(this, "Địa chỉ: ${addr.shortLine}", Toast.LENGTH_SHORT).show()
                }
            }

            llAddressContainer.addView(itemView)
        }
    }

    private fun showAddAddressDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null)
        val rbHome = dialogView.findViewById<RadioButton>(R.id.rbHome)
        val rbWork = dialogView.findViewById<RadioButton>(R.id.rbWork)
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)
        val etReceiver = dialogView.findViewById<EditText>(R.id.etReceiver)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val cbIsDefault = dialogView.findViewById<CheckBox>(R.id.cbIsDefault)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Pre-fill user information if available
        if (UserProfileManager.name.isNotEmpty()) {
            etReceiver.setText(UserProfileManager.name)
        }
        if (UserProfileManager.phone.isNotEmpty()) {
            etPhone.setText(UserProfileManager.phone)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val addrText = etAddress.text.toString().trim()
            val receiver = etReceiver.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (addrText.isEmpty()) {
                etAddress.error = "Vui lòng nhập địa chỉ"
                etAddress.requestFocus()
                return@setOnClickListener
            }
            if (receiver.isEmpty()) {
                etReceiver.error = "Vui lòng nhập tên người nhận"
                etReceiver.requestFocus()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhone.error = "Vui lòng nhập số điện thoại"
                etPhone.requestFocus()
                return@setOnClickListener
            }

            val label = when {
                rbHome.isChecked -> "Nhà"
                rbWork.isChecked -> "Công ty"
                else -> "Khác"
            }

            val saved = SavedAddress(
                id = 0L,
                label = label,
                receiver = receiver,
                phone = phone,
                address = addrText,
                isDefault = cbIsDefault.isChecked
            )

            AddressStore.insert(this, saved)
            Toast.makeText(this, "Đã thêm địa chỉ thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            renderAddressList()
        }

        dialog.show()
    }
}
