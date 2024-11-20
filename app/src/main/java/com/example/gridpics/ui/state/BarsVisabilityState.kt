package com.example.gridpics.ui.state

sealed class BarsVisabilityState
{
	data object IsVisible: BarsVisabilityState()
	data object NotVisible: BarsVisabilityState()
}