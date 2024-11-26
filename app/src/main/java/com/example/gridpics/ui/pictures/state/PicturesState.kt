package com.example.gridpics.ui.pictures.state

sealed class PicturesState
{
	data object NothingFound: PicturesState()
	data object ConnectionError: PicturesState()
	data class SearchIsOk(val data: String): PicturesState()
	data class Loaded(val data: String): PicturesState()
}
