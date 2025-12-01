package com.pos.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.client.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias MenuStructure = Map<String, Map<String, List<MenuItem>>>

class OrderViewModel(
    private val apiService: ApiService
) : ViewModel() {

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

    fun clearUserMessage() { _userMessage.value = null }
    fun clearAccountingStatus() { _isAccountingCompleted.value = false }

    fun initializeOrder(tableId: Int, bookId: Int, existingOrderId: Int?, customerCount: Int) {
        _currentTableId = tableId
        _currentOrderId.value = null
        _cartItems.value = emptyList()
        _orderHistory.value = null

        loadMenuStructure(bookId)

        if (existingOrderId != null && existingOrderId > 0) {
            _currentOrderId.value = existingOrderId
            fetchOrderHistory()
        } else {
            startOrder(tableId, bookId, customerCount)
        }
    }

    private fun loadMenuStructure(bookId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getMenuStructure(bookId)
                if (response.isSuccessful) {
                    val structure = response.body() ?: emptyMap()
                    _menuStructure.value = structure
                    _menuMap.clear()
                    structure.values.forEach { minorMap ->
                        minorMap.values.forEach { list ->
                            list.forEach { item -> _menuMap[item.menuId] = item }
                        }
                    }
                }
                val optRes = apiService.getOptions()
                if (optRes.isSuccessful) {
                    _options.value = optRes.body() ?: emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "メニュー読込エラー: ${e.message}"
            }
        }
    }

    private fun startOrder(tableId: Int, bookId: Int, customerCount: Int) {
        viewModelScope.launch {
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

    fun submitOrder() {
        val orderId = _currentOrderId.value ?: return
        val items = _cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            var successCount = 0
            items.forEach { item ->
                try {
                    val detail = OrderDetail(
                        orderId = orderId,
                        menuId = item.menuItem.menuId,
                        quantity = item.quantity,
                        optionIds = item.selectedOptions.map { it.optionId }
                    )
                    val res = apiService.addOrderItem(detail)
                    if (res.isSuccessful) successCount++
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (successCount == items.size) {
                _userMessage.value = "全ての注文を承りました"
                _cartItems.value = emptyList()
                fetchOrderHistory()
            } else {
                _errorMessage.value = "一部注文失敗 ($successCount/${items.size})"
                _cartItems.value = emptyList()
            }
        }
    }

    // ★修正: 引数でIDを指定できるように変更
    fun fetchOrderHistory(targetOrderId: Int? = null) {
        val orderId = targetOrderId ?: _currentOrderId.value
        if (orderId == null) return

        viewModelScope.launch {
            try {
                val res = apiService.getAccountingDetails(orderId)
                if (res.isSuccessful) {
                    _orderHistory.value = res.body()
                    if (targetOrderId != null) {
                        _currentOrderId.value = targetOrderId
                    }
                } else {
                    _errorMessage.value = "伝票情報が見つかりません"
                    _orderHistory.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "履歴取得失敗: ${e.message}"
            }
        }
    }

    // ★追加: テーブル番号から伝票を検索
    fun searchOrderByTable(tableId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getTableStatuses()
                if (response.isSuccessful) {
                    val tables = response.body() ?: emptyList()
                    val targetTable = tables.find { it.tableId == tableId }

                    if (targetTable != null) {
                        if (targetTable.orderId != null && targetTable.orderId > 0) {
                            _userMessage.value = "テーブル $tableId の伝票を呼び出します"
                            fetchOrderHistory(targetTable.orderId)
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

    // ★追加: 伝票番号から検索
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

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun getMenuName(menuId: Int): String = _menuMap[menuId]?.menuName ?: "商品ID:$menuId"
    fun getMenuPrice(menuId: Int): Int = _menuMap[menuId]?.price ?: 0
    fun addOrderItem(menuItem: MenuItem) { addToCart(menuItem, 1, emptyList()) }
}