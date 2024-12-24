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
import kotlinx.coroutines.launch

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(""), "", 0, 0, "", true))
	var usedValueFromIntent = ""
	private val errorsList: MutableList<String> = mutableListOf()
	private var index = 0
	var onPauseWasCalled = false
	private var caseFirstStart = false

	init
	{
		search()
	}

	private fun search()
	{
		Log.d("lifecycle", "vm is recreated")
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
					{
						flow.value = flow.value.copy(
							loadingState = PicturesState.SearchIsOk(urls.value)
						)
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
		viewModelScope.launch {
			val flow = picturesUiState
			val notNullUrls = urls ?: ""
			if(caseEmptySharedPreferenceOnFirstLaunch)
			{
				caseFirstStart = true
			}
			else
			{
				caseFirstStart = false
				flow.value = flow.value.copy(picturesUrl = notNullUrls)
			}
			Log.d("shared list", flow.value.picturesUrl)
		}
	}

	fun removeUrlFromSavedUrls(url: String)
	{
		viewModelScope.launch {
			Log.d("ahaha removing", url)
			val state = picturesUiState
			val urls = state.value.picturesUrl
			Log.d("ahaha first start?", "$caseFirstStart")
			Log.d("ahaha removing", url)
			removeUrlAndPostNewString(urls, url)
			caseFirstStart = false
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
		Log.d("lifecycle", "current pic was changed in click")
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
		viewModelScope.launch {
			Log.d("lifecycle", "current pic was changed in save")
			val state = picturesUiState
			Log.d("current picture", "real current pic $url")
			state.value = state.value.copy(currentPicture = url + "\n")
		}
	}

	fun postPicsFromThemeChange(url: String)
	{
		val state = picturesUiState
		val list = state.value.picturesUrl.split("\n").toMutableList()
		viewModelScope.launch(Dispatchers.IO) {
			list.add(0, url)
			val newString = createNewString(list)
			state.value = state.value.copy(picturesUrl = newString)
		}
	}

	fun restoreDeletedUrl()
	{
		val state = picturesUiState
		val url = state.value.currentPicture
		if(url.isNotEmpty())
		{
			val list = state.value.picturesUrl.split("\n").toMutableList()
			val index = index
			if(index < list.size)
			{
				list.add(index, url)
			}
			viewModelScope.launch {
				val newString = createNewString(list)
				Log.d("index new list", list.toString())
				Log.d("index new string", newString)
				state.value = state.value.copy(picturesUrl = newString)
			}
		}
	}

	fun postUsedIntent(url: String)
	{
		usedValueFromIntent = url
	}

	fun getUsedIntentValue(): String
	{
		return usedValueFromIntent
	}

	fun urlWasAlreadyInSP(url: String, urlsFromSP: String)
	{
		val list = urlsFromSP.split("\n")
		index = list.indexOf(url)
		Log.d("now index", "$index")
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}

	fun putPreviousPictureCorrectly(oldPicture: String)
	{
		val state = picturesUiState
		val value = state.value
		val list = value.picturesUrl.split("\n").toSet().toMutableList()
		viewModelScope.launch {
			if(list.contains(oldPicture))
			{
				list.remove(oldPicture)
			}
			val index = index
			list.add(index, oldPicture)
			if(list.contains("\n"))
			{
				list.remove("\n")
			}
			val newString = createNewString(list)
			state.value = state.value.copy(picturesUrl = newString)
		}
		Log.d("now index 2", "$index")
	}

	private fun createNewString(list: MutableList<String>): String
	{
		var newString = ""
		val size = list.size
		for(i in 0 ..< size)
		{
			newString += if(i != size)
			{
				list[i] + "\n"
			}
			else
			{
				list[i]
			}
		}
		//fix problems with string
		val withoutDoubleNewlines = newString.replace("\n\n", "\n")
		val withoutNewLinesInStart = withoutDoubleNewlines.trimStart('\n')
		val withoutTrailingNewlines = withoutNewLinesInStart.trimEnd('\n')
		return withoutTrailingNewlines
	}

	private fun removeUrlAndPostNewString(urls: String, url: String)
	{
		val state = picturesUiState
		val newString = if(urls.startsWith("$url\n"))
		{
			removePrefix(urls, "$url\n")
		}
		else
		{
			removePrefix(urls, url)
		}
		Log.d("we got removed:", "removed $newString")
		state.value = state.value.copy(picturesUrl = newString)
	}
}