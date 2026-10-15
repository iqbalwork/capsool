package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.platform.barcode.BarcodeEncoder
import com.iqbalwork.capsule.platform.barcode.BarcodeMatrix
import kotlin.math.max
import kotlin.math.min

/**
 * F-57: Renders a barcode matrix with integer-scaled pixels on an opaque white plate
 * with quiet zone, supporting 180-degree flip for presenting to cashiers/scanners.
 */
@Composable
fun ExpandedBarcodeCard(
    barcode: BarcodeContent,
    modifier: Modifier = Modifier,
    plateHeight: Dp = 90.dp,
    showLabel: Boolean = true
) {
    val matrix: BarcodeMatrix? = remember(barcode.content, barcode.format) {
        runCatching { BarcodeEncoder.encode(barcode.content, barcode.format) }.getOrNull()
    }

    val codeColor = remember(barcode.color) {
        val c = Color(barcode.color)
        if (c.alpha == 0f) Color.Black else c
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .rotate(if (barcode.flipped) 180f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (matrix != null) {
            val is2D = matrix.height > 1
            // White plate behind the code + quiet zone padding
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BarcodeCanvas(
                    matrix = matrix,
                    color = codeColor,
                    is2D = is2D,
                    height = plateHeight
                )
            }
        }

        if (showLabel) {
            val labelText = barcode.label.ifBlank { barcode.content }
            if (labelText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = labelText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BarcodeCanvas(
    matrix: BarcodeMatrix,
    color: Color,
    is2D: Boolean,
    height: Dp
) {
    val canvasModifier = if (is2D) {
        Modifier.size(height)
    } else {
        Modifier
            .fillMaxWidth(0.88f)
            .height(height)
    }

    Canvas(modifier = canvasModifier) {
        drawRect(color = Color.White)

        if (is2D) {
            val moduleSize = max(1, min(size.width / matrix.width, size.height / matrix.height).toInt())
            val drawnW = matrix.width * moduleSize
            val drawnH = matrix.height * moduleSize
            val startX = (size.width - drawnW) / 2f
            val startY = (size.height - drawnH) / 2f

            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix.get(x, y)) {
                        drawRect(
                            color = color,
                            topLeft = Offset(startX + x * moduleSize, startY + y * moduleSize),
                            size = Size(moduleSize.toFloat(), moduleSize.toFloat())
                        )
                    }
                }
            }
        } else {
            val moduleWidth = max(1, (size.width / matrix.width).toInt())
            val drawnW = matrix.width * moduleWidth
            val startX = (size.width - drawnW) / 2f

            for (x in 0 until matrix.width) {
                if (matrix.get(x, 0)) {
                    drawRect(
                        color = color,
                        topLeft = Offset(startX + x * moduleWidth, 0f),
                        size = Size(moduleWidth.toFloat(), size.height)
                    )
                }
            }
        }
    }
}
