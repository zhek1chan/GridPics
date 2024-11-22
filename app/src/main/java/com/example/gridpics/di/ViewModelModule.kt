package com.example.gridpics.di

import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
	single {
		PicturesViewModel(get())
	}

	viewModelOf(::PicturesViewModel)

	single {
		DetailsViewModel()
	}

	viewModelOf(::DetailsViewModel)

	single { SettingsViewModel() }

	viewModelOf(::SettingsViewModel)
}