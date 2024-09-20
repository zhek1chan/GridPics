package com.example.gridpics.domain.interactor

import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow

class NewsInteractorImpl(
    private val repository: NewsRepository
) : NewsInteractor {
    override suspend fun getPics(): Flow<Resource<List<String>>> =
        repository.getPics()
}