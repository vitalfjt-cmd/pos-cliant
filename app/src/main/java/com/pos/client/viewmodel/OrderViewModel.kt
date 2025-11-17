package com.pos.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.client.data.model.ApiService
import com.pos.client.data.model.MenuBook
import com.pos.client.data.model.MenuItem
import com.pos.client.data.model.OrderDetail
import com.pos.client.data.model.OrderHeaderRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// (MenuStructure の定義は変更なし)
typealias MenuStructure = Map<String, Map<String, List<MenuItem>>>

// APIサービスをコンストラクタで受け取る
class OrderViewModel(
    private val apiService: ApiService
) : ViewModel() {

    // (StateFlow の定義は変更なし)
    private val _menuStructure = MutableStateFlow<MenuStructure>(emptyMap())
    val menuStructure: StateFlow<MenuStructure> = _menuStructure.asStateFlow()

    private val _currentOrderId = MutableStateFlow<Int?>(null)
    val currentOrderId: StateFlow<Int?> = _currentOrderId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // (loadMenuStructure は変更なし)
    fun loadMenuStructure(bookId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getMenuStructure(bookId)
                if (response.isSuccessful) {
                    _menuStructure.value = response.body() ?: emptyMap()
                } else {
                    _errorMessage.value = "メニューの取得に失敗しました"
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー: ${e.message}"
            }
        }
    }

    // ★★★ 修正箇所：startOrder にデバッグログを追加 ★★★
    fun startOrder(tableId: Int, bookId: Int) {
        if (_currentOrderId.value != null) return

        viewModelScope.launch {
            val orderRequest = OrderHeaderRequest(
                tableId = tableId,
                bookId = bookId,
                customerCount = 1
            )

            try {
                val response = apiService.startOrder(orderRequest)
                if (response.isSuccessful) {
                    _currentOrderId.value = response.body()?.orderId
                    println("Order started. ID: ${response.body()?.orderId}") // ★ デバッグログ
                } else {
                    _errorMessage.value = "注文の開始に失敗しました: ${response.code()}"
                    println("startOrder Error: ${response.code()} ${response.message()}") // ★ デバッグログ
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー: ${e.message}"
                println("startOrder Network Error: ${e.message}") // ★ デバッグログ
            }
        }
    }

    // ★★★ 修正箇所：addOrderItem にデバッグログを追加 ★★★
    fun addOrderItem(menuItem: MenuItem) {
        val orderId = _currentOrderId.value
        if (orderId == null) {
            _errorMessage.value = "注文が開始されていません。もう一度お試しください。"
            println("addOrderItem Error: currentOrderId is null") // ★ デバッグログ
            return
        }

        viewModelScope.launch {
            val orderDetail = OrderDetail(
                orderId = orderId,
                menuId = menuItem.menuId,
                quantity = 1
            )

            try {
                val response = apiService.addOrderItem(orderDetail)
                if (response.isSuccessful) {
                    println("Item added: ${response.body()?.message}") // ★ デバッグログ
                } else {
                    _errorMessage.value = "注文の追加に失敗しました: ${response.code()}"
                    println("addOrderItem Error: ${response.code()} ${response.message()}") // ★ デバッグログ
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー: ${e.message}"
                println("addOrderItem Network Error: ${e.message}") // ★ デバッグログ
            }
        }
    }
}