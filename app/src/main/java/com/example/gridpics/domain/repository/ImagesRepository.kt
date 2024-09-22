package com.example.gridpics.domain.repository

import com.example.gridpics.data.network.Resource
import kotlinx.coroutines.flow.Flow

interface ImagesRepository {
    suspend fun getPics(): Flow<Resource<String>>
}