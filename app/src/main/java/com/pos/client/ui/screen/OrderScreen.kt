package com.pos.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pos.client.data.model.MenuItem
import com.pos.client.data.model.CartItem
import com.pos.client.data.model.AccountingResponse
import com.pos.client.data.model.OptionItem
import com.pos.client.viewmodel.OrderViewModel
import kotlinx.coroutines.delay

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
                title = { Text("注文 (Table: $tableId)") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
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
                            Icon(Icons.Default.List, contentDescription = null)
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
                    // ★ここが修正ポイント: Mapデータを新しいUI用のListデータに変換
                    // Map<Major, Map<Minor, List<MenuItem>>> -> List<UiMenuCategory>
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick(menuItem) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(menuItem.menuName, fontWeight = FontWeight.Bold)
            Text("¥${menuItem.price}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun ItemDetailDialog(
    menuItem: MenuItem,
    allOptions: List<OptionItem>,
    onDismiss: () -> Unit,
    onAddToCart: (Int, List<OptionItem>) -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    val selectedOptions = remember { mutableStateListOf<OptionItem>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(menuItem.menuName) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("価格: ¥${menuItem.price}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Text("-", style = MaterialTheme.typography.headlineSmall)
                    }
                    Text(quantity.toString(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
                    IconButton(onClick = { quantity++ }, modifier = Modifier.background(Color.LightGray, CircleShape)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Text("オプション選択", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))

                allOptions.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedOptions.remove(option)
                                else selectedOptions.add(option)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.optionName)
                        Spacer(modifier = Modifier.weight(1f))
                        if (option.price > 0) {
                            Text("+¥${option.price}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddToCart(quantity, selectedOptions.toList())
            }) {
                Text("カートに入れる")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

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
                                IconButton(onClick = { onRemoveItem(item) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            }
                            Divider()
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

@Composable
fun HistoryListDialog(history: AccountingResponse?, viewModel: OrderViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("注文履歴", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (history?.details.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("注文履歴はありません", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(history!!.details!!) { detail ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(viewModel.getMenuName(detail.menuId))
                                    Text("状態: ${detail.itemStatus}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text("${detail.quantity}点")
                            }
                            Divider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("閉じる") }
            }
        }
    }
}
