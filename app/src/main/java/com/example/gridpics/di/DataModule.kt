package com.example.gridpics.di

import com.example.gridpics.data.network.Api
import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.RetrofitNetworkClient
import com.google.gson.Gson
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val dataModule = module {
    single<Api> {

        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://it-link.ru/test/")
            .build()
            .create(Api::class.java)

    }

    factory { Gson().setLenient()
        .create() }

    single<NetworkClient> {
        RetrofitNetworkClient(get(), get())
    }
}