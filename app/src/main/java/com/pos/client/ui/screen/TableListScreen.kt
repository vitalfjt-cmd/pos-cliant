package com.pos.client.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.Floor
import com.pos.client.data.model.TableStatus
import com.pos.client.viewmodel.TableListViewModel

// ★ 実験的なAPI（TopAppBar）の使用を許可するアノテーション
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableListScreen(
    viewModel: TableListViewModel,
    // ★★★ 修正箇所：クリックイベントをMainActivityに渡すように変更 ★★★
    onTableClicked: (TableStatus) -> Unit
) {
    // ViewModelから状態を収集
    val floors by viewModel.floors.collectAsState()
    val currentFloorId by viewModel.selectedFloorId.collectAsState()
    val tables by viewModel.currentFloorTables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("POS Client: テーブル一覧") })
        },
        // フロアタブをTopBarの下に配置
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

                // ローディング表示とテーブルリストの切り替え
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Text("データをロード中...", modifier = Modifier.padding(top = 70.dp))
                    }
                } else {
                    TableGrid(
                        tables = tables,
                        // ★★★ 修正箇所：MainActivityにイベントを渡す ★★★
                        onTableClicked = onTableClicked
                    )
                }
            }
        }
    )
}

// --- UI Components ---
// (FloorTabRow, TableGrid, TableCard は all_kotlin.txt の内容と同一のため変更なし)
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
    val backgroundColor = if (table.isOccupied) {
        Color(0xFFE57373) // 赤系 (使用中)
    } else {
        Color(0xFF81C784) // 緑系 (空席)
    }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "テーブル ${table.tableId}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "定員: ${table.capacity}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}