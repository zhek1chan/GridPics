package com.example.gridpics.ui.pictures

import java.io.File

sealed class PictureState {
    data object NothingFound : PictureState()
    data object ConnectionError : PictureState()
    data class SearchIsOk(val data: String) : PictureState()
}
