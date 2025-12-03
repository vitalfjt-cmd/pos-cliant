package com.pos.client.di

import com.pos.client.api.OrderApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // サーバーのURL (エミュレータなら 10.0.2.2、実機ならPCのIP)
    // MainActivityと同じIPアドレスを指定
    private const val BASE_URL = "http://192.168.45.2:8080/"

    val instance: OrderApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(OrderApi::class.java)
    }
}