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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel,
    tableId: Int,
    bookId: Int,
    existingOrderId: Int?,
    customerCount: Int, // ★追加: 人数を受け取る
    onBackClicked: () -> Unit
) {
    val menuStructure by viewModel.menuStructure.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val orderHistory by viewModel.orderHistory.collectAsState()
    val options by viewModel.options.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // ダイアログフラグ
    var showItemDetailDialog by remember { mutableStateOf<MenuItem?>(null) }
    var showCartDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showBillDialog by remember { mutableStateOf(false) }
    // ... (既存の状態取得) ...
    val isAccountingCompleted by viewModel.isAccountingCompleted.collectAsState() // ★追加

    LaunchedEffect(tableId) {
        // ★修正: 人数もViewModelに渡す
        viewModel.initializeOrder(tableId, bookId, existingOrderId, customerCount)
//        viewModel.initializeOrder(tableId, bookId, existingOrderId)
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    // ★追加: 会計完了を監視して画面を戻す
    LaunchedEffect(isAccountingCompleted) {
        if (isAccountingCompleted) {
            // 少し待ってから戻るとメッセージが読める（お好みで）
            kotlinx.coroutines.delay(1500)
            viewModel.clearAccountingStatus()
            onBackClicked() // テーブル一覧に戻る
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
                        showBillDialog = true
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Receiptアイコンの代用としてEmailを使用
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
                    val majorCategories = menuStructure.keys.toList()
                    if (selectedCategoryIndex >= majorCategories.size) selectedCategoryIndex = 0

                    ScrollableTabRow(
                        selectedTabIndex = selectedCategoryIndex,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(
                                    tabPositions[selectedCategoryIndex]
                                )
                            )
                        }
                    ) {
                        majorCategories.forEachIndexed { index, category ->
                            Tab(
                                selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index },
                                text = { Text(category) }
                            )
                        }
                    }

                    val currentMajor = majorCategories[selectedCategoryIndex]
                    val currentMinorMap = menuStructure[currentMajor] ?: emptyMap()

                    MenuContent(
                        minorMap = currentMinorMap,
                        onMenuClicked = { showItemDetailDialog = it })
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

    // --- Dialogs ---

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

//    if (showBillDialog) {
//        BillDialog(orderHistory) { showBillDialog = false }
//    }
    if (showBillDialog) {
        BillDialog(
            orderHistory = orderHistory,
            onPay = { paymentId, amount ->
                viewModel.completeAccounting(paymentId, amount)
                showBillDialog = false // ダイアログは閉じる（その後画面遷移）
            },
            onDismiss = { showBillDialog = false }
        )
    }
}

// --- Components ---

@Composable
fun MenuContent(minorMap: Map<String, List<MenuItem>>, onMenuClicked: (MenuItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        minorMap.forEach { (minor, items) ->
            item {
                Text(minor, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(16.dp))
            }
            items(items) { item ->
                MenuItemRow(item, onMenuClicked)
            }
        }
    }
}

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
                        // ★修正: Iconの代わりにCheckboxを使用（エラー解消）
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null // Rowのclickableで制御するためnull
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

// ★修正: 支払いボタンを追加した会計ダイアログ
@Composable
fun BillDialog(
    orderHistory: AccountingResponse?,
    onPay: (Int, Int) -> Unit, // paymentId, amount
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("お会計", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))

                val total = orderHistory?.header?.totalAmount ?: 0
                Text("合計金額", style = MaterialTheme.typography.titleMedium)
                Text("¥$total", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(32.dp))

                if (total > 0) {
                    Text("支払い方法を選択", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 現金払い (PaymentID: 1)
                    Button(
                        onClick = { onPay(1, total) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null) // 適当なアイコン（現金）
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("現金で支払う")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // カード払い (PaymentID: 2)
                    Button(
                        onClick = { onPay(2, total) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null) // 適当なアイコン（カード）
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("カードで支払う")
                    }
                } else {
                    Text("※請求額が0円のため会計できません", color = Color.Red)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("閉じる") }
            }
        }
    }
}
//@Composable
//fun BillDialog(history: AccountingResponse?, onDismiss: () -> Unit) {
//    Dialog(onDismissRequest = onDismiss) {
//        Card(modifier = Modifier.fillMaxWidth()) {
//            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//                Text("お会計", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
//                Spacer(modifier = Modifier.height(32.dp))
//
//                val total = history?.header?.totalAmount ?: 0
//                Text("合計金額", style = MaterialTheme.typography.titleMedium)
//                Text("¥$total", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
//
//                Spacer(modifier = Modifier.height(32.dp))
//                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("閉じる") }
//            }
//        }
//    }
//}