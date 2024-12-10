package com.example.gridpics.ui.pictures.state

import androidx.compose.foundation.lazy.grid.LazyGridState

data class PicturesScreenUiState(
	val loadingState: PicturesState,
	val picturesUrl: String?,
	val listState: LazyGridState
)
