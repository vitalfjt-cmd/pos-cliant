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
import com.pos.client.data.model.MenuBook // 追加

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    viewModel: TableListViewModel,
    // ★修正: bookId も渡せるように変更
    onTableClicked: (TableStatus, Int, Int) -> Unit // table, count, bookId
//    onTableClicked: (TableStatus, Int) -> Unit
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
//                                // 使用中の場合は bookId は不要（0などでOK）
//                                onTableClicked(table, 0, 0)
                                // ★★★ 修正箇所: ここでサーバーから来た bookId を渡す ★★★
                                // bookId が null の場合は安全策として 1 (通常) を渡す
                                onTableClicked(table, 0, table.bookId ?: 1)
                            } else {
                                showEntranceDialog = table
                            }
                        }
                    )
//                    TableGrid(
//                        tables = tables,
//                        onTableClicked = { table ->
//                            if (table.isOccupied) {
//                                onTableClicked(table, 0)
//                            } else {
//                                showEntranceDialog = table
//                            }
//                        }
//                    )
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

//    showEntranceDialog?.let { table ->
//        EntranceDialog(
//            tableNumber = table.tableId.toString(),
//            onDismiss = { showEntranceDialog = null },
//            onConfirm = { count ->
//                showEntranceDialog = null
//                onTableClicked(table, count)
//            }
//        )
//    }
}

// --- UI Components ---

@Composable
fun EntranceDialog(
    tableNumber: String,
    menuBooks: List<MenuBook>, // ★追加
    onDismiss: () -> Unit,
//    onConfirm: (Int) -> Unit // count, bookId
    // ★★★ 修正箇所: (Int) -> Unit を (Int, Int) -> Unit に変更 ★★★
    onConfirm: (Int, Int) -> Unit
) {
    var count by remember { mutableIntStateOf(2) }
    // ★追加: 選択されたブックID (初期値はリストの先頭、なければ1)
    var selectedBookId by remember { mutableIntStateOf(menuBooks.firstOrNull()?.bookId ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("入店受付 (Table $tableNumber)") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // --- 人数選択 (既存) ---
                Text("ご来店人数", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (count > 1) count-- }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Text("-", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$count 名", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { count++ }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "増やす")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- ★追加: メニューブック選択 ---
                Text("メニュー種別", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                // 簡易的な選択ボタンリスト
                menuBooks.forEach { book ->
                    val isSelected = (book.bookId == selectedBookId)
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                    val textColor = if (isSelected) Color.White else Color.Black

                    OutlinedButton(
                        onClick = { selectedBookId = book.bookId },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isSelected) color else Color.Transparent),
                        border = if(isSelected) null else ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text(book.bookName, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(count, selectedBookId) }) { // ★修正
                Text("案内する")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("入店受付 (Table $tableNumber)") },
//        text = {
//            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
//                Text("ご来店人数を入力してください", style = MaterialTheme.typography.bodyMedium)
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    IconButton(
//                        onClick = { if (count > 1) count-- },
//                        modifier = Modifier.background(Color.LightGray, CircleShape)
//                    ) {
//                        // ★修正: Icon(Remove) の代わりに Text("-") を使用
//                        Text("-", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
//                    }
//
//                    Text(
//                        text = "$count 名",
//                        style = MaterialTheme.typography.headlineMedium,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(horizontal = 24.dp)
//                    )
//
//                    IconButton(
//                        onClick = { count++ },
//                        modifier = Modifier.background(Color.LightGray, CircleShape)
//                    ) {
//                        Icon(Icons.Default.Add, contentDescription = "増やす")
//                    }
//                }
//            }
//        },
//        confirmButton = {
//            Button(onClick = { onConfirm(count) }) {
//                Text("案内する")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("キャンセル")
//            }
//        }
//    )
//}

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