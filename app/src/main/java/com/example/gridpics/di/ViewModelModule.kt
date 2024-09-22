package com.example.gridpics.di

import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        PicturesViewModel(get())
    }

    viewModel {
        SettingsViewModel(get())
    }

}