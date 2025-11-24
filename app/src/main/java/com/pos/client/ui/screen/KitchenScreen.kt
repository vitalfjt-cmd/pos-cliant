package com.pos.client.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pos.client.viewmodel.KitchenItem
import com.pos.client.viewmodel.KitchenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(
    viewModel: KitchenViewModel,
    onBackClicked: () -> Unit
) {
    val items by viewModel.kdsItems.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("キッチンディスプレイ (KDS)") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchItems() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "更新")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("調理待ちの注文はありません", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(items) { item ->
                            KdsItemRow(
                                item = item,
                                onStartCooking = { viewModel.updateStatus(item.detailId, "調理中") },
                                onCompleteCooking = { viewModel.updateStatus(item.detailId, "提供済") }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun KdsItemRow(
    item: KitchenItem,
    onStartCooking: () -> Unit,
    onCompleteCooking: () -> Unit
) {
    val isCooking = item.status == "調理中"
    val containerColor = if (isCooking) Color(0xFFFFF9C4) else Color.White // 調理中は黄色っぽく

    ListItem(
        colors = ListItemDefaults.colors(containerColor = containerColor),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.menuName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("x${item.quantity}", style = MaterialTheme.typography.titleLarge)
            }
        },
        overlineContent = {
            Text("Table: ${item.tableNumber} / Status: ${item.status}")
        },
        trailingContent = {
            Row {
                if (!isCooking) {
                    Button(
                        onClick = onStartCooking,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("開始")
                    }
                } else {
                    Button(
                        onClick = onCompleteCooking,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("完了")
                    }
                }
            }
        }
    )
}