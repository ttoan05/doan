package com.example.foodhub.ui.admin

import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.foodhub.R
import com.example.foodhub.data.model.Restaurant
import com.example.foodhub.utils.SampleData
import java.io.File
import java.io.FileOutputStream

/**
 * Tách riêng toàn bộ logic Dialog thêm / sửa / xóa cho Danh mục và Nhà hàng
 * Giữ nguyên 100% logic giao diện và hành vi ban đầu.
 */
object AdminCategoryDialogHelper {

    fun showAddCategoryDialog(
        activity: AdminCategoryActivity,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_category_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatSubtitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogCatClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogCatName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCatCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogCatSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadCatImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadCatPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedCatPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeCatPhotoBadge)

        tvTitle.text = "Thêm danh mục món mới"
        tvSubtitle.text = "Điền tên danh mục và tải ảnh đại diện lên hệ thống"
        btnSave.text = "Thêm danh mục"

        var selectedImagePath: String? = null

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Exception) {}
            }
            ivUploadedPreview.visibility = View.GONE
            tvChangePhotoBadge.visibility = View.GONE
            llUploadPlaceholder.visibility = View.VISIBLE
        }

        cvUploadImage.setOnClickListener {
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(activity, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = activity.dbHelper.insertCategory(name, selectedImagePath) != -1L
            if (ok) {
                SampleData.reloadFromDb(activity)
                Toast.makeText(activity, "Đã thêm danh mục '$name'", Toast.LENGTH_SHORT).show()
                onSuccess()
                dialog.dismiss()
            } else {
                Toast.makeText(activity, "Danh mục này đã tồn tại", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun showEditCategoryDialog(
        activity: AdminCategoryActivity,
        oldName: String,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_category_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogCatTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogCatSubtitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogCatClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogCatName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCatCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogCatSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadCatImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadCatPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedCatPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeCatPhotoBadge)

        tvTitle.text = "Chỉnh sửa danh mục"
        tvSubtitle.text = "Cập nhật tên và đổi hình ảnh đại diện của danh mục"
        btnSave.text = "Lưu thay đổi"
        etName.setText(oldName)

        var selectedImagePath: String? = activity.dbHelper.getCategoryImageUri(oldName)

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        ivUploadedPreview.setImageBitmap(bitmap)
                        ivUploadedPreview.visibility = View.VISIBLE
                        tvChangePhotoBadge.visibility = View.VISIBLE
                        llUploadPlaceholder.visibility = View.GONE
                        return
                    }
                } catch (_: Exception) {}
            }
            val defaultRes = activity.resolveCategoryThumbnail(oldName)
            if (defaultRes != 0) {
                ivUploadedPreview.setImageResource(defaultRes)
                ivUploadedPreview.visibility = View.VISIBLE
                tvChangePhotoBadge.visibility = View.VISIBLE
                llUploadPlaceholder.visibility = View.GONE
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        updateImagePreview(selectedImagePath)

        cvUploadImage.setOnClickListener {
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(activity, "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = activity.dbHelper.updateCategory(oldName, newName, selectedImagePath)
            if (ok) {
                SampleData.reloadFromDb(activity)
                Toast.makeText(activity, "Đã cập nhật danh mục '$newName'", Toast.LENGTH_SHORT).show()
                onSuccess()
                dialog.dismiss()
            } else {
                Toast.makeText(activity, "Không thể cập nhật danh mục", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    fun confirmDeleteCategory(
        activity: AdminCategoryActivity,
        cat: String,
        foodCount: Int,
        onSuccess: () -> Unit
    ) {
        val warning = if (foodCount > 0) {
            "\nLưu ý: Hiện có $foodCount món ăn thuộc danh mục này sẽ chuyển sang danh mục 'Khác'."
        } else ""

        AlertDialog.Builder(activity)
            .setTitle("Xác nhận xóa danh mục")
            .setMessage("Bạn có chắc chắn muốn xóa danh mục '$cat'?$warning")
            .setPositiveButton("Xóa") { _, _ ->
                val ok = activity.dbHelper.deleteCategory(cat)
                if (ok) {
                    SampleData.reloadFromDb(activity)
                    Toast.makeText(activity, "Đã xóa danh mục '$cat'", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(activity, "Không thể xóa danh mục này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun showAddRestaurantDialog(
        activity: AdminCategoryActivity,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_restaurant_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRestTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogRestClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogRestName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogRestDesc)
        val etTime = dialogView.findViewById<EditText>(R.id.etDialogRestTime)
        val etDist = dialogView.findViewById<EditText>(R.id.etDialogRestDist)
        val etBadge = dialogView.findViewById<EditText>(R.id.etDialogRestBadge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogRestCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogRestSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadRestImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadRestPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedRestPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeRestPhotoBadge)

        tvTitle.text = "Thêm nhà hàng mới"
        etTime.setText("20-30 phút")
        etDist.setText("1.5 km")
        etBadge.setText("Freeship đơn từ 50k")

        var selectedImagePath: String? = null

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    ivUploadedPreview.setImageBitmap(bitmap)
                    ivUploadedPreview.visibility = View.VISIBLE
                    tvChangePhotoBadge.visibility = View.VISIBLE
                    llUploadPlaceholder.visibility = View.GONE
                } catch (_: Exception) {}
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        cvUploadImage.setOnClickListener {
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val time = etTime.text.toString().trim().ifEmpty { "20-30 phút" }
            val dist = etDist.text.toString().trim().ifEmpty { "1.5 km" }
            val badge = etBadge.text.toString().trim()

            if (name.isEmpty() || desc.isEmpty()) {
                Toast.makeText(activity, "Vui lòng nhập tên nhà hàng và mô tả/khu vực", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newId = activity.dbHelper.getNextRestaurantId()
            val newRestaurant = Restaurant(
                id = newId,
                name = name,
                desc = desc,
                rating = 5.0,
                time = time,
                dist = dist,
                badge = badge,
                bgColorStart = Color.parseColor("#E63946"),
                bgColorEnd = Color.parseColor("#C1121F"),
                imageRes = 0,
                imageUri = selectedImagePath
            )

            activity.dbHelper.insertRestaurant(newRestaurant)
            SampleData.reloadFromDb(activity)
            Toast.makeText(activity, "Đã thêm nhà hàng '$name' thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSuccess()
        }

        dialog.show()
    }

    fun showEditRestaurantDialog(
        activity: AdminCategoryActivity,
        restaurant: Restaurant,
        onSuccess: () -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_admin_restaurant_form, null)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRestTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnDialogRestClose)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogRestName)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDialogRestDesc)
        val etTime = dialogView.findViewById<EditText>(R.id.etDialogRestTime)
        val etDist = dialogView.findViewById<EditText>(R.id.etDialogRestDist)
        val etBadge = dialogView.findViewById<EditText>(R.id.etDialogRestBadge)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogRestCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogRestSave)

        val cvUploadImage = dialogView.findViewById<View>(R.id.cvUploadRestImage)
        val llUploadPlaceholder = dialogView.findViewById<View>(R.id.llUploadRestPlaceholder)
        val ivUploadedPreview = dialogView.findViewById<ImageView>(R.id.ivUploadedRestPreview)
        val tvChangePhotoBadge = dialogView.findViewById<View>(R.id.tvChangeRestPhotoBadge)

        tvTitle.text = "Chỉnh sửa nhà hàng"
        etName.setText(restaurant.name)
        etDesc.setText(restaurant.desc)
        etTime.setText(restaurant.time)
        etDist.setText(restaurant.dist)
        etBadge.setText(restaurant.badge)

        var selectedImagePath: String? = restaurant.imageUri

        fun updateImagePreview(path: String?) {
            if (!path.isNullOrEmpty() && File(path).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(path)
                    ivUploadedPreview.setImageBitmap(bitmap)
                    ivUploadedPreview.visibility = View.VISIBLE
                    tvChangePhotoBadge.visibility = View.VISIBLE
                    llUploadPlaceholder.visibility = View.GONE
                    return
                } catch (_: Exception) {}
            }
            if (restaurant.imageRes != 0) {
                ivUploadedPreview.setImageResource(restaurant.imageRes)
                ivUploadedPreview.visibility = View.VISIBLE
                tvChangePhotoBadge.visibility = View.VISIBLE
                llUploadPlaceholder.visibility = View.GONE
            } else {
                ivUploadedPreview.visibility = View.GONE
                tvChangePhotoBadge.visibility = View.GONE
                llUploadPlaceholder.visibility = View.VISIBLE
            }
        }

        updateImagePreview(selectedImagePath)

        cvUploadImage.setOnClickListener {
            activity.pickImage { newPath ->
                selectedImagePath = newPath
                updateImagePreview(newPath)
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val time = etTime.text.toString().trim().ifEmpty { "20-30 phút" }
            val dist = etDist.text.toString().trim().ifEmpty { "1.5 km" }
            val badge = etBadge.text.toString().trim()

            if (name.isEmpty() || desc.isEmpty()) {
                Toast.makeText(activity, "Vui lòng nhập tên nhà hàng và mô tả/khu vực", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedRestaurant = restaurant.copy(
                name = name,
                desc = desc,
                time = time,
                dist = dist,
                badge = badge,
                imageUri = selectedImagePath
            )

            activity.dbHelper.updateRestaurant(updatedRestaurant)
            SampleData.reloadFromDb(activity)
            Toast.makeText(activity, "Đã cập nhật nhà hàng '$name' thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onSuccess()
        }

        dialog.show()
    }

    fun confirmDeleteRestaurant(
        activity: AdminCategoryActivity,
        r: Restaurant,
        foodCount: Int,
        onSuccess: () -> Unit
    ) {
        val warning = if (foodCount > 0) {
            "\nLưu ý: Hiện đang có $foodCount món ăn gắn với nhà hàng này."
        } else ""

        AlertDialog.Builder(activity)
            .setTitle("Xác nhận xóa nhà hàng")
            .setMessage("Bạn có chắc chắn muốn xóa nhà hàng '${r.name}'?$warning")
            .setPositiveButton("Xóa") { _, _ ->
                val ok = activity.dbHelper.deleteRestaurant(r.id)
                if (ok) {
                    SampleData.reloadFromDb(activity)
                    Toast.makeText(activity, "Đã xóa nhà hàng '${r.name}'", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(activity, "Không thể xóa nhà hàng này", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    fun saveImageToInternalStorage(activity: AdminCategoryActivity, uri: Uri): String? {
        return try {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = File(activity.filesDir, fileName)
            activity.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
