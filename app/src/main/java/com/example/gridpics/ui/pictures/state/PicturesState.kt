package com.example.gridpics.ui.pictures.state

sealed class PicturesState
{
	data object NothingFound: PicturesState()
	data object ConnectionError: PicturesState()
	data class SearchIsOk(val data: List<String>): PicturesState()
}
