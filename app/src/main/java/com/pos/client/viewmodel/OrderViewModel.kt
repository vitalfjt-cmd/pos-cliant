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

    // ★修正: customerCount (人数) を引数に追加
    fun initializeOrder(tableId: Int, bookId: Int, existingOrderId: Int?, customerCount: Int) {
        // 状態リセット（テーブルが変わった場合のみ、または強制的に）
        // 今回はシンプルに毎回リセットして整合性を保つ
        _currentTableId = tableId
        _currentOrderId.value = null
        _cartItems.value = emptyList()
        _orderHistory.value = null

        loadMenuStructure(bookId)

        if (existingOrderId != null && existingOrderId > 0) {
            // 既存の注文がある場合（再開）
            _currentOrderId.value = existingOrderId
            println("Resuming Order ID: $existingOrderId")
            fetchOrderHistory()
        } else {
            // 新規注文の場合（入店）
            // ★修正: 人数を渡して注文開始
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

    // ★修正: customerCountを受け取るように変更
    private fun startOrder(tableId: Int, bookId: Int, customerCount: Int) {
        viewModelScope.launch {
            try {
                // Requestに人数をセットして送信
                val request = OrderHeaderRequest(tableId, bookId, customerCount)
                val response = apiService.startOrder(request)

                if (response.isSuccessful) {
                    _currentOrderId.value = response.body()?.orderId
                    println("New Order Started: ${response.body()?.orderId}")
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

    fun fetchOrderHistory() {
        val orderId = _currentOrderId.value ?: return
        viewModelScope.launch {
            try {
                val res = apiService.getAccountingDetails(orderId)
                if (res.isSuccessful) {
                    _orderHistory.value = res.body()
                }
            } catch (e: Exception) {
                _errorMessage.value = "履歴取得失敗"
            }
        }
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

    fun getMenuName(menuId: Int): String = _menuMap[menuId]?.menuName ?: "商品ID:$menuId"
    fun getMenuPrice(menuId: Int): Int = _menuMap[menuId]?.price ?: 0

    fun addOrderItem(menuItem: MenuItem) { addToCart(menuItem, 1, emptyList()) }
}