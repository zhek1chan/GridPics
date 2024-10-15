package com.example.gridpics.di

import com.example.gridpics.data.ThemeSettingsImpl
import com.example.gridpics.data.repository.ImagesRepositoryImpl
import com.example.gridpics.domain.ThemeSettings
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.domain.interactor.ImagesInteractorImpl
import com.example.gridpics.domain.interactor.SettingsInteractor
import com.example.gridpics.domain.interactor.SettingsInteractorImpl
import com.example.gridpics.domain.repository.ImagesRepository
import org.koin.dsl.module

val domainModule = module {
	factory<ImagesInteractor> {
		ImagesInteractorImpl(repository = get())
	}

	single<ImagesRepository> {
		ImagesRepositoryImpl(networkClient = get())
	}

	single<ThemeSettings> {
		ThemeSettingsImpl(get(), get())
	}

	single<SettingsInteractor> {
		SettingsInteractorImpl(get())
	}
}