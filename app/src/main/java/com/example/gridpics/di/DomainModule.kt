package com.example.gridpics.di

import com.example.gridpics.data.repository.NewsRepositoryImpl
import com.example.gridpics.domain.interactor.NewsInteractor
import com.example.gridpics.domain.interactor.NewsInteractorImpl
import com.example.gridpics.domain.repository.NewsRepository
import org.koin.dsl.module

val domainModule = module {
    factory<NewsInteractor> {
        NewsInteractorImpl(repository = get())
    }

    single<NewsRepository> {
        NewsRepositoryImpl(networkClient = get())
    }
}