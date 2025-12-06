package com.pos.client.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WarikanDialog(
    totalAmount: Int,
    onDismiss: () -> Unit
) {
    var peopleCount by remember { mutableStateOf("2") } // デフォルト2名
    val count = peopleCount.toIntOrNull() ?: 1
    val amountPerPerson = if (count > 0) totalAmount / count else 0
    val remainder = if (count > 0) totalAmount % count else 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("割り勘計算") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("合計金額: ¥$totalAmount", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = peopleCount,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length < 3) peopleCount = it },
                    label = { Text("人数") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (count > 0) {
                    Text("1人あたり", style = MaterialTheme.typography.bodyMedium)
                    Text("¥$amountPerPerson", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (remainder > 0) {
                        Text("余り: ¥$remainder", color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("閉じる") }
        }
    )
}