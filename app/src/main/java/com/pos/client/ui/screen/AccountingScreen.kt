package com.pos.client.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pos.client.viewmodel.OrderViewModel
import com.pos.client.ui.dialog.SplitBillDialog
import com.pos.client.ui.dialog.WarikanDialog
data class AccountingState(
    val deposit: Int = 0,
    val discountValue: Int = 0,
    val isPercent: Boolean = false,
    val currentInput: String = ""
) {
    fun calculateFinalTotal(originalTotal: Int): Int {
        if (originalTotal == 0) return 0
        val discountAmount = if (isPercent) {
            (originalTotal * discountValue / 100)
        } else {
            discountValue
        }
        return (originalTotal - discountAmount).coerceAtLeast(0)
    }

    fun calculateChange(originalTotal: Int): Int {
        val finalTotal = calculateFinalTotal(originalTotal)
        return if (deposit >= finalTotal) deposit - finalTotal else 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingScreen(
    viewModel: OrderViewModel,
    onBackClicked: () -> Unit
) {
    val orderHistory by viewModel.orderHistory.collectAsState()
    val isAccountingCompleted by viewModel.isAccountingCompleted.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var state by remember { mutableStateOf(AccountingState()) }
    val snackbarHostState = remember { SnackbarHostState() }

    // ★追加: 会計完了ダイアログ表示フラグ
    var showCompleteDialog by remember { mutableStateOf(false) }
    // ★追加: 会計完了後、データがクリアされても正しいお釣りを表示するために値を保持する
    var lastChange by remember { mutableIntStateOf(0) }

    val originalTotal = orderHistory?.header?.totalAmount ?: 0
    val finalTotal = state.calculateFinalTotal(originalTotal)
    val change = state.calculateChange(originalTotal)

    // ★追加: ダイアログ制御用
    var showSplitBillDialog by remember { mutableStateOf(false) }
    var showWarikanDialog by remember { mutableStateOf(false) }

    // ★追加: 分割完了監視
    val splitCompletedOrderId by viewModel.splitCompletedEvent.collectAsState()

    // ★追加: 分割が完了したら、新しい伝票番号を検索して再読み込み
    LaunchedEffect(splitCompletedOrderId) {
        splitCompletedOrderId?.let { newOrderId ->
            showSplitBillDialog = false
            viewModel.clearSplitEvent()
            // 新しい伝票（分割された方）をすぐに呼び出す場合
            viewModel.searchOrderBySlip(newOrderId)
            // または、トーストを出して元の伝票のままにするなら
            // viewModel.fetchOrderHistory()
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(isAccountingCompleted) {
        if (isAccountingCompleted) {
            showCompleteDialog = true
        }
    }

    // 画面表示時にデータ読み込みを確認する処理を追加
    LaunchedEffect(Unit) {
        viewModel.ensureMenuMapLoaded()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("会計処理 / 伝票呼出") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { paddingValues ->
            Row(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                // --- 左側：明細エリア ---
                Card(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(8.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text(
                            text = "伝票No: ${orderHistory?.header?.orderId ?: "未選択"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(orderHistory?.details ?: emptyList()) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(viewModel.getMenuName(item.menuId), fontWeight = FontWeight.Bold)
                                    }
                                    Text("x${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                                    Text("¥${item.subtotal}", fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CalculationRow(label = "小計", amount = originalTotal)
                            if (state.discountValue > 0) {
                                val label = if (state.isPercent) "値引 (${state.discountValue}%)" else "値引"
                                val discountAmount = originalTotal - finalTotal
                                CalculationRow(label = label, amount = -discountAmount, isRed = true)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("請求合計", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "¥$finalTotal",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            CalculationRow(label = "お預かり", amount = state.deposit, isBold = true)
                            CalculationRow(label = "お釣り", amount = change, isBold = true)
                        }
                    }
                }

                // --- 右側：操作パネル ---
                Column(
                    modifier = Modifier.weight(0.6f).padding(8.dp).fillMaxHeight()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(80.dp).padding(bottom = 8.dp),
                        color = Color.Black,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (state.currentInput.isEmpty()) "0" else state.currentInput,
                                color = Color.Green,
                                style = MaterialTheme.typography.displayMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FuncButton(
                                text = "呼出(卓)",
                                onClick = {
                                    // ★実装: テーブル番号で検索
                                    val tableId = state.currentInput.toIntOrNull()
                                    if (tableId != null) {
                                        viewModel.searchOrderByTable(tableId)
                                        state = state.copy(currentInput = "", deposit = 0)
                                    }
                                },
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            FuncButton(
                                text = "呼出(伝)",
                                onClick = {
                                    // ★実装: 伝票番号で検索
                                    val orderId = state.currentInput.toIntOrNull()
                                    if (orderId != null) {
                                        viewModel.searchOrderBySlip(orderId)
                                        state = state.copy(currentInput = "", deposit = 0)
                                    }
                                },
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            FuncButton(
                                text = "伝票合算",
                                onClick = {
                                    // 1. 親伝票（ターゲット）が表示されているか確認
                                    val targetOrderId = orderHistory?.header?.orderId

                                    // 2. テンキーに入力された子伝票（ソース）のIDを取得
                                    val sourceOrderId = state.currentInput.toIntOrNull()

                                    if (targetOrderId != null && sourceOrderId != null) {
                                        // 合算実行
                                        viewModel.executeMerge(sourceOrderId, targetOrderId)
                                        // 入力クリア
                                        state = state.copy(currentInput = "", deposit = 0)
                                    } else {
                                        // エラー表示（ViewModel経由が理想ですが、簡易的にトーストが出せないので今回は何もしないか、ログ出す）
                                        // 実際は viewModel.showUserMessage("合算する伝票番号を入力してください") などとしたい
                                    }
                                },
                                color = Color(0xFFCE93D8),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            FuncButton(
                                text = "ダミー",
                                onClick = { /* 実装済みなら */ },
                                color = Color(0xFFCE93D8),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            // ★追加: 個別会計（明細分割）
                            FuncButton(
                                text = "個別会計",
                                onClick = { showSplitBillDialog = true },
                                color = Color(0xFFCE93D8),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            // ★追加: 単純割り勘
                            FuncButton(
                                text = "割り勘",
                                onClick = { showWarikanDialog = true },
                                color = Color(0xFFCE93D8),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FuncButton(
                                text = "値引(円)",
                                onClick = {
                                    val value = state.currentInput.toIntOrNull() ?: 0
                                    state = state.copy(discountValue = value, isPercent = false, currentInput = "")
                                },
                                color = Color(0xFFFFCC80),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            FuncButton(
                                text = "割引(%)",
                                onClick = {
                                    val value = state.currentInput.toIntOrNull() ?: 0
                                    state = state.copy(discountValue = value, isPercent = true, currentInput = "")
                                },
                                color = Color(0xFFFFCC80),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                            FuncButton(
                                text = "C (クリア)",
                                onClick = { state = state.copy(currentInput = "", deposit = 0, discountValue = 0) },
                                color = Color(0xFFEF9A9A),
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(2f).fillMaxHeight()) {
                            TenKeyGrid(
                                onNumberClick = { num ->
                                    if (state.currentInput.length < 9) {
                                        state = state.copy(currentInput = state.currentInput + num)
                                    }
                                },
                                onDoubleZeroClick = {
                                    if (state.currentInput.isNotEmpty() && state.currentInput.length < 8) {
                                        state = state.copy(currentInput = state.currentInput + "00")
                                    }
                                },
                                onEnterClick = {
                                    // ★修正: ジャスト預かり対応
                                    if (state.currentInput.isEmpty()) {
                                        // 入力が空なら、請求額をそのまま預かり金としてセット（ジャスト預かり）
                                        state = state.copy(deposit = finalTotal, currentInput = "")
                                    } else {
                                        // 入力があればそれを預かり金とする
                                        val deposit = state.currentInput.toIntOrNull() ?: 0
                                        state = state.copy(deposit = deposit, currentInput = "")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("決済方法を選択", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        val methods = listOf(1 to "現金", 2 to "カード", 3 to "QR決済", 4 to "交通系", 5 to "金券", 6 to "ポイント")
                        items(methods) { (id, name) ->
                            PaymentButton(name) {
                                if (orderHistory != null) {
                                    // ★修正: 預かり金チェック
                                    // 現金(id=1)以外は預かり金不要とするなら、条件を `id == 1 && state.deposit < finalTotal` にする
                                    // ここではシンプルに「入力された預かり金が請求額未満ならNG」とする
                                    if (state.deposit < finalTotal) {
                                        // 既存の clearUserMessage などがあるため、viewModel経由でメッセージを表示させるのが良いですが、
                                        // 簡易的に SnackbarHostState に直接表示はできないため、ViewModelのStateを使うのが無難
                                        // ただし、ここではViewModelにメッセージ設定メソッドがないと仮定し、
                                        // 単に処理をブロックするだけにするか、ViewModelを修正します。
                                        // (OrderViewModelに _userMessage.value = "..." をセットするメソッドがあると良いです)

                                        // ※ViewModel側に `fun showUserMessage(msg: String)` を追加することを推奨します
                                        // ここではViewModelのコードを変えずに済むよう、リフレクション等無理なことはせず、
                                        // OrderViewModelにパブリックな `showUserMessage` がある前提で書くか、
                                        // 既存の `addToCart` などと同じ仕組みを利用します。
                                        // 今回は既存の `userMessage` が StateFlow なので、直接セットできません。
                                        //
                                        // ★OrderViewModelに以下のメソッドを追加してください:
                                        // fun showUserMessage(message: String) { _userMessage.value = message }
                                        // もし追加できない場合は、この画面独自でSnackbarを出す必要がありますが、
                                        // ここではViewModelに追加した前提で呼び出します。
                                        viewModel.showUserMessage("預かり金が不足しています")
                                    } else {
                                        // ★修正: 確定したお釣り額を保存してからAPIを呼ぶ
                                        lastChange = change
                                        viewModel.completeAccounting(paymentId = id, amount = finalTotal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // 会計完了ダイアログ
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = {}, // 外側タップで閉じない
            title = { Text("会計完了") },
            text = {
                Column {
                    Text("お会計が完了しました。", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
//                    Text("お釣り: \$change", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    // ★修正: 計算済みで保存しておいたお釣り(lastChange)を表示
                    Text("お釣り: ¥$lastChange", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        viewModel.clearAccountingStatus() // フラグを下ろす
                        state = AccountingState() // 状態リセット
                        onBackClicked() // 画面遷移
                    }
                ) {
                    Text("閉じる")
                }
            }
        )
    }

    // 個別会計ダイアログ
    if (showSplitBillDialog && orderHistory != null) {
        SplitBillDialog(
            orderHistory = orderHistory,
            viewModel = viewModel, // ★名前解決のためにViewModelを渡す
            onDismiss = { showSplitBillDialog = false },
            onExecuteSplit = { selectedIds ->
                orderHistory?.header?.orderId?.let { sourceId ->
                    viewModel.executeSplitOrder(sourceId, selectedIds)
                }
            }
        )
    }

    // 割り勘ダイアログ
    if (showWarikanDialog && orderHistory != null) {
        WarikanDialog(
            totalAmount = finalTotal, // 値引き後の金額
            onDismiss = { showWarikanDialog = false }
        )
    }
}

@Composable
fun CalculationRow(label: String, amount: Int, isRed: Boolean = false, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text("¥$amount", style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, color = if (isRed) Color.Red else Color.Unspecified, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun FuncButton(text: String, onClick: () -> Unit, color: Color = Color.LightGray, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = color), contentPadding = PaddingValues(0.dp)) {
        Text(text = text, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PaymentButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Text(text = text, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TenKeyGrid(onNumberClick: (String) -> Unit, onDoubleZeroClick: () -> Unit, onEnterClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        val rows = listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"))
        for (row in rows) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (num in row) {
                    TenKeyButton(text = num, onClick = { onNumberClick(num) })
                }
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TenKeyButton(text = "0", onClick = { onNumberClick("0") })
            TenKeyButton(text = "00", onClick = onDoubleZeroClick)
            Button(onClick = onEnterClick, modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text(text = "現計", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.TenKeyButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), elevation = ButtonDefaults.buttonElevation(2.dp)) {
        Text(text = text, fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
