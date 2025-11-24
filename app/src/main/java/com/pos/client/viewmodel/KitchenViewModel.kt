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

// 表示用のデータクラス
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
                delay(5000) // 5秒ごとに更新
            }
        }
    }

    fun fetchItems() {
        viewModelScope.launch {
            try {
                val response = apiService.getKdsItems()
                if (response.isSuccessful) {
                    val rawList = response.body() ?: emptyList()
                    // MapからKitchenItemへ変換
                    val items = rawList.mapNotNull { map ->
                        try {
                            // サーバーのSQLの列名に合わせて取得
                            // SELECT d.order_id, d.item_status, d.quantity, m.menu_name, t.table_number ...
                            // ※ detail_id がないと更新できないため、サーバー側で SELECT d.detail_id も必要
                            // ★ここ重要: サーバーがdetail_idを返していない場合、動かない可能性があります
                            val detailId = (map["detail_id"] as? Double)?.toInt() // Gsonは数値をDoubleにする場合がある
                                ?: (map["detail_id"] as? Int)
                                ?: return@mapNotNull null // IDがないデータはスキップ

                            KitchenItem(
                                detailId = detailId,
                                orderId = ((map["order_id"] as? Double)?.toInt() ?: map["order_id"] as? Int) ?: 0,
                                menuName = (map["menu_name"] as? String) ?: "不明",
                                quantity = ((map["quantity"] as? Double)?.toInt() ?: map["quantity"] as? Int) ?: 1,
                                status = (map["item_status"] as? String) ?: "未調理",
                                tableNumber = (map["table_number"] as? String) ?: "-"
                            )
                        } catch (e: Exception) {
                            null
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
        viewModelScope.launch {
            try {
                val response = apiService.updateKdsStatus(detailId, newStatus)
                if (response.isSuccessful) {
                    fetchItems() // 更新成功したら即座にリストを再取得
                } else {
                    _errorMessage.value = "ステータス更新失敗"
                }
            } catch (e: Exception) {
                _errorMessage.value = "通信エラー"
            }
        }
    }
}