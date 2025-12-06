package com.pos.client.data.model

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import retrofit2.http.Query // これを追加
import retrofit2.Response

// --- データクラス ---

data class TableStatus(
    @SerializedName("id") val tableId: Int,
    @SerializedName("floor_id") val floorId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("order_id") val orderId: Int?,// ★追加: サーバーから受け取る注文ID
    @SerializedName("book_id") val bookId: Int? // ★★★ 追加: サーバーから受け取るBookID ★★★
) {
    val isOccupied: Boolean get() = status == "OCCUPIED"
}

data class Floor(
    @SerializedName("floorId") val floorId: Int,
    @SerializedName("name") val name: String
)

data class MenuBook(
    @SerializedName("bookId") val bookId: Int,
    @SerializedName("bookName") val bookName: String
)

data class OrderResponse(
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("message") val message: String
)

data class MenuItem(
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("menu_name") val menuName: String,
    @SerializedName("price") val price: Int,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("is_sold_out") val isSoldOut: Boolean = false,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class OrderDetail(
//    内容同じ
    // 新規登録時は null を送る（0だと更新扱いになりエラーになる）
    val detailId: Int? = null,

    // サーバーは "orderId" を期待しています（前回の order_id は間違いでした）
    val orderId: Int,
    val menuId: Int,
    val quantity: Int,

    // 金額やステータスもキャメルケースで送ります
    val priceAtOrder: Int? = null,
    val subtotal: Int? = null,
    val itemStatus: String? = null,

    // オプションは空リストでもOK
    val optionIds: List<Int>? = null,
    val optionsText: String? = null
)
// サーバーは "order_id" を返してくるので、ここでマッピングしないとIDが0になります
data class StartOrderResponse(
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("message") val message: String
)

// サーバーは "detail_id" を返してくるためマッピングが必要
data class OrderDetailResponse(
    @SerializedName("detail_id") val detailId: Int,
    val message: String
)

data class OrderHeaderRequest(
    val tableId: Int,
    val bookId: Int,
    val customerCount: Int
)

// --- カート・履歴機能用のデータクラス ---

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int,
    val selectedOptions: List<OptionItem> = emptyList() // ★追加
)

data class AccountingResponse(
    val header: OrderHeaderInfo?,
    val details: List<OrderDetailInfo>?,
    val paymentMethods: List<PaymentMethodInfo>?
)

data class OrderHeaderInfo(
    val orderId: Int,
    val totalAmount: Int,
    val orderStatus: String
)

data class OrderDetailInfo(
    val detailId: Int,
    val menuId: Int,
    val quantity: Int,
    val subtotal: Int,
    val itemStatus: String
    // メニュー名はViewModel側でマスタから結合して表示します
)

data class PaymentMethodInfo(
    val paymentId: Int,
    val methodName: String
)

// オプションマスタ
data class OptionItem(
    @SerializedName("optionId") val optionId: Int,
    @SerializedName("optionName") val optionName: String,
    @SerializedName("price") val price: Int
)

// --- KDS用のデータクラス ---
data class KdsItem(
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("menu_name") val menuName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("item_status") val itemStatus: String, // '未調理', '調理中'
    @SerializedName("table_number") val tableNumber: String?
    // detail_id がサーバーのレスポンス(Map)に含まれていない場合は追加が必要ですが、
    // サーバー側のSQL(OrderDetailRepository)を見る限り detail_id がSELECT句にない可能性があります。
    // ★サーバー側の修正が必要ないよう、SQLのSELECT句に d.detail_id を追加したと仮定するか、
    // 現状のサーバーコードに合わせて修正します。
    // サーバーコード: SELECT d.order_id... となっています。更新には detailId が必須です。
    // ここでは一旦 detailId がある前提で書きますが、動かない場合はサーバークエリ修正が必要です。
)

// 会計登録用リクエストデータ
data class AccountingRequest(
    @SerializedName("orderId") val orderId: Int,
    @SerializedName("paymentId") val paymentId: Int,
    @SerializedName("paymentAmount") val paymentAmount: Int,
    // 今回は値引きなし、お釣り計算なしの簡易実装とします
    @SerializedName("discountId") val discountId: Int = 1, // 1:なし
    @SerializedName("discountValue") val discountValue: Int = 0
)

// --- API Service Interface ---

interface ApiService {
    @GET("tables/status")
    suspend fun getTableStatuses(): Response<List<TableStatus>>

    @GET("floors")
    suspend fun getFloors(): Response<List<Floor>>

    @GET("menus/books")
    suspend fun getMenuBooks(): Response<List<MenuBook>>

    @GET("menus/{bookId}")
    suspend fun getMenuStructure(@Path("bookId") bookId: Int): Response<Map<String, Map<String, List<MenuItem>>>>

    @POST("order/start")
    suspend fun startOrder(@Body orderRequest: OrderHeaderRequest): Response<OrderResponse>

    @POST("order/add")
    suspend fun addOrderItem(@Body item: OrderDetail): Response<OrderDetailResponse>

    @GET("accounting/details/{orderId}")
    suspend fun getAccountingDetails(@Path("orderId") orderId: Int): Response<AccountingResponse>

    // ★追加: オプション一覧取得
    @GET("menus/options")
    suspend fun getOptions(): Response<List<OptionItem>>

    // ★追加: KDS用API
    @GET("kds/items/pending")
    suspend fun getKdsItems(): Response<List<Map<String, Any>>> // 柔軟にMapで受け取る

    @POST("kds/item/{detailId}/status")
    suspend fun updateKdsStatus(
        @Path("detailId") detailId: Int,
        @Query("status") status: String
    ): Response<Map<String, String>>

    // ★追加: 会計完了API
    @POST("accounting/complete")
    suspend fun completeAccounting(@Body request: AccountingRequest): Response<Any>
}

// --- Repository ---

class TableRepository(private val apiService: ApiService) {
    suspend fun getTableStatuses(): List<TableStatus> {
        return try {
            val response = apiService.getTableStatuses()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) { emptyList() }
    }
    suspend fun getFloors(): List<Floor> {
        return try {
            val response = apiService.getFloors()
            if (response.isSuccessful) response.body() ?: emptyList() else MockData.mockFloors
        } catch (e: Exception) { MockData.mockFloors }
    }

    // data/model/CoreData.kt 内の TableRepository クラスに追加
    suspend fun getMenuBooks(): List<MenuBook> {
        return try {
            val response = apiService.getMenuBooks()
            if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) { emptyList() }
    }
}

object MockData {
    val mockFloors = listOf(Floor(1, "1F"), Floor(2, "2F"))
}