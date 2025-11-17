package com.pos.client.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// ★ Button, Column, Scaffold などを OrderScreen.kt に移動
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.pos.client.data.model.ApiService
import com.pos.client.data.model.TableRepository
import com.pos.client.ui.theme.PosClientTheme
import com.pos.client.viewmodel.OrderViewModel
import com.pos.client.viewmodel.TableListViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// (Screen Sealed Class, DI は変更なし)
sealed class Screen {
    data object TableList : Screen()
    data class Order(val tableId: Int, val bookId: Int) : Screen()
}
private val gson = Gson()
private const val BASE_URL = "http://192.168.45.2:8080/api/"
private val retrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}
private val apiService: ApiService by lazy {
    retrofit.create(ApiService::class.java)
}
private val tableRepository: TableRepository by lazy {
    TableRepository(apiService)
}
private val tableListViewModel: TableListViewModel by lazy {
    TableListViewModel(tableRepository)
}
private val orderViewModel: OrderViewModel by lazy {
    OrderViewModel(apiService)
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PosClientTheme {
                var currentScreen: Screen by remember { mutableStateOf(Screen.TableList) }

                when (val screen = currentScreen) {
                    // 1. テーブル一覧画面
                    is Screen.TableList -> {
                        TableListScreen(
                            viewModel = tableListViewModel,
                            onTableClicked = { table ->
                                currentScreen = Screen.Order(table.tableId, 1) // (bookId 1 を指定)
                            }
                        )
                    }

                    // 2. 注文画面
                    is Screen.Order -> {
                        // ★★★ 修正箇所：プレースホルダーから本物の OrderScreen に変更 ★★★
                        OrderScreen(
                            viewModel = orderViewModel,
                            tableId = screen.tableId,
                            bookId = screen.bookId,
                            onBackClicked = {
                                currentScreen = Screen.TableList // 一覧に戻る
                            }
                        )
                    }
                }
            }
        }
    }
}

// ★ プレースホルダー(OrderScreenPlaceholder)は OrderScreen.kt に移動したため削除