package com.pos.client.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.Floor
import com.pos.client.data.model.TableStatus
import com.pos.client.viewmodel.TableListViewModel
import com.pos.client.ui.dialog.EntranceDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    viewModel: TableListViewModel,
    // ★修正: bookId も渡せるように変更
    onTableClicked: (TableStatus, Int, Int) -> Unit // table, count, bookId
) {
    // ... (既存のState取得)
    val menuBooks by viewModel.menuBooks.collectAsState() // ★追加
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
                    // ★修正: テーブルクリック時の処理
                    TableGrid(
                        tables = tables,
                        onTableClicked = { table ->
                            if (table.isOccupied) {
                                // ★★★ 修正箇所: ここでサーバーから来た bookId を渡す ★★★
                                // bookId が null の場合は安全策として 1 (通常) を渡す
                                onTableClicked(table, 0, table.bookId ?: 1)
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
            menuBooks = menuBooks, // ★追加: 一覧を渡す
            onDismiss = { showEntranceDialog = null },
            onConfirm = { count, bookId -> // ★修正: bookIdも受け取る
                showEntranceDialog = null
                onTableClicked(table, count, bookId)
            }
        )
    }
}

// --- UI Components ---
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