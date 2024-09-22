package com.example.gridpics.domain.interactor

import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.ImagesRepository
import kotlinx.coroutines.flow.Flow

class ImagesInteractorImpl(
    private val repository: ImagesRepository
) : ImagesInteractor {
    override suspend fun getPics(): Flow<Resource<String>> =
        repository.getPics()
}