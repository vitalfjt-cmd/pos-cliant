package com.pos.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.client.api.OrderApi
import com.pos.client.data.model.*
import com.pos.client.data.repository.OrderRepository
import com.pos.client.di.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// MenuStructureの型エイリアス
typealias MenuStructure = Map<String, Map<String, List<MenuItem>>>

class OrderViewModel : ViewModel() {

    // Repositoryの初期化 (DIライブラリを使わない簡易的な方法)
    // OrderApiインターフェースの実体はRetrofitClientから取得
    private val apiService: OrderApi = RetrofitClient.instance
    private val repository = OrderRepository(apiService)

    private val _menuStructure = MutableStateFlow<MenuStructure>(emptyMap())
    val menuStructure: StateFlow<MenuStructure> = _menuStructure.asStateFlow()

    private val _currentOrderId = MutableStateFlow<Int?>(null)
    val currentOrderId: StateFlow<Int?> = _currentOrderId.asStateFlow()

    private var _currentTableId: Int? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _orderHistory = MutableStateFlow<AccountingResponse?>(null)
    val orderHistory: StateFlow<AccountingResponse?> = _orderHistory.asStateFlow()

    private val _menuMap = mutableMapOf<Int, MenuItem>()

    private val _options = MutableStateFlow<List<OptionItem>>(emptyList())
    val options: StateFlow<List<OptionItem>> = _options.asStateFlow()

    private val _isAccountingCompleted = MutableStateFlow(false)
    val isAccountingCompleted: StateFlow<Boolean> = _isAccountingCompleted.asStateFlow()


// ■修正1: ensureMenuMapLoaded は launch で囲む
    fun ensureMenuMapLoaded() {
        if (_menuMap.isEmpty()) {
            viewModelScope.launch { loadDefaultMenu() }
        }
    }

    // ■修正2: suspend関数に変更 (launchを削除)
    private suspend fun loadDefaultMenu() {
        try {
            val response = apiService.getMenuBooks()
            if (response.isSuccessful) {
                val books = response.body()
                if (!books.isNullOrEmpty()) {
                    val firstBookId = books.first().bookId
                    loadMenuStructure(firstBookId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 分割完了通知
    private val _splitCompletedEvent = MutableStateFlow<Int?>(null)
    val splitCompletedEvent: StateFlow<Int?> = _splitCompletedEvent.asStateFlow()

    fun clearSplitEvent() {
        _splitCompletedEvent.value = null
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearAccountingStatus() {
        _isAccountingCompleted.value = false
    }

    // ★今回の追加: 分割実行ロジック
    fun executeSplitOrder(sourceOrderId: Int, selectedDetailIds: List<Int>) {
        viewModelScope.launch {
            try {
                val result = repository.splitOrder(sourceOrderId, selectedDetailIds)
                if (result != null) {
                    _userMessage.value = result.message
                    _splitCompletedEvent.value = result.newOrderId

                    // 必要に応じて履歴を再取得
                    // fetchOrderHistory()
                } else {
                    _userMessage.value = "分割に失敗しました"
                }
            } catch (e: Exception) {
                _userMessage.value = "エラー: ${e.message}"
            }
        }
    }

    // ■修正3: 初期化処理を「直列実行」に変更して安全性を確保
    fun initializeOrder(tableId: Int, bookId: Int, existingOrderId: Int?, customerCount: Int) {
        _currentTableId = tableId

        viewModelScope.launch {
            // ローディング開始（メニューを空にすると画面側でスピナーが出る）
            _menuStructure.value = emptyMap()
            _currentOrderId.value = null
            _cartItems.value = emptyList()
            _orderHistory.value = null

            // 1. まず注文IDを確保する (ここが終わるまで待機)
            if (existingOrderId != null && existingOrderId > 0) {
                _currentOrderId.value = existingOrderId
                fetchOrderHistorySuspend(existingOrderId) // 下で作るsuspend版を呼ぶ
            } else {
                startOrderSuspend(tableId, bookId, customerCount) // 下で作るsuspend版を呼ぶ
            }

            // 2. ID確保後にメニューを読み込む
            // これにより「メニューが表示された＝IDがある」ことが保証される
            loadMenuStructure(bookId)
        }
    }

    // ■修正4: suspend関数に変更 (launchを削除)
    private suspend fun loadMenuStructure(bookId: Int) {
        try {
            val response = apiService.getMenuStructure(bookId)
            if (response.isSuccessful) {
                val structure = response.body() ?: emptyMap()

                // メニューマップ作成
                _menuMap.clear()
                structure.values.forEach { minorMap ->
                    minorMap.values.forEach { list ->
                        list.forEach { item -> _menuMap[item.menuId] = item }
                    }
                }

                // ★ここで初めてUIを表示させる
                _menuStructure.value = structure
            }
            val optRes = apiService.getOptions()
            if (optRes.isSuccessful) {
                _options.value = optRes.body() ?: emptyList()
            }
        } catch (e: Exception) {
            _errorMessage.value = "メニュー読込エラー: ${e.message}"
        }
    }

    // ■修正5: startOrderを分離し、suspend関数に変更
    private suspend fun startOrderSuspend(tableId: Int, bookId: Int, customerCount: Int) {
        try {
            val request = OrderHeaderRequest(tableId, bookId, customerCount)
            val response = apiService.startOrder(request)
            if (response.isSuccessful) {
                _currentOrderId.value = response.body()?.orderId
            } else {
                _errorMessage.value = "注文開始失敗"
            }
        } catch (e: Exception) {
            _errorMessage.value = "通信エラー"
        }
    }

    fun addToCart(menuItem: MenuItem, quantity: Int, selectedOptions: List<OptionItem>) {
        val currentList = _cartItems.value.toMutableList()
        currentList.add(CartItem(menuItem, quantity, selectedOptions))
        _cartItems.value = currentList
        _userMessage.value = "${menuItem.menuName} をカートに追加"
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentList = _cartItems.value.toMutableList()
        currentList.remove(cartItem)
        _cartItems.value = currentList
    }
    // submitOrder メソッド全体を以下のように修正してください
    fun submitOrder() {
        val orderId = _currentOrderId.value ?: return
        val items = _cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            // エラー捕捉用のフラグ
            var isError = false

            items.forEach { item ->
                try {
                    val detail = OrderDetail(
                        detailId = null, // 新規登録
                        orderId = orderId,
                        menuId = item.menuItem.menuId,
                        quantity = item.quantity,
                        priceAtOrder = item.menuItem.price,
                        subtotal = item.menuItem.price * item.quantity,
                        itemStatus = "未調理",
                        optionIds = item.selectedOptions.map { it.optionId },
                        optionsText = item.selectedOptions.joinToString(",") { it.optionName }
                    )

                    val res = apiService.addOrderItem(detail)
                    if (!res.isSuccessful) {
                        isError = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    isError = true
                }
            }

            if (!isError) {
                _userMessage.value = "注文を承りました"
                _cartItems.value = emptyList()
                fetchOrderHistory()
            } else {
                _userMessage.value = "一部の注文送信に失敗しました。店員にお知らせください。"
                // カートはクリアせず、再試行できるように残す
            }
        }
    }

// ■修正6: fetchOrderHistoryもsuspend版を用意
    private suspend fun fetchOrderHistorySuspend(orderId: Int) {
        try {
            val res = apiService.getAccountingDetails(orderId)
            if (res.isSuccessful) {
                _orderHistory.value = res.body()
            } else {
                _errorMessage.value = "伝票情報が見つかりません"
            }
        } catch (e: Exception) {
            _errorMessage.value = "履歴取得失敗: ${e.message}"
        }
    }

    // 既存の fetchOrderHistory はボタン等から呼ばれるので残すが、中身は suspend版 を呼ぶように修正
    fun fetchOrderHistory(targetOrderId: Int? = null) {
        val orderId = targetOrderId ?: _currentOrderId.value ?: return
        viewModelScope.launch {
            fetchOrderHistorySuspend(orderId)
            if (targetOrderId != null) {
                _currentOrderId.value = targetOrderId
            }
        }
    }

    // テーブル番号から伝票を検索
    fun searchOrderByTable(tableId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getTableStatuses()
                if (response.isSuccessful) {
                    val tables = response.body() ?: emptyList()
                    val targetTable = tables.find { it.tableId == tableId }

                    if (targetTable != null) {
                        // 1. ローカル変数に入れる
                        val targetOrderId = targetTable.orderId

                        // 2. ★修正: ここで 'targetTable.orderId' ではなく 'targetOrderId' を使う
                        if (targetOrderId != null && targetOrderId > 0) {
                            _userMessage.value = "テーブル $tableId の伝票を呼び出します"
                            // 3. ★修正: ここも 'targetOrderId' を使う
                            fetchOrderHistory(targetOrderId)
                        } else {
                            _userMessage.value = "テーブル $tableId は空席または伝票がありません"
                            _orderHistory.value = null
                        }
                    } else {
                        _userMessage.value = "テーブル $tableId が存在しません"
                    }
                } else {
                    _errorMessage.value = "テーブル情報の取得に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "テーブル検索エラー: ${e.message}"
            }
        }
    }

    // 伝票番号から検索
    fun searchOrderBySlip(orderId: Int) {
        _userMessage.value = "伝票No $orderId を検索中..."
        fetchOrderHistory(orderId)
    }

    fun completeAccounting(paymentId: Int, amount: Int) {
        val orderId = _currentOrderId.value ?: return

        viewModelScope.launch {
            try {
                val request = AccountingRequest(
                    orderId = orderId,
                    paymentId = paymentId,
                    paymentAmount = amount
                )
                val response = apiService.completeAccounting(request)

                if (response.isSuccessful) {
                    _userMessage.value = "会計が完了しました"
                    _isAccountingCompleted.value = true
                    _currentOrderId.value = null
                    _cartItems.value = emptyList()
                    _orderHistory.value = null
                } else {
                    _errorMessage.value = "会計処理に失敗しました: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー: ${e.message}"
            }
        }
    }

    // viewmodel/OrderViewModel.kt の class OrderViewModel 内に追加

    // 伝票合算の実行
    fun executeMerge(sourceOrderId: Int, targetOrderId: Int) {
        if (sourceOrderId == targetOrderId) {
            _userMessage.value = "同じ伝票同士は合算できません"
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.mergeOrders(sourceOrderId, targetOrderId)
                if (success) {
                    _userMessage.value = "伝票No:$sourceOrderId を No:$targetOrderId に合算しました"
                    // 合算後の親伝票を再読み込みして表示更新
                    fetchOrderHistory(targetOrderId)
                } else {
                    _userMessage.value = "合算に失敗しました"
                }
            } catch (e: Exception) {
                _userMessage.value = "合算エラー: ${e.message}"
            }
        }
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun getMenuName(menuId: Int): String = _menuMap[menuId]?.menuName ?: "商品ID:$menuId"
    fun getMenuPrice(menuId: Int): Int = _menuMap[menuId]?.price ?: 0
    fun addOrderItem(menuItem: MenuItem) { addToCart(menuItem, 1, emptyList()) }
}