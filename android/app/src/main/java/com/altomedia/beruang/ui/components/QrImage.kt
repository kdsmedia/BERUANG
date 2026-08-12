package com.altomedia.beruang.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders a QR code for [content] using ZXing, drawn directly on a Compose
 * Canvas (no Bitmap conversion needed). Square, default 200x200 dp.
 */
@Composable
fun QrImage(content: String, sizeDp: Int = 200) {
    val matrix = remember(content, sizeDp) {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 256, 256, hints)
    }
    Canvas(
        Modifier
            .size(sizeDp.dp)
            .background(Color.White)
    ) {
        drawQrMatrix(matrix)
    }
}

private fun DrawScope.drawQrMatrix(matrix: com.google.zxing.common.BitMatrix) {
    val w = matrix.width
    val h = matrix.height
    val cell = minOf(size.width / w, size.height / h)
    val totalW = cell * w
    val totalH = cell * h
    val offX = (size.width - totalW) / 2f
    val offY = (size.height - totalH) / 2f
    for (x in 0 until w) {
        for (y in 0 until h) {
            if (matrix[x, y]) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(offX + x * cell, offY + y * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}
