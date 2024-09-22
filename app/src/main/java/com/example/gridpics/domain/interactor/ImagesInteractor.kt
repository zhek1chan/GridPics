package com.example.gridpics.domain.interactor

import com.example.gridpics.data.network.Resource
import kotlinx.coroutines.flow.Flow

interface ImagesInteractor {
    suspend fun getPics(): Flow<Resource<String>>
}