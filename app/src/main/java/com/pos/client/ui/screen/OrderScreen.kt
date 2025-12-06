package com.pos.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pos.client.data.model.MenuItem
import com.pos.client.viewmodel.OrderViewModel
import com.pos.client.ui.dialog.ItemDetailDialog
import com.pos.client.ui.dialog.CartDialog
import com.pos.client.ui.dialog.HistoryListDialog
import kotlinx.coroutines.delay
import coil.compose.AsyncImage


// ★ここに追加：UI専用のデータクラス（MenuItemは既存のものを使うので定義しない）
data class UiMenuCategory(
    val id: String,
    val name: String,
    val subCategories: List<UiMenuSubCategory>
)

data class UiMenuSubCategory(
    val id: String,
    val name: String,
    val items: List<MenuItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    tableId: Int,
    bookId: Int,
    existingOrderId: Int?,
    customerCount: Int,
    onBackClicked: () -> Unit,
    onAccountingClicked: () -> Unit
) {
    // ViewModelのデータ構造は Map<String, Map<String, List<MenuItem>>> と想定
    val menuStructure by viewModel.menuStructure.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val orderHistory by viewModel.orderHistory.collectAsState()
    val options by viewModel.options.collectAsState()
    val isAccountingCompleted by viewModel.isAccountingCompleted.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // ダイアログフラグ
    var showItemDetailDialog by remember { mutableStateOf<MenuItem?>(null) }
    var showCartDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // ★追加: 分割完了イベントの監視
    val splitCompletedOrderId by viewModel.splitCompletedEvent.collectAsState()
    // ★追加: 現在の注文IDを監視
    val currentOrderId by viewModel.currentOrderId.collectAsState()

    LaunchedEffect(tableId) {
        viewModel.initializeOrder(tableId, bookId, existingOrderId, customerCount)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(isAccountingCompleted) {
        if (isAccountingCompleted) {
            delay(1500)
            viewModel.clearAccountingStatus()
            onBackClicked()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("注文 (Table: $tableId / ID: ${currentOrderId ?: "未生成"})") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        viewModel.fetchOrderHistory()
                        showHistoryDialog = true
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                            Text("注文履歴")
                        }
                    }

                    TextButton(onClick = {
                        viewModel.fetchOrderHistory()
                        onAccountingClicked()
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Text("お会計")
                        }
                    }

                    TextButton(onClick = { }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Text("店員呼出")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (cartItems.isNotEmpty()) {
                BadgedBox(badge = {
                    Badge { Text(cartItems.sumOf { it.quantity }.toString()) }
                }) {
                    FloatingActionButton(
                        onClick = { showCartDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "カート",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        content = { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (menuStructure.isNotEmpty()) {
                    val uiCategories = remember(menuStructure) {
                        menuStructure.entries.map { (majorName, minorMap) ->
                            UiMenuCategory(
                                id = majorName,
                                name = majorName,
                                subCategories = minorMap.entries.map { (minorName, items) ->
                                    UiMenuSubCategory(
                                        id = minorName,
                                        name = minorName,
                                        items = items
                                    )
                                }
                            )
                        }
                    }

                    // ★修正ポイント: 古いコードを削除し、新しい2階層タブコンポーネントを呼び出す
                    OrderScreenTabs(
                        categories = uiCategories,
                        onItemClick = { showItemDetailDialog = it }
                    )

                } else if (errorMessage != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("読込中...", modifier = Modifier.padding(top = 48.dp))
                    }
                }
            }
        }
    )

    // --- Dialogs (変更なし) ---
    showItemDetailDialog?.let { menuItem ->
        ItemDetailDialog(
            menuItem = menuItem,
            allOptions = options,
            onDismiss = { showItemDetailDialog = null },
            onAddToCart = { qty, selectedOpts ->
                viewModel.addToCart(menuItem, qty, selectedOpts)
                showItemDetailDialog = null
            }
        )
    }

    if (showCartDialog) {
        CartDialog(cartItems, { showCartDialog = false }, { viewModel.removeFromCart(it) }, {
            viewModel.submitOrder()
            showCartDialog = false
        })
    }

    if (showHistoryDialog) {
        HistoryListDialog(orderHistory, viewModel) { showHistoryDialog = false }
    }
}

// Helper: 画像のフルパスを作る (http://192.168.x.x:8080/images/...)
fun getFullImageUrl(path: String?): String? {
    if (path.isNullOrEmpty()) return null
    // RetrofitClient.BASE_URL は "http://.../api/" となっている場合があるので調整が必要
    // ここでは簡易的にハードコードか、BASE_URLから "api/" を除いて結合する処理を書きます
    val baseUrl = "http://192.168.45.2:8080" // ★環境に合わせて変更
    return if (path.startsWith("http")) path else "$baseUrl$path"
}

// --- Components ---

// ★追加した新しい2階層タブコンポーネント
@Composable
fun OrderScreenTabs(
    categories: List<UiMenuCategory>,
    onItemClick: (MenuItem) -> Unit
) {
    // 選択状態の管理
    var selectedParentIndex by remember { mutableIntStateOf(0) }
    var selectedChildIndex by remember { mutableIntStateOf(0) }

    // インデックスの範囲チェック（データ更新時などに範囲外になるのを防ぐ）
    val safeParentIndex = selectedParentIndex.coerceIn(0, (categories.size - 1).coerceAtLeast(0))
    val currentParentCategory = categories.getOrNull(safeParentIndex)

    val currentSubCategories = currentParentCategory?.subCategories ?: emptyList()

    // 子インデックスもリセット処理が入るが、描画時の範囲チェックも行う
    val safeChildIndex = selectedChildIndex.coerceIn(0, (currentSubCategories.size - 1).coerceAtLeast(0))

    // 【重要】親タブが切り替わったら、子タブの選択を「一番左(0)」に戻す
    LaunchedEffect(safeParentIndex) {
        selectedChildIndex = 0
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // --- 1段目: 大分類 (親タブ) ---
        ScrollableTabRow(
            selectedTabIndex = safeParentIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = safeParentIndex == index,
                    onClick = { selectedParentIndex = index },
                    text = { Text(text = category.name, style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        // --- 2段目: 小分類 (子タブ) ---
        if (currentSubCategories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = safeChildIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                currentSubCategories.forEachIndexed { index, subCategory ->
                    Tab(
                        selected = safeChildIndex == index,
                        onClick = { selectedChildIndex = index },
                        text = { Text(text = subCategory.name) }
                    )
                }
            }
        }

        // --- 3段目: 商品リスト (コンテンツ) ---
        val currentItems = currentSubCategories.getOrNull(safeChildIndex)?.items ?: emptyList()

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentItems) { item ->
                MenuItemRow(menuItem = item, onClick = onItemClick)
            }
            // 下部のスペース（FABやボトムバー用）
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// 既存のコンポーネント (MenuItemRowなど) はそのまま利用
@Composable
fun MenuItemRow(menuItem: MenuItem, onClick: (MenuItem) -> Unit) {
    val cardColor = if (menuItem.isSoldOut) Color.LightGray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val contentColor = if (menuItem.isSoldOut) Color.Gray else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clickable(enabled = !menuItem.isSoldOut) { onClick(menuItem) },
        elevation = CardDefaults.cardElevation(if (menuItem.isSoldOut) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .padding(12.dp) // 少しパディング調整
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ★追加: 画像表示エリア
                if (menuItem.imageUrl != null) {
                    AsyncImage(
                        model = getFullImageUrl(menuItem.imageUrl),
                        contentDescription = menuItem.menuName,
                        modifier = Modifier
                            .size(64.dp) // 正方形
                            .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                } else {
                    // 画像がない場合のダミー（必要なら）
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Image", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // テキスト情報
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = menuItem.menuName,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        style = if (menuItem.isSoldOut) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                    )
                }

                Text(
                    text = "¥${menuItem.price}",
                    color = if (menuItem.isSoldOut) contentColor else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 売り切れ時のオーバーレイ表示（スタンプ風）
            if (menuItem.isSoldOut) {
                Text(
                    text = "SOLD OUT",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .rotate(-15f) // 少し斜めにする
                )
            }
        }
    }
}
