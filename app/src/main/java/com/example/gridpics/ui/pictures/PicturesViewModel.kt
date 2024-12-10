package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.NothingFound, "", LazyGridState()))
	var currentPicture = mutableStateOf("")
	private var savedPosition = Pair(0, 0)
	private var wasClicked = false
	private val errorsList: MutableList<String> = mutableListOf()

	init
	{
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data -> flow.value = flow.value.copy(loadingState = PicturesState.SearchIsOk(urls.value))
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

	fun postSavedUrls(urls: String?)
	{
		val flow = picturesUiState
		viewModelScope.launch {
			flow.value = flow.value.copy(picturesUrl = urls)
		}
	}

	fun saveListState(listState: LazyGridState)
	{
		picturesUiState.value = picturesUiState.value.copy(listState = listState)
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
		val list = errorsList
		return if(list.isNotEmpty())
		{
			list.contains(url)
		}
		else false
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

	fun restoreScrollPosition()
	{
		if (wasClicked)
		{
			Log.d("zapuskaem scroll", "log log, $savedPosition")
			viewModelScope.launch {
				delay(150)
				picturesUiState.value.listState.scrollToItem(savedPosition.first, savedPosition.second)
			}
			wasClicked = false
		}
	}

	fun clickOnPicture(url: String)
	{
		wasClicked = true
		currentPicture.value = url
		val state = picturesUiState.value.listState
		Log.d("zapuskaem scroll", "pochemuto bil click")
		savedPosition = Pair(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}