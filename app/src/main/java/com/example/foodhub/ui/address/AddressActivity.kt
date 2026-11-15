package com.example.foodhub.ui.address

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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.foodhub.R
import com.example.foodhub.data.AddressStore
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
            showEditAddressDialog(null)
        }

        findViewById<View>(R.id.btnAddEmpty)?.setOnClickListener {
            showEditAddressDialog(null)
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

            // Xóa địa chỉ khi bấm icon thùng rác
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

            // Bấm vào cả ô địa chỉ để mở hộp thoại chỉnh sửa / cập nhật
            itemView.setOnClickListener {
                showEditAddressDialog(addr)
            }

            llAddressContainer.addView(itemView)
        }
    }

    /**
     * Mở hộp thoại:
     * - Nếu `addressToEdit == null`: Thêm địa chỉ mới
     * - Nếu `addressToEdit != null`: Sửa/Cập nhật lại địa chỉ đã chọn
     */
    private fun showEditAddressDialog(addressToEdit: SavedAddress?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvAddressDialogTitle)
        val rbHome = dialogView.findViewById<RadioButton>(R.id.rbHome)
        val rbWork = dialogView.findViewById<RadioButton>(R.id.rbWork)
        val rbOther = dialogView.findViewById<RadioButton>(R.id.rbOther)
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)
        val etReceiver = dialogView.findViewById<EditText>(R.id.etReceiver)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val cbIsDefault = dialogView.findViewById<CheckBox>(R.id.cbIsDefault)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        if (addressToEdit != null) {
            tvDialogTitle.text = "Chỉnh sửa địa chỉ"
            btnSave.text = "Cập nhật"

            etAddress.setText(addressToEdit.address)
            etReceiver.setText(addressToEdit.receiver)
            etPhone.setText(addressToEdit.phone)
            cbIsDefault.isChecked = addressToEdit.isDefault

            when (addressToEdit.label) {
                "Nhà riêng", "Nhà" -> rbHome.isChecked = true
                "Văn phòng", "Công ty" -> rbWork.isChecked = true
                else -> rbOther.isChecked = true
            }
        } else {
            tvDialogTitle.text = "Thêm địa chỉ mới"
            btnSave.text = "Lưu địa chỉ"
            rbHome.isChecked = true
            cbIsDefault.isChecked = AddressStore.getAll(this).isEmpty()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val address = etAddress.text.toString().trim()
            val receiver = etReceiver.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (address.isEmpty() || receiver.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val label = when {
                rbHome.isChecked -> "Nhà riêng"
                rbWork.isChecked -> "Văn phòng"
                else -> "Khác"
            }

            if (addressToEdit != null) {
                val updated = addressToEdit.copy(
                    label = label,
                    address = address,
                    receiver = receiver,
                    phone = phone,
                    isDefault = cbIsDefault.isChecked
                )
                AddressStore.update(this, updated)
                Toast.makeText(this, "Đã cập nhật địa chỉ", Toast.LENGTH_SHORT).show()
            } else {
                val newAddr = SavedAddress(
                    label = label,
                    address = address,
                    receiver = receiver,
                    phone = phone,
                    isDefault = cbIsDefault.isChecked
                )
                AddressStore.insert(this, newAddr)
                Toast.makeText(this, "Đã lưu địa chỉ", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            renderAddressList()
        }

        dialog.show()
    }
}
