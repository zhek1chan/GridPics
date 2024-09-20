package com.example.gridpics.data.repository

import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NewsRepositoryImpl(
    private val networkClient: NetworkClient,
) : NewsRepository {
    override suspend fun getPics(): Flow<Resource<List<String>>> = flow {
        when (val response = networkClient.getPics()) {
            is Resource.Data -> {
                with(response) {
                    val data = value.imageUrls
                    emit(Resource.Data(data))
                }
            }

            is Resource.NotFound -> emit(Resource.NotFound(response.message))
            is Resource.ConnectionError -> {
                emit(Resource.ConnectionError(response.message))
            }
        }
    }.flowOn(Dispatchers.IO)
}