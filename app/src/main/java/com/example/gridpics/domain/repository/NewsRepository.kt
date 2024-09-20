package com.example.gridpics.domain.repository

import com.example.gridpics.data.network.Resource
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    suspend fun getPics(): Flow<Resource<List<String>>>
}