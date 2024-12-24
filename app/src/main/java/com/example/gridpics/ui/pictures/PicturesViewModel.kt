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
	var onPauseWasCalled = false

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
		viewModelScope.launch {
			val flow = picturesUiState
			val notNullUrls = urls ?: ""
			if(caseEmptySharedPreferenceOnFirstLaunch)
			{
				saveSharedPictureForFirstLaunch = notNullUrls
			}
			else
			{
				flow.value = flow.value.copy(picturesUrl = notNullUrls)
			}
			Log.d("shared list", flow.value.picturesUrl)
		}
	}

	fun removeUrlFromSavedUrls(url: String)
	{
		val state = picturesUiState
		val urls = state.value.picturesUrl
		val urlsAreEmpty = urls.isEmpty()
		if(urlsAreEmpty)
		{
			viewModelScope.launch(Dispatchers.IO) {
				while(urlsAreEmpty)
				{
					//кейс, когда нет сохранённых картинок,
					//не успел загрузиться список с сервера и мы поделились картинкой в приложение (до первого входа)
					//ждём ответа-результата с сервера перед вставкой картинки, которой поделились
					//чтобы не блокировать main поток использую IO
					delay(300)
				}
				saveSharedPictureForFirstLaunch = ""
				removeUrlAndPostNewString(urls, url)
			}
		}
		else viewModelScope.launch {
			removeUrlAndPostNewString(urls, url)
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
			// тк мы запускаем функцию во время onRestart(),
			// то нужно использовать IO иначе main поток блокируется
			viewModelScope.launch(Dispatchers.IO) {
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

	fun clearUsedIntentValue()
	{
		usedValueFromIntent = ""
	}

	fun getUsedIntentValue(): String
	{
		return usedValueFromIntent
	}

	fun urlWasAlreadyInSP(url: String, urlsFromSP: String)
	{
		val list = urlsFromSP.split("\n")
		index = list.indexOf(url)
		Log.d("index", "$index")
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
		viewModelScope.launch(Dispatchers.IO) {
			while(list.contains(oldPicture))
			{
				list.remove(oldPicture)
			}
			val index = index
			list.add(index, oldPicture)
			while(list.contains("\n"))
			{
				list.remove("\n")
			}
			val newString = createNewString(list)
			state.value = state.value.copy(picturesUrl = newString)
		}
		index = 0
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
		while(newString.endsWith("\n") && newString.length >= 2)
		{
			newString.removeRange(newString.length - 2 ..< newString.length)
		}
		if(newString.contains("\n\n"))
		{
			newString.replace("\n\n", "\n")
		}
		if(newString.startsWith("\n") && newString.length >= 2)
		{
			newString.removeRange(0 .. 1)
		}
		return newString
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
		while(newString.startsWith("\n"))
		{
			newString.removeRange(0 .. 1)
		}
		state.value = state.value.copy(picturesUrl = newString)
	}

	fun replaceFirstValue() {
		/*val state = picturesUiState.value
		val currentPic = state.currentPicture
		postSavedUrls(currentPic+"\n"+state.picturesUrl, false)*/
	}
}