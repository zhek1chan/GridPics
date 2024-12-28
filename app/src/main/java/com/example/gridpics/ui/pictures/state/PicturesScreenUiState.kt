package com.example.gridpics.ui.pictures.state

import com.example.gridpics.ui.settings.ThemePick

data class PicturesScreenUiState(
	val loadingState: PicturesState,
	val picturesUrl: MutableList<String>,
	val index: Int,
	val offset: Int,
	val currentPicture: String,
	val isPortraitOrientation: Boolean,
	val themeState: ThemePick
)
