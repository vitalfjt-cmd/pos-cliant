package com.pos.client.data.model

// ★今回の追加
data class SplitOrderRequest(
    val sourceOrderId: Int,
    val detailIds: List<Int>
)

data class SplitOrderResponse(
    val sourceOrderId: Int,
    val newOrderId: Int,
    val message: String
)



data class TableStatusResponse(
    val id: Int,       // テーブルID
    val floor_id: Int,
    val status: String,
    val capacity: Int,
    val order_id: Int?, // 紐付いている伝票ID
    val book_id: Int?   // 紐付いているブックID
) {
    // ViewModelで tableId, orderId として参照している場合は
    // @SerializedName を使うか、ViewModel側を修正する必要がありますが
    // ここではViewModelに合わせてプロパティを追加/マッピングします
    val tableId: Int get() = id
    val orderId: Int? get() = order_id
}
data class MergeOrderRequest(
    val sourceOrderId: Int,
    val targetOrderId: Int
)