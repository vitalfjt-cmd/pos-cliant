package com.pos.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.client.data.model.MenuItem
import com.pos.client.viewmodel.OrderViewModel
import com.pos.client.viewmodel.MenuStructure

// ★ 実験的なAPI（TopAppBar）の使用を許可するアノテーション
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    tableId: Int,
    bookId: Int,
    onBackClicked: () -> Unit
) {
    // ViewModelから状態を収集
    val menuStructure by viewModel.menuStructure.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    // ★ 注文IDの状態を収集
    val currentOrderId by viewModel.currentOrderId.collectAsState()

    // ★★★ 修正箇所：
    // この画面が初めて表示された時に
    // 1. サーバーからメニュー構造をロード
    // 2. サーバーに「注文開始」を通知
    LaunchedEffect(tableId, bookId) {
        viewModel.loadMenuStructure(bookId)
        viewModel.startOrder(tableId, bookId) // ★ 注文ヘッダーを作成
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // ★ 注文IDが取得できたら表示
                title = { Text("注文画面 (T: $tableId / O: ${currentOrderId ?: "..."})") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "テーブル一覧に戻る")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (menuStructure.isNotEmpty()) {
                    // メニュー構造をリスト表示
                    MenuContent(
                        menuStructure = menuStructure,
                        onMenuClicked = { menuItem ->
                            // ★★★ 修正箇所：ViewModelのaddOrderItemを呼び出す ★★★
                            viewModel.addOrderItem(menuItem)
                        }
                    )
                } else if (errorMessage != null) {
                    // エラー表示
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "不明なエラー", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // ローディング表示
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("メニューを読み込み中...", modifier = Modifier.padding(top = 70.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun MenuContent(
    menuStructure: MenuStructure,
    // ★ クリックイベントの型を変更
    onMenuClicked: (MenuItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // ... (forEach ループは変更なし) ...
        menuStructure.forEach { (majorCategory, minorMap) ->
            item {
                Text(
                    text = majorCategory,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            minorMap.forEach { (minorCategory, menuItems) ->
                item {
                    Text(
                        text = minorCategory,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                // 3. メニューアイテムのリスト
                items(menuItems) { menuItem ->
                    MenuItemRow(menuItem = menuItem, onMenuClicked = {
                        // ★★★ 修正箇所：ViewModelにイベントを渡す ★★★
                        onMenuClicked(menuItem)
                    })
                }
            }
        }
    }
}

// (MenuItemRow は変更なし)
@Composable
fun MenuItemRow(menuItem: MenuItem, onMenuClicked: (MenuItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            .clickable { onMenuClicked(menuItem) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = menuItem.menuName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "¥${menuItem.price}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}