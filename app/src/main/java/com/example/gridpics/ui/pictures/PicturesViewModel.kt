package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.NothingFound, false, ""))
	var currentPicture = ""
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	fun observeBackNav(): Flow<Boolean> = backNav
	init
	{
		viewModelScope.launch {
			var value = picturesUiState.value
			interactor.getPics().collect { urls ->
				value = when(urls)
				{
					is Resource.Data -> value.copy(loadingState = PicturesState.SearchIsOk(urls.value))
					is Resource.ConnectionError -> value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound -> value.copy(loadingState = PicturesState.NothingFound)
				}
				picturesUiState.value = value
			}
		}
	}

	fun postState(useLoadedState: Boolean, urls: String)
	{
		var value = picturesUiState.value
		viewModelScope.launch {
			value = if(useLoadedState)
			{
				value.copy(loadingState = PicturesState.Loaded(urls))
			} else
			{
				value.copy(loadingState = PicturesState.SearchIsOk(urls))
			}
			picturesUiState.value = value
		}
	}

	fun postSavedUrls(urls: String?)
	{
		var value = picturesUiState.value
		viewModelScope.launch {
			value = value.copy(picturesUrl = urls)
		}
	}

	fun postCacheWasCleared(cacheWasCleared: Boolean)
	{
		var value = picturesUiState.value
		viewModelScope.launch {
			value = value.copy(clearedCache = cacheWasCleared)
		}
	}

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
		return errorsList.contains(url)
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

	fun backNavButtonPress(pressed: Boolean)
	{
		backNav.value = pressed
	}

	fun clickOnPicture(url: String)
	{
		Log.d("recompose", "I call recompose :)")
		currentPicture = url
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}