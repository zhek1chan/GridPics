package com.example.gridpics.di

import com.example.gridpics.data.network.Api
import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.RetrofitNetworkClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val dataModule = module {
    val gson = GsonBuilder()
        .setLenient()
        .create()
    single<Api> {

        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl("https://it-link.ru/test/")
            .build()
            .create(Api::class.java)

    }

    factory { Gson() }

    single<NetworkClient> {
        RetrofitNetworkClient(get(), get())
    }
}