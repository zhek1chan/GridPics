package com.example.gridpics.ui.pictures

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.Image
import coil3.asDrawable
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
class PicturesViewModel(
	private val interactor: ImagesInteractor,
	private val context: Context,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.NothingFound, false, ""))
	var currentPicture = mutableStateOf("")
	var cacheIsEmpty = true

	var loadedPictures: MutableList<Image> = mutableListOf()
	private val errorsList: MutableList<String> = mutableListOf()
	private val backNav = MutableStateFlow(false)
	private var dataWasDelivered = MutableStateFlow(false)
	private fun observeDelivery(): Flow<Boolean> = dataWasDelivered
	fun observeBackNav(): Flow<Boolean> = backNav

	init
	{
		val flow = picturesUiState
		viewModelScope.launch {
			interactor.getPics().collect { urls ->
				when(urls)
				{
					is Resource.Data ->
					{
						flow.value = flow.value.copy(loadingState = PicturesState.SearchIsOk(urls.value))
						if(cacheIsEmpty)
						{
							val list = (urls.value).split("\n")
							val loadedList = mutableListOf<Image>()
							var count = 1
							for(item in list)
							{
								val imgRequest =
									ImageRequest.Builder(context)
										.data(item)
										.allowHardware(false)
										.httpHeaders(
											NetworkHeaders.Builder()
												.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
												.build()
										)
										.networkCachePolicy(CachePolicy.ENABLED)
										.memoryCachePolicy(CachePolicy.ENABLED)
										.coroutineContext(Dispatchers.IO)
										.diskCachePolicy(CachePolicy.ENABLED)
										.placeholder(R.drawable.loading)
										.error(R.drawable.error)
										.target {
											loadedList.add(it)
											Log.d("loaded Size", "${loadedList.size}")
											if(loadedList.size > 5 * count)
											{
												loadedPictures = loadedList
												count++
												this.launch {
													observeDelivery().collectLatest { it1 ->
														while(!it1)
														{
															delay(1)
														}
														dataWasDelivered.emit(false)
													}
												}
											}
											if(it.asDrawable(context.resources) == getDrawable(context, R.drawable.error))
											{
												errorsList.add(item)
											}
										}
										.build()
								context.imageLoader
									.enqueue(imgRequest)
							}
						}
					}
					is Resource.ConnectionError -> flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound -> flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
				}
			}
		}
	}

	fun postDataWasDelivered(delivered: Boolean)
	{
		viewModelScope.launch {
			dataWasDelivered.emit(delivered)
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

	fun postCacheWasCleared(cacheWasCleared: Boolean)
	{
		val flow = picturesUiState
		viewModelScope.launch {
			flow.value = flow.value.copy(clearedCache = cacheWasCleared)
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

	fun backNavButtonPress(pressed: Boolean)
	{
		viewModelScope.launch {
			backNav.emit(pressed)
		}
	}

	fun clickOnPicture(url: String)
	{
		currentPicture.value = url
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}
}