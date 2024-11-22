package com.example.gridpics.ui.state

import android.app.Activity
import androidx.lifecycle.MutableLiveData

data class MultiWindowStateTracker(
	private val activity: Activity,
)
{
	sealed class MultiWindowState
	{
		data object MultiWindow: MultiWindowState()
		data object NotMultiWindow: MultiWindowState()
	}

	val multiWindowState = MutableLiveData<MultiWindowState>(MultiWindowState.NotMultiWindow)
	fun checkState()
	{
		val newState = if(activity.isInMultiWindowMode || activity.isInPictureInPictureMode)
		{
			MultiWindowState.MultiWindow
		}
		else
		{
			MultiWindowState.NotMultiWindow
		}

		if(newState != multiWindowState.value)
		{
			multiWindowState.value = newState
		}
	}
}