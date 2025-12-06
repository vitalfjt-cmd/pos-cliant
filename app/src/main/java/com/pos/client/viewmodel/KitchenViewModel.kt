package com.pos.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.client.data.model.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class KitchenItem(
    val detailId: Int,
    val orderId: Int,
    val menuName: String,
    val quantity: Int,
    val status: String,
    val tableNumber: String
)

class KitchenViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _kdsItems = MutableStateFlow<List<KitchenItem>>(emptyList())
    val kdsItems: StateFlow<List<KitchenItem>> = _kdsItems.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                fetchItems()
                delay(5000)
            }
        }
    }

    fun fetchItems() {
        viewModelScope.launch {
            try {
                val response = apiService.getKdsItems()
                if (response.isSuccessful) {
                    val rawList = response.body() ?: emptyList()

                    val items = rawList.map { map ->
                        try {
                            // ★修正: キー名のゆらぎに対応 (スネークケース優先、なければキャメルケース)
                            // ID
                            val idObj = map["detail_id"] ?: map["detailId"] ?: map["id"]
                            // Double型で来る場合があるので安全に変換
                            val detailId = (idObj as? Double)?.toInt() ?: (idObj as? Int) ?: -1 // IDがなくても -1 として表示させる

                            // OrderID
                            val orderIdObj = map["order_id"] ?: map["orderId"]
                            val orderId = (orderIdObj as? Double)?.toInt() ?: (orderIdObj as? Int) ?: 0

                            // MenuName
                            val menuName = (map["menu_name"] ?: map["menuName"] ?: "名称不明").toString()

                            // Quantity
                            val qtyObj = map["quantity"]
                            val quantity = (qtyObj as? Double)?.toInt() ?: (qtyObj as? Int) ?: 1

                            // Status
                            val status = (map["item_status"] ?: map["itemStatus"] ?: map["status"] ?: "状態不明").toString()

                            // Table
                            val tableNum = (map["table_number"] ?: map["tableNumber"] ?: "-").toString()

                            KitchenItem(
                                detailId = detailId,
                                orderId = orderId,
                                menuName = menuName,
                                quantity = quantity,
                                status = status,
                                tableNumber = tableNum
                            )
                        } catch (e: Exception) {
                            println("KitchenDebug: Parse Error ${e.message}")
                            // エラーがあってもダミーを表示して気づけるようにする
                            KitchenItem(-99, 0, "パースエラー", 1, "Error", "-")
                        }
                    }
                    _kdsItems.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー: ${e.message}"
            }
        }
    }

    fun updateStatus(detailId: Int, newStatus: String) {
        if (detailId < 0) return // ダミーIDの場合は更新しない

        viewModelScope.launch {
            try {
                val response = apiService.updateKdsStatus(detailId, newStatus)
                if (response.isSuccessful) {
                    fetchItems()
                } else {
                    _errorMessage.value = "ステータス更新失敗"
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー"
            }
        }
    }
}