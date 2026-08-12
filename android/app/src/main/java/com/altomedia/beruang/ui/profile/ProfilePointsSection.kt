package com.altomedia.beruang.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altomedia.beruang.data.model.Profile
import com.altomedia.beruang.ui.components.QrImage
import com.altomedia.beruang.ui.components.RankBadge
import com.altomedia.beruang.ui.components.RankTier
import com.altomedia.beruang.ui.components.outlinedFieldColors
import com.altomedia.beruang.ui.theme.*

/**
 * The points "wallet" section shown on the profile screen (below the profile
 * header). Shows balance, rank badge, 6-digit account id, and buttons to show
 * the user's QR (virtual account) or scan someone else's to transfer points.
 */
@Composable
fun ProfilePointsSection(
    profile: Profile?,
    onShowQr: () -> Unit,
    onScan: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("POIN BERUANG", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${profile?.points ?: 0}", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("pts", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                }
                Spacer(Modifier.height(6.dp))
                RankBadge(profile?.points ?: 0)
            }
            // Coin icon
            Box(Modifier.size(48.dp).clip(CircleShape).background(GoldSoft), contentAlignment = Alignment.Center) {
                Text("🐻", fontSize = 26.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        // Account ID
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Surface2).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ID Akun", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(profile?.account_id ?: "------", color = Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onShowQr,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Text),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("QR Saya", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onScan,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        val next = RankTier.nextTier(profile?.points ?: 0)
        if (next != null) {
            Spacer(Modifier.height(8.dp))
            Text("Naik ke ${next.label}: butuh ${next.min - (profile?.points ?: 0)} poin lagi.", color = Muted, fontSize = 11.sp)
        }
    }
}

/** Dialog showing the user's QR code (their account_id) for receiving points. */
@Composable
fun MyQrDialog(profile: Profile?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Green) } },
        title = { Text("QR Akun Saya", color = Text, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(profile?.displayName ?: "User", color = Text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                QrImage(profile?.account_id ?: "------", sizeDp = 220)
                Spacer(Modifier.height(10.dp))
                Text("ID: ${profile?.account_id ?: "------"}", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Scan QR ini untuk menerima poin dari pengguna lain.", color = Muted, fontSize = 11.sp)
            }
        }
    )
}

/**
 * Transfer dialog: after scanning a recipient QR (their account_id), the user
 * enters an amount + 4-digit PIN. If the sender has no PIN yet, prompts to
 * create one first.
 */
@Composable
fun TransferDialog(
    recipientAccountId: String,
    hasPin: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreatePin: (String) -> Unit,
    onTransfer: (amount: Long, pin: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toLongOrNull() ?: 0
                    if (!hasPin) onCreatePin(pin) else onTransfer(amt, pin)
                },
                enabled = !busy && pin.length == 4 && amount.toLongOrNull()?.let { it > 0 } == true,
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (busy) CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else Text(if (hasPin) "Kirim" else "Atur PIN", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = Muted) } },
        title = { Text(if (hasPin) "Transfer Poin" else "Atur PIN Transaksi", color = Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface2).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ke ID", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(recipientAccountId, color = Text, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Jumlah poin") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { pin = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(if (hasPin) "PIN (4 digit)" else "Buat PIN (4 digit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (!hasPin) {
                    Spacer(Modifier.height(6.dp))
                    Text("Belum ada PIN. Buat 4 digit angka untuk transaksi poin.", color = Muted, fontSize = 11.sp)
                }
            }
        }
    )
}
