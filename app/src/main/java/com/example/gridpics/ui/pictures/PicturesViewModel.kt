package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
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
		Log.d("pictures urls", "$urls")
		val flow = picturesUiState
		val notNullUrls = urls ?: ""
		if(caseEmptySharedPreferenceOnFirstLaunch)
		{
			saveSharedPictureForFirstLaunch = notNullUrls
		}
		else
		{
			viewModelScope.launch {
				flow.value = flow.value.copy(picturesUrl = notNullUrls)
			}
		}
	}

	fun removeUrlFromSavedUrls(url: String)
	{
		Log.d("Removed from list", url)
		val flow = picturesUiState
		viewModelScope.launch {
			while(flow.value.picturesUrl.isEmpty())
			{
				delay(300)
			}
			saveSharedPictureForFirstLaunch = ""
			flow.value = flow.value.copy(picturesUrl = removePrefix(flow.value.picturesUrl, "$url\n"))
		}
		Log.d("Removed from list - now list is", flow.value.picturesUrl)
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
		state.value = state.value.copy(currentPicture = url)
	}

	fun postUsedIntent(url: String)
	{
		usedValueFromIntent = url
	}

	fun clearUsedIntentValue()
	{
		usedValueFromIntent = ""
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}