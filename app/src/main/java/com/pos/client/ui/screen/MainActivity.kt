package com.pos.client.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart // POSアイコン
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.gson.Gson
import com.pos.client.data.model.ApiService
import com.pos.client.data.model.TableRepository
import com.pos.client.ui.theme.PosClientTheme
import com.pos.client.viewmodel.OrderViewModel
import com.pos.client.viewmodel.TableListViewModel
import com.pos.client.viewmodel.KitchenViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

sealed class Screen {
    data object TableList : Screen()
    data class Order(val tableId: Int, val bookId: Int, val existingOrderId: Int?, val customerCount: Int) : Screen()
    data object Kitchen : Screen()
    data object Accounting : Screen()
}

private val gson = Gson()
// ★環境に合わせて変更してください
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
private val kitchenViewModel: KitchenViewModel by lazy {
    KitchenViewModel(apiService)
}

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PosClientTheme {
                var currentScreen: Screen by remember { mutableStateOf(Screen.TableList) }

                when (val screen = currentScreen) {
                    is Screen.TableList -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("POS Client: テーブル一覧") },
                                    actions = {
                                        // ★追加: POSレジ画面へのボタン
                                        IconButton(onClick = { currentScreen = Screen.Accounting }) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "POSレジ")
                                        }
                                        IconButton(onClick = { currentScreen = Screen.Kitchen }) {
                                            Icon(Icons.Default.Settings, contentDescription = "Kitchen")
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                TableListScreen(
                                    viewModel = tableListViewModel,
                                    onTableClicked = { table, count, bookId ->
                                        currentScreen = Screen.Order(
                                            tableId = table.tableId,
                                            bookId = bookId,
                                            existingOrderId = table.orderId,
                                            customerCount = count
                                        )
                                    }
                                )
                            }
                        }
                    }

                    is Screen.Order -> {
                        OrderScreen(
                            viewModel = orderViewModel,
                            tableId = screen.tableId,
                            bookId = screen.bookId,
                            existingOrderId = screen.existingOrderId,
                            customerCount = screen.customerCount,
                            onBackClicked = { currentScreen = Screen.TableList },
                            onAccountingClicked = { currentScreen = Screen.Accounting }
                        )
                    }

                    is Screen.Kitchen -> {
                        KitchenScreen(
                            viewModel = kitchenViewModel,
                            onBackClicked = { currentScreen = Screen.TableList }
                        )
                    }

                    is Screen.Accounting -> {
                        AccountingScreen(
                            viewModel = orderViewModel,
                            onBackClicked = { currentScreen = Screen.TableList }
                        )
                    }
                }
            }
        }
    }
}