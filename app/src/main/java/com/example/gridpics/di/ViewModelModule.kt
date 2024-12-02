package com.example.gridpics.di

import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.PicturesViewModel
import org.koin.dsl.module

val viewModelModule = module {
	single {
		PicturesViewModel(get())
	}

	single {
		DetailsViewModel(get())
	}

}