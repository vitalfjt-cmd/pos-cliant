package com.pos.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
// import androidx.compose.material.icons.filled.Remove // ★削除: これがエラーの原因
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pos.client.data.model.Floor
import com.pos.client.data.model.TableStatus
import com.pos.client.viewmodel.TableListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    viewModel: TableListViewModel,
    onTableClicked: (TableStatus, Int) -> Unit
) {
    val floors by viewModel.floors.collectAsState()
    val currentFloorId by viewModel.selectedFloorId.collectAsState()
    val tables by viewModel.currentFloorTables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showEntranceDialog by remember { mutableStateOf<TableStatus?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchTableStatuses()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("POS Client: テーブル一覧") })
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                if (floors.isNotEmpty()) {
                    FloorTabRow(
                        floors = floors,
                        selectedFloorId = currentFloorId,
                        onFloorSelected = viewModel::selectFloor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("データをロード中...", modifier = Modifier.padding(top = 70.dp))
                    }
                } else {
                    TableGrid(
                        tables = tables,
                        onTableClicked = { table ->
                            if (table.isOccupied) {
                                onTableClicked(table, 0)
                            } else {
                                showEntranceDialog = table
                            }
                        }
                    )
                }
            }
        }
    )

    showEntranceDialog?.let { table ->
        EntranceDialog(
            tableNumber = table.tableId.toString(),
            onDismiss = { showEntranceDialog = null },
            onConfirm = { count ->
                showEntranceDialog = null
                onTableClicked(table, count)
            }
        )
    }
}

// --- UI Components ---

@Composable
fun EntranceDialog(
    tableNumber: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var count by remember { mutableIntStateOf(2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("入店受付 (Table $tableNumber)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("ご来店人数を入力してください", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (count > 1) count-- },
                        modifier = Modifier.background(Color.LightGray, CircleShape)
                    ) {
                        // ★修正: Icon(Remove) の代わりに Text("-") を使用
                        Text("-", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "$count 名",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(
                        onClick = { count++ },
                        modifier = Modifier.background(Color.LightGray, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "増やす")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(count) }) {
                Text("案内する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun FloorTabRow(
    floors: List<Floor>,
    selectedFloorId: Int,
    onFloorSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = floors.indexOfFirst { it.floorId == selectedFloorId },
        edgePadding = 0.dp
    ) {
        floors.forEach { floor ->
            Tab(
                selected = floor.floorId == selectedFloorId,
                onClick = { onFloorSelected(floor.floorId) },
                text = { Text(floor.name) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun TableGrid(
    tables: List<TableStatus>,
    onTableClicked: (TableStatus) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tables) { table ->
            TableCard(table = table, onTableClicked = onTableClicked)
        }
    }
}

@Composable
fun TableCard(
    table: TableStatus,
    onTableClicked: (TableStatus) -> Unit
) {
    val backgroundColor = if (table.isOccupied) Color(0xFFE57373) else Color(0xFF81C784)
    val statusText = if (table.isOccupied) "使用中" else "空席"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onTableClicked(table) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "テーブル ${table.tableId}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(text = statusText, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(text = "定員: ${table.capacity}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}