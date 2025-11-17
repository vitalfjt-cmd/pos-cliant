package com.pos.client.data.model

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.Response

// (TableStatus, Floor, MenuBook, MenuItem, OrderDetail, OrderDetailResponse, OrderHeaderRequest の定義は変更なし)
// ... (中略) ...
data class TableStatus(
    @SerializedName("id") val tableId: Int,
    @SerializedName("floor_id") val floorId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("capacity") val capacity: Int
) {
    val isOccupied: Boolean get() = status == "OCCUPIED"
}
data class Floor(
    @SerializedName("id") val floorId: Int,
    @SerializedName("name") val name: String
)
data class MenuBook(
    @SerializedName("bookId") val bookId: Int,
    @SerializedName("bookName") val bookName: String
)
// ★★★ 修正箇所：サーバー(OrderController)が返すMapと一致させる ★★★
data class OrderResponse(
    // @SerializedName("orderId") val orderId: Int, // 古い定義
    // @SerializedName("status") val status: String // 古い定義
    @SerializedName("order_id") val orderId: Int, // ★ サーバーの "order_id" と一致
    @SerializedName("message") val message: String // ★ サーバーの "message" と一致
)
data class MenuItem(
    @SerializedName("menu_id") val menuId: Int,
    @SerializedName("menu_name") val menuName: String,
    @SerializedName("price") val price: Int,
    @SerializedName("category_id") val categoryId: Int
)
data class OrderDetail(
    val detailId: Int? = null,
    val orderId: Int,
    val menuId: Int,
    val quantity: Int,
    val priceAtOrder: Int? = null,
    val subtotal: Int? = null,
    val itemStatus: String? = null
)
data class OrderDetailResponse(
    @SerializedName("detail_id") val detailId: Int,
    val message: String
)
data class OrderHeaderRequest(
    val tableId: Int,
    val bookId: Int,
    val customerCount: Int
)


// --- API Service Interface ---
// (APIのシグネチャは all_kotlin.txt のままで完璧です)
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
    suspend fun startOrder(@Body orderRequest: OrderHeaderRequest): Response<OrderResponse> // ★ 戻り値が OrderResponse

    @POST("order/add")
    suspend fun addOrderItem(@Body item: OrderDetail): Response<OrderDetailResponse>
}

// --- Repository (データ取得ロジック) ---
// (変更なし)
class TableRepository(private val apiService: ApiService) {
    suspend fun getTableStatuses(): List<TableStatus> {
        return try {
            val response = apiService.getTableStatuses()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                println("API Error (TableStatus): ${response.code()} ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            println("Network Error (TableStatus): ${e.message}")
            emptyList()
        }
    }
    suspend fun getFloors(): List<Floor> {
        return try {
            val response = apiService.getFloors()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                println("API Error (Floors): ${response.code()} ${response.message()}")
                MockData.mockFloors // フォールバック
            }
        } catch (e: Exception) {
            println("Network Error (Floors): ${e.message}")
            MockData.mockFloors // フォールバック
        }
    }
}

// --- Mock Data (API呼び出し失敗時のフォールバックとして残す) ---
// (変更なし)
object MockData {
    val mockFloors = listOf(
        Floor(floorId = 1, name = "1F (ホール)"),
        Floor(floorId = 2, name = "2F (個室)")
    )
}