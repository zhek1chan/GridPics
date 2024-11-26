package com.example.gridpics.ui.pictures.state

data class PicturesScreenUiState(
	val loadingState: PicturesState,
	val clearedCache: Boolean,
	val picturesUrl: String?
)
