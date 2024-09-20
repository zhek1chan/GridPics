package com.example.gridpics.data.network

import com.example.gridpics.data.dto.PicturesDto
import retrofit2.Response
import retrofit2.http.GET

interface Api {
    @GET("images.txt")
    suspend fun getNews(): Response<PicturesDto>
}