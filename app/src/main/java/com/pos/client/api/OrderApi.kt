package com.pos.client.api

import com.pos.client.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface OrderApi {

    // --- 既存機能 (ここに追加しないとViewModelから呼べません) ---

    // メニューブック取得
    @GET("api/menus/books")
    suspend fun getMenuBooks(): Response<List<MenuBook>>

    // メニュー構造取得
    @GET("api/menus/{bookId}")
    suspend fun getMenuStructure(@Path("bookId") bookId: Int): Response<Map<String, Map<String, List<MenuItem>>>>

    // オプション一覧取得
    @GET("api/menus/options")
    suspend fun getOptions(): Response<List<OptionItem>>

    // 注文開始
    @POST("api/order/start")
    suspend fun startOrder(@Body request: OrderHeaderRequest): Response<StartOrderResponse>

    // 注文明細追加
    @POST("api/order/add")
    suspend fun addOrderItem(@Body detail: OrderDetail): Response<OrderDetailResponse>

    // 会計情報取得
    @GET("api/accounting/details/{orderId}")
    suspend fun getAccountingDetails(@Path("orderId") orderId: Int): Response<AccountingResponse>

    // 会計完了
    @POST("api/accounting/complete")
    suspend fun completeAccounting(@Body request: AccountingRequest): Response<Map<String, Any>>

    // テーブル状況取得
    @GET("api/tables/status")
    suspend fun getTableStatuses(): Response<List<TableStatusResponse>>

    // --- 新機能 ---

    // 伝票分割
    @POST("api/order/split")
    suspend fun splitOrder(@Body request: SplitOrderRequest): Response<SplitOrderResponse>

    // 在庫更新 (前回実装したもの)
    @POST("api/menus/{menuId}/stock")
    suspend fun updateStock(@Path("menuId") menuId: Int, @Body body: Map<String, Boolean>): Response<Map<String, String>>

    // api/OrderApi.kt の interface OrderApi 内に追加

    // 伝票合算
    @POST("api/order/merge")
    suspend fun mergeOrders(@Body request: MergeOrderRequest): Response<Map<String, Any>>
}