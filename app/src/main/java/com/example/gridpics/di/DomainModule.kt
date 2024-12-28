package com.example.gridpics.di

import android.app.Application
import com.example.gridpics.data.repository.ImagesRepositoryImpl
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.domain.interactor.ImagesInteractorImpl
import com.example.gridpics.domain.repository.ImagesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val domainModule = module {
	single<ImagesInteractor> {
		ImagesInteractorImpl(repository = get(), context = androidContext() as Application)
	}

	single<ImagesRepository> {
		ImagesRepositoryImpl(networkClient = get())
	}
}