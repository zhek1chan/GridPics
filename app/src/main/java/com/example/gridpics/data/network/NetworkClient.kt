package com.example.gridpics.data.network

import com.example.gridpics.data.dto.PicturesDto

interface NetworkClient {

    suspend fun getPics(): Resource<String>

}