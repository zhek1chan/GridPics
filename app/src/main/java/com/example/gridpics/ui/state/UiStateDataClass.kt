package com.example.gridpics.ui.state

import com.example.gridpics.ui.pictures.PicturesState

data class UiStateDataClass(
	val isMultiWindowed: Boolean,
	val barsAreVisible: Boolean,
	val pictureScreenState: PicturesState,
)