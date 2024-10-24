package com.example.gridpics.di

import android.content.Context
import com.example.gridpics.App
import com.example.gridpics.data.network.Api
import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.RetrofitNetworkClient
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SP_KEY
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val dataModule = module {
	val gson = GsonBuilder().setLenient().create()
	single<Api> {
		Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(gson)).baseUrl("http://it-link.ru/test/").client(App.client).build().create(Api::class.java)
	}

	single {
		androidContext().getSharedPreferences(THEME_SP_KEY, Context.MODE_PRIVATE)
	}


	factory { Gson() }

	single<NetworkClient> {
		RetrofitNetworkClient(get(), get())
	}
}