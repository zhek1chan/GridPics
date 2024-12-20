package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(""), "", 0, 0, "", true))
	var usedValueFromIntent = ""
	private val errorsList: MutableList<String> = mutableListOf()
	private var saveSharedPictureForFirstLaunch = ""
	private var index = 0
	var newIntentFlag = true

	init
	{
		Log.d("lifecycle", "vm is recreated")
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
					{
						val savedUrls = saveSharedPictureForFirstLaunch
						flow.value = flow.value.copy(
							loadingState = PicturesState.SearchIsOk(savedUrls + urls.value)
						)
						Log.d("pictures were loaded", savedUrls + urls.value)
					}
					is Resource.ConnectionError -> flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound -> flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
				}
			}
		}
	}

	fun postState(useLoadedState: Boolean, urls: String)
	{
		val flow = picturesUiState
		viewModelScope.launch {
			flow.value = if(useLoadedState)
			{
				flow.value.copy(loadingState = PicturesState.Loaded(urls))
			}
			else
			{
				flow.value.copy(loadingState = PicturesState.SearchIsOk(urls))
			}
		}
	}

	fun postSavedUrls(urls: String?, caseEmptySharedPreferenceOnFirstLaunch: Boolean)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val flow = picturesUiState
			var notNullUrls = urls ?: ""
			if(caseEmptySharedPreferenceOnFirstLaunch)
			{
				saveSharedPictureForFirstLaunch = notNullUrls
			}
			else
			{
				notNullUrls = newStringWithoutDoubles(notNullUrls.split("\n").toMutableList())
				flow.value = flow.value.copy(picturesUrl = notNullUrls)
			}
		}
	}

	fun removeUrlFromSavedUrls(url: String)
	{
		Log.d("index", "i called removeUrl")
		val flow = picturesUiState
		viewModelScope.launch(Dispatchers.IO) {
			while(flow.value.picturesUrl.isEmpty())
			{
				delay(100)
			}
			saveSharedPictureForFirstLaunch = ""
			for(i in 0 .. 1)
			{
				flow.value = flow.value.copy(picturesUrl = removePrefix(flow.value.picturesUrl, "$url\n"))
			}
		}
	}

	private fun removePrefix(str: String, prefix: String): String =
		if(str.startsWith(prefix)) str.substring(prefix.length) else str

	fun addError(url: String)
	{
		val list = errorsList
		if(!list.contains(url))
		{
			list.add(url)
		}
	}

	fun checkOnErrorExists(url: String): Boolean
	{
		val list = errorsList
		return (list.isNotEmpty() && list.contains(url))
	}

	fun removeSpecialError(url: String)
	{
		val list = errorsList
		if(list.contains(url))
		{
			list.remove(url)
		}
	}

	fun clearErrors()
	{
		errorsList.clear()
	}

	fun clickOnPicture(url: String, index: Int, offset: Int)
	{
		val state = picturesUiState
		state.value = state.value.copy(index = index, offset = offset, currentPicture = url)
	}

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState
		state.value = state.value.copy(isPortraitOrientation = isPortrait)
	}

	fun saveCurrentPictureUrl(url: String)
	{
		val state = picturesUiState
		state.value = state.value.copy(currentPicture = url + "\n")
	}

	fun restoreDeletedUrl(url: String)
	{
		Log.d("index", "i called restoreDeleted")
		val state = picturesUiState
		val list = state.value.picturesUrl.split("\n").toMutableList()
		list.add(index, url)
		val newString = newStringWithoutDoubles(list)
		//string problems fixer
		if(newString.startsWith("\n"))
		{
			newString.removeRange(0 .. 1)
		}
		if(newString.endsWith("\n"))
		{
			newString.removeRange(newString.length - 2 ..< newString.length)
		}
		if(newString.contains("\n\n"))
		{
			newString.replace("\n\n", "\n")
		}
		Log.d("index tut new string", newString)
		state.value = state.value.copy(picturesUrl = newString)
	}

	private fun newStringWithoutDoubles(list: MutableList<String>): String
	{
		var newString = ""
		val size = list.size
		for(i in 0 ..< size)
		{
			val item = list[i]
			if(item.isNotEmpty() && !item.contains("\n"))
			{
				newString += if(i != size - 1)
				{
					item + "\n"
				}
				else
				{
					item
				}
			}
		}
		return newString
	}

	fun postUsedIntent(url: String)
	{
		usedValueFromIntent = url
	}

	fun clearUsedIntentValue()
	{
		usedValueFromIntent = ""
	}

	fun postIntentWasUsed(used: Boolean)
	{
		newIntentFlag = used
	}

	fun urlWasAlreadyInSP(url: String, urlsFromSP: String)
	{
		Log.d("index", "i called urlWasAlready")
		val list = urlsFromSP.split("\n")
		index = list.indexOf(url)
		Log.d("index num", index.toString())
	}

	fun replaceInRightWay()
	{
		Log.d("index", "i called replace")
		val state = picturesUiState
		val list = state.value.picturesUrl.split("\n").toMutableList()
		val wrongFirst = list[0]
		list.add(index, wrongFirst)
		list.removeAt(0)
		state.value = state.value.copy(picturesUrl = newStringWithoutDoubles(list))
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}