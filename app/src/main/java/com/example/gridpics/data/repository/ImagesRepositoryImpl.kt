package com.example.gridpics.data.repository

import com.example.gridpics.data.network.NetworkClient
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.ImagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ImagesRepositoryImpl(
	private val networkClient: NetworkClient,
): ImagesRepository
{
	override suspend fun getPics(): Flow<Resource<String>> = flow {
		while(true)
		{
			when(val response = networkClient.getPics())
			{
				is Resource.Data ->
				{
					with(response) {
						val data = value
						emit(Resource.Data(data))
					}
					break
				}
				is Resource.NotFound -> {
					emit(Resource.NotFound(response.message))
					break
				}
				is Resource.ConnectionError ->
				{
					emit(Resource.ConnectionError(response.message))
					kotlinx.coroutines.delay(1000)
				}
			}
		}
	}.flowOn(Dispatchers.IO)
}