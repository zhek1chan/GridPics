package com.example.gridpics.domain.model

import android.graphics.Bitmap

data class PicturesDataForNotification(
	val url: String?,
	val bitmap: Bitmap?,
	val showButtons: Boolean,
)
