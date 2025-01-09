package com.example.gridpics.ui.details.state

data class DetailsScreenUiState(
	val isMultiWindowed: Boolean,
	val barsAreVisible: Boolean,
	val isSharedImage: Boolean,
	val picturesUrl: List<String>,
	val currentPicture: String
)