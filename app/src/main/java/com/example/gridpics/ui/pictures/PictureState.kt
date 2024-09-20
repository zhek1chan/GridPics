package com.example.gridpics.ui.pictures

sealed class PictureState {
    data object NothingFound : PictureState()
    data object ConnectionError : PictureState()
    data class SearchIsOk(val data: List<String>) : PictureState()
}
