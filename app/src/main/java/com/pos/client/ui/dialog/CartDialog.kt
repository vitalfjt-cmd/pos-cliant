package com.pos.client.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pos.client.data.model.CartItem

@Composable
fun CartDialog(
    cartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onSubmitOrder: () -> Unit
) {
    val total = cartItems.sumOf {
        val itemPrice = it.menuItem.price + it.selectedOptions.sumOf { opt -> opt.price }
        itemPrice * it.quantity
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("注文カート", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("カートは空です", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(cartItems) { item ->
                            val unitPrice = item.menuItem.price + item.selectedOptions.sumOf { it.price }

                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.menuItem.menuName, fontWeight = FontWeight.Bold)
                                    if (item.selectedOptions.isNotEmpty()) {
                                        val opts = item.selectedOptions.joinToString(", ") { it.optionName }
                                        Text("+$opts", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Text("¥$unitPrice x ${item.quantity}")
                                }
                                IconButton(onClick = { onRemoveItem(item) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("合計")
                    Text("¥$total", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (cartItems.isNotEmpty()) {
                    Button(onClick = onSubmitOrder, modifier = Modifier.fillMaxWidth()) { Text("注文を確定する") }
                } else {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(Color.Gray)) { Text("閉じる") }
                }
            }
        }
    }
}