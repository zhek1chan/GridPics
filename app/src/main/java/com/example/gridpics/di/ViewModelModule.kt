package com.example.gridpics.di

import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
	viewModel {
		PicturesViewModel(get(), androidContext())
	}

	viewModelOf(::PicturesViewModel)

	single {
		DetailsViewModel(get())
	}
}