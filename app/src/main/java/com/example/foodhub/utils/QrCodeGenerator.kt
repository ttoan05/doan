package com.example.foodhub.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Bộ tạo mã QR thanh toán chuẩn quốc gia VietQR / Napas 24/7 và MoMo (chuẩn EMVCo).
 * Hoạt động 100% offline, sinh payload EMVCo chính xác để mọi ứng dụng ngân hàng
 * (MB Bank, Vietcombank, Techcombank, VPBank, ACB...) và ví MoMo quét ra đúng
 * tên người nhận TRUONG THANH TOAN, số tài khoản, số tiền và nội dung chuyển khoản.
 */
object QrCodeGenerator {

    /**
     * Mã BIN chuẩn của các ngân hàng phổ biến (MB Bank = 970422).
     */
    const val BIN_MB_BANK = "970422"

    /**
     * Tạo chuỗi dữ liệu chuẩn EMVCo Merchant-Presented Mode (VietQR / Napas 24/7).
     * Khi quét bằng bất kỳ ứng dụng ngân hàng nào (MB, VCB, Techcombank...),
     * ứng dụng sẽ tự động tra cứu ra tên chủ tài khoản thụ hưởng và điền sẵn số tiền, nội dung.
     */
    fun buildVietQrPayload(
        bankBin: String = BIN_MB_BANK,
        accountNumber: String = "0867656271",
        amount: Int,
        transferNote: String
    ): String {
        // Tag 00: Phiên bản định dạng payload EMVCo (01)
        val f00 = "000201"

        // Tag 01: Phương thức khởi tạo (12: QR động có số tiền, 11: QR tĩnh)
        val f01 = if (amount > 0) "010212" else "010211"

        // Tag 38: Thông tin định danh thanh toán Napas 24/7 (VietQR)
        // Sub-tag 00: GUID Napas (A000000727)
        val guidTlv = "0010A000000727"
        // Sub-tag 01: Tổ chức thụ hưởng (Mã BIN ngân hàng + Số tài khoản)
        val binTlv = "0006$bankBin"
        val accTlv = "01" + String.format("%02d", accountNumber.length) + accountNumber
        val beneficiaryVal = binTlv + accTlv
        val sub01 = "01" + String.format("%02d", beneficiaryVal.length) + beneficiaryVal
        // Sub-tag 02: Mã dịch vụ Napas 24/7 tới số tài khoản (QRIBFTTA)
        val serviceTlv = "0208QRIBFTTA"
        val f38Val = guidTlv + sub01 + serviceTlv
        val f38 = "38" + String.format("%02d", f38Val.length) + f38Val

        // Tag 53: Mã tiền tệ theo ISO 4217 (704 = VNĐ)
        val f53 = "5303704"

        // Tag 54: Số tiền giao dịch
        val f54 = if (amount > 0) {
            val amountStr = amount.toString()
            "54" + String.format("%02d", amountStr.length) + amountStr
        } else ""

        // Tag 58: Mã quốc gia (VN)
        val f58 = "5802VN"

        // Tag 62: Thông tin bổ sung (Nội dung chuyển tiền)
        val cleanNote = transferNote.replace("[^a-zA-Z0-9 ]".toRegex(), "").trim().take(25)
        val f62 = if (cleanNote.isNotEmpty()) {
            val sub08 = "08" + String.format("%02d", cleanNote.length) + cleanNote
            "62" + String.format("%02d", sub08.length) + sub08
        } else ""

        // Tag 63: Mã kiểm tra CRC16 (4 ký tự Hex)
        val rawData = f00 + f01 + f38 + f53 + f54 + f58 + f62 + "6304"
        val crc = calculateCrc16(rawData)
        return rawData + crc
    }

    /**
     * Tính mã CRC16-CCITT (False) theo chuẩn EMVCo:
     * Polynomial = 0x1021, Initial value = 0xFFFF, không đảo bit.
     */
    fun calculateCrc16(data: String): String {
        var crc = 0xFFFF
        val bytes = data.toByteArray(Charsets.US_ASCII)
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return String.format("%04X", crc and 0xFFFF)
    }

    /**
     * Tạo Bitmap ảnh mã QR kèm khung nhận diện thương hiệu sắc nét chuẩn thanh toán.
     * Quét được bởi tất cả App ngân hàng (MB, Vietcombank, Techcombank...) và Ví MoMo.
     */
    fun createPaymentQrBitmap(
        isMomo: Boolean,
        bankCode: String = "MB",
        accountNumber: String = "0867656271",
        accountName: String = "TRUONG THANH TOAN",
        amount: Int,
        transferNote: String,
        width: Int = 600,
        height: Int = 640
    ): Bitmap {
        // Chuỗi payload EMVCo chuẩn Napas 24/7 (MB Bank BIN 970422)
        val qrPayload = buildVietQrPayload(
            bankBin = BIN_MB_BANK,
            accountNumber = accountNumber,
            amount = amount,
            transferNote = transferNote
        )

        // Tạo ma trận QR bằng ZXing với mức sửa lỗi cao M (15%) để quét nhạy và nhanh nhất
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )

        val qrSize = (width * 0.78f).toInt()
        val bitMatrix = MultiFormatWriter().encode(qrPayload, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)

        val qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        val dotColor = Color.parseColor("#0F172A")

        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                qrBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) dotColor else Color.WHITE)
            }
        }

        // Vẽ thẻ kết hợp khung thương hiệu chuyên nghiệp
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Viền ngoài thẻ
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 3f
        val borderRect = RectF(6f, 6f, width - 6f, height - 6f)
        canvas.drawRoundRect(borderRect, 24f, 24f, paint)

        // 2. Banner tiêu đề thương hiệu phía trên
        paint.style = Paint.Style.FILL
        val brandColor = if (isMomo) Color.parseColor("#A50064") else Color.parseColor("#005BAA")
        paint.color = brandColor
        val headerRect = RectF(6f, 6f, width - 6f, 64f)
        canvas.drawRoundRect(headerRect, 24f, 24f, paint)
        canvas.drawRect(6f, 32f, width - 6f, 64f, paint)

        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val headerText = if (isMomo) "VÍ MOMO • QUÉT MÃ CHUYỂN TIỀN" else "VIETQR • NAPAS 24/7 • MB BANK"
        canvas.drawText(headerText, width / 2f, 44f, paint)

        // 3. Vẽ mã QR ở vị trí trung tâm (giữ nguyên không che để máy ảnh quét nhạy 100%)
        val qrLeft = (width - qrSize) / 2f
        val qrTop = 72f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // 4. Chân trang hiển thị thông tin người nhận và số tiền
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("$accountName  •  $accountNumber", width / 2f, qrTop + qrSize + 28f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val noteText = "Số tiền: ${FormatUtils.money(amount)}  •  Cú pháp: $transferNote"
        canvas.drawText(noteText, width / 2f, qrTop + qrSize + 56f, paint)

        return outputBitmap
    }
}
