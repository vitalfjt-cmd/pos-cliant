package com.pos.client.data.repository

import com.pos.client.api.OrderApi
import com.pos.client.data.model.MergeOrderRequest
import com.pos.client.data.model.SplitOrderRequest
import com.pos.client.data.model.SplitOrderResponse

class OrderRepository(private val api: OrderApi) {

    // 伝票分割
    suspend fun splitOrder(sourceOrderId: Int, detailIds: List<Int>): SplitOrderResponse? {
        return try {
            val response = api.splitOrder(SplitOrderRequest(sourceOrderId, detailIds))
            if (response.isSuccessful) {
                response.body()
            } else {
                // エラー時はnullを返すか、例外を投げる
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 伝票合算
    suspend fun mergeOrders(sourceOrderId: Int, targetOrderId: Int): Boolean {
        return try {
            val response = api.mergeOrders(MergeOrderRequest(sourceOrderId, targetOrderId))
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}