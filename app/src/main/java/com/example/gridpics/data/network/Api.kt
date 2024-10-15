package com.example.gridpics.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming

interface Api
{
	@Streaming
	@GET("images.txt")
	suspend fun getNews(): ResponseBody
}