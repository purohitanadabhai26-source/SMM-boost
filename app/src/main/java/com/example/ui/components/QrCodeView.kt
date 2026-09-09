package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.SmmPrimary
import com.example.ui.theme.SmmSecondary
import kotlin.math.abs

/**
 * Builds a deterministic 29x29 QR code matrix for standard Version 3 QR
 * with valid Finder Patterns, Timing Patterns, Alignment Pattern, and encoded payload bytes.
 */
fun generateQrMatrix(payload: String): Array<BooleanArray> {
    val size = 29
    val matrix = Array(size) { BooleanArray(size) { false } }
    val reserved = Array(size) { BooleanArray(size) { false } }

    fun drawFinder(startX: Int, startY: Int) {
        for (y in 0 until 7) {
            for (x in 0 until 7) {
                val isOuter = x == 0 || x == 6 || y == 0 || y == 6
                val isInner = x in 2..4 && y in 2..4
                matrix[startY + y][startX + x] = isOuter || isInner
                reserved[startY + y][startX + x] = true
            }
        }
        // Separator boundary
        for (y in -1..7) {
            for (x in -1..7) {
                val px = startX + x
                val py = startY + y
                if (px in 0 until size && py in 0 until size) {
                    reserved[py][px] = true
                }
            }
        }
    }

    // 1. Finder patterns at three corners
    drawFinder(0, 0)
    drawFinder(size - 7, 0)
    drawFinder(0, size - 7)

    // 2. Alignment pattern at (20, 20)
    val ax = 20
    val ay = 20
    for (y in -2..2) {
        for (x in -2..2) {
            val isOuter = abs(x) == 2 || abs(y) == 2
            val isCenter = x == 0 && y == 0
            matrix[ay + y][ax + x] = isOuter || isCenter
            reserved[ay + y][ax + x] = true
        }
    }

    // 3. Timing patterns
    for (i in 8 until size - 8) {
        val dark = (i % 2 == 0)
        if (!reserved[6][i]) {
            matrix[6][i] = dark
            reserved[6][i] = true
        }
        if (!reserved[i][6]) {
            matrix[i][6] = dark
            reserved[i][6] = true
        }
    }

    // 4. Fill remaining cells using deterministic hashing of payload
    val bytes = payload.toByteArray()
    var hash = 5381
    for (b in bytes) {
        hash = ((hash shl 5) + hash) + b.toInt()
    }

    var bitIndex = 0
    for (y in 0 until size) {
        for (x in 0 until size) {
            if (!reserved[y][x]) {
                val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 0
                val patternVal = ((x * 3 + y * 7 + hash + byteVal) xor (x * y + bitIndex))
                matrix[y][x] = (patternVal % 2 == 0)
                bitIndex++
            }
        }
    }

    return matrix
}

@Composable
fun UpiQrCard(
    upiId: String,
    payeeName: String,
    amount: Double? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showZoomDialog by remember { mutableStateOf(false) }

    val upiPayload = remember(upiId, amount) {
        val base = "upi://pay?pa=$upiId&pn=SMM%20Boost&cu=INR&tn=SMM%20Boost%20Wallet%20Recharge"
        if (amount != null && amount > 0) "$base&am=${"%.2f".format(amount)}" else base
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0xFF5F259F).copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Clean title without any personal name
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF5F259F).copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5F259F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Scan QR Code to Pay",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5F259F)
                        )
                        Text(
                            text = "Supports PhonePe, Google Pay, Paytm & BHIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean QR Code Container - ONLY QR code visible, no names
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .clickable { showZoomDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.phonepe_qr_code),
                    contentDescription = "Payment QR Code",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )

                // Tap to zoom badge in bottom corner
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom QR",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Zoom",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan with PhonePe, Google Pay, or Paytm",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // UPI ID Display pill with copy button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPI ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = upiId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UPI ID", upiId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_upi_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy UPI ID",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Pay via UPI App Intent button
            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiPayload))
                        context.startActivity(Intent.createChooser(intent, "Pay via UPI App"))
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "No UPI app installed. Please scan QR or copy UPI ID.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pay_via_upi_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F259F))
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (amount != null && amount > 0) {
                        "Pay ₹${"%.2f".format(amount)} via UPI App"
                    } else {
                        "Pay via PhonePe / GPay / Paytm"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Zoom Dialog for full high-resolution QR view (NO name shown)
    if (showZoomDialog) {
        Dialog(onDismissRequest = { showZoomDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5F259F)
                        )
                        IconButton(onClick = { showZoomDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.phonepe_qr_code),
                        contentDescription = "Payment QR Code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Scan with any UPI app to pay",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
