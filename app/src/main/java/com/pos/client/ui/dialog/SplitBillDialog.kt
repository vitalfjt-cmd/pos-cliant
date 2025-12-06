package com.pos.client.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.AccountingResponse
import com.pos.client.viewmodel.OrderViewModel

/**
 * 個別会計（伝票分割）用のダイアログ
 * OrderScreen と AccountingScreen の両方から利用可能
 */
@Composable
fun SplitBillDialog(
    orderHistory: AccountingResponse?,
    viewModel: OrderViewModel,
    onDismiss: () -> Unit,
    onExecuteSplit: (List<Int>) -> Unit
) {
    // 選択された明細IDを管理
    val selectedDetailIds = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("個別会計：支払う商品を選択") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // まだ会計が終わっていない商品だけをフィルタリング
                val activeDetails = orderHistory?.details?.filter {
                    it.itemStatus != "会計済" && it.itemStatus != "CANCELLED"
                } ?: emptyList()

                if (activeDetails.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("精算可能な商品がありません", color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(activeDetails) { detail ->
                            val isSelected = selectedDetailIds.contains(detail.detailId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedDetailIds.remove(detail.detailId)
                                        else selectedDetailIds.add(detail.detailId)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedDetailIds.add(detail.detailId)
                                        else selectedDetailIds.remove(detail.detailId)
                                    }
                                )
                                Column {
                                    // ViewModelを使ってメニュー名を表示
                                    Text(viewModel.getMenuName(detail.menuId), fontWeight = FontWeight.Bold)
                                    Text("¥${detail.subtotal} (数量:${detail.quantity})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExecuteSplit(selectedDetailIds.toList()) },
                enabled = selectedDetailIds.isNotEmpty()
            ) {
                Text("分割実行")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}