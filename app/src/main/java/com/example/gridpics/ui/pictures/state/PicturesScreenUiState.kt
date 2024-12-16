package com.example.gridpics.ui.pictures.state

data class PicturesScreenUiState(
	val loadingState: PicturesState,
	val picturesUrl: String,
	val index: Int,
	val offset: Int,
	val currentPicture: String,
	val isPortraitOrientation: Boolean,
)
