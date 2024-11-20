package com.example.gridpics.ui.state

sealed class MultiWindowState
{
	data object MultiWindow : MultiWindowState()
	data object NotMultiWindow : MultiWindowState()
}