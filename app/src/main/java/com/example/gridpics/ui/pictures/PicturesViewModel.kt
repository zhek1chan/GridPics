package com.example.gridpics.ui.pictures

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.ImagesInteractor
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesState
import com.example.gridpics.ui.settings.ThemePick
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

class PicturesViewModel(
	private val interactor: ImagesInteractor,
): ViewModel()
{
	val picturesUiState = mutableStateOf(PicturesScreenUiState(PicturesState.SearchIsOk(mutableListOf()), mutableListOf(), 0, 0, true, ThemePick.FOLLOW_SYSTEM, emptyList()))
	private val errorsMap: MutableMap<String, String> = mutableMapOf()
	private var pairOfPivotsXandY = Pair(0.1f, 0.1f)
	private var gridQuantity = 0
	private var screenWidth = 0
	private var screenHeight = 0
	private var densityOfScreen = 0f
	private var initialPage = 0
	private var sizeOfGridInPixels = 0
	private var urlForCalculation = ""
	private var listOfPositions = mutableListOf<Pair<Float, Float>>()
	private var mapOfColumns = mutableMapOf<String, Int>()
	private var mapOfLines = mutableMapOf<String, Int>()
	private var firstVisibleItemIndex = 0
	private var numOfVisibleElements = 0
	private var cutouts = Pair(0f, 0f)

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
						val savedUrls = flow.value.picturesUrl
						val deletedUrls = flow.value.deletedUrls
						val urlsFromNet = convertToListFromString(urls.value)
						val urlsToAdd = if(savedUrls.isNotEmpty())
						{
							compareAndCombineLists(savedUrls, deletedUrls)
						}
						else
						{
							compareAndCombineLists(urlsFromNet, deletedUrls)
						}
						flow.value = flow.value.copy(
							loadingState = PicturesState.SearchIsOk(urlsToAdd),
							picturesUrl = urlsToAdd
						)
					}
					is Resource.ConnectionError ->
						flow.value = flow.value.copy(loadingState = PicturesState.ConnectionError)
					is Resource.NotFound ->
						flow.value = flow.value.copy(loadingState = PicturesState.NothingFound)
				}
			}
		}
	}

	fun addPictureToUrls(pic: String)
	{
		viewModelScope.launch {
			val state = picturesUiState
			val sendList = mutableListOf<String>()
			val list = state.value.picturesUrl
			val size = list.size
			for(i in 0 ..< size)
			{
				if(list[i] != pic)
				{
					sendList.add(list[i])
				}
			}
			sendList.add(0, pic)
			Log.d("checkCheck", "list $list")
			state.value = state.value.copy(picturesUrl = sendList)
		}
	}

	fun postSavedUrls(urls: List<String>)
	{
		val state = picturesUiState
		state.value = state.value.copy(picturesUrl = urls)
	}

	fun addError(url: String, message: String)
	{
		val map = errorsMap
		if(!map.contains(url))
		{
			map[url] = message
		}
	}

	fun checkOnErrorExists(url: String): String?
	{
		val list = errorsMap
		return if(list.contains(url))
		{
			list[url]
		}
		else
		{
			null
		}
	}

	fun removeSpecialError(url: String)
	{
		val map = errorsMap
		if(map.contains(url))
		{
			map.remove(url)
		}
	}

	fun clearErrors()
	{
		errorsMap.clear()
	}

	fun clickOnPicture(index: Int, offset: Int)
	{
		val state = picturesUiState
		val nIndex = if(index >= 0)
		{
			index
		}
		else
		{
			0
		}
		Log.d("checkIndex", "$nIndex")
		state.value = state.value.copy(index = nIndex, offset = offset)
	}

	fun changeOrientation(isPortrait: Boolean)
	{
		val state = picturesUiState
		if(isPortrait)
		{
			val k = screenWidth
			screenWidth = screenHeight
			screenHeight = k
		}
		else
		{
			val k = screenHeight
			screenHeight = screenWidth
			screenWidth = k
		}
		state.value = state.value.copy(isPortraitOrientation = isPortrait)
	}

	fun isValidUrl(url: String): Boolean
	{
		val urlPattern = Regex("^(https?|ftp)://([a-z0-9-]+\\.)+[a-z0-9]{2,6}(:[0-9]+)?(/\\S*)?$")
		return urlPattern.matches(url)
	}

	fun postThemePick(option: ThemePick)
	{
		val state = picturesUiState
		state.value = state.value.copy(themeState = option)
	}

	fun convertToListFromString(string: String?): List<String>
	{
		return string?.split("\n")?.distinct() ?: listOf()
	}

	fun convertFromListToString(list: List<String>): String
	{
		var newString = ""
		val size = list.size
		for(i in 0 ..< size)
		{
			newString += if(i != size - 1)
			{
				list[i] + "\n"
			}
			else
			{
				list[i]
			}
		}
		return newString
	}

	fun returnStringOfList(): String
	{
		return convertFromListToString(picturesUiState.value.picturesUrl)
	}

	fun postDeletedUrls(urls: List<String>)
	{
		val state = picturesUiState
		state.value = state.value.copy(deletedUrls = urls)
	}

	private fun postPivotsXandY(pairOfPivots: Pair<Float, Float>)
	{
		pairOfPivotsXandY = pairOfPivots
	}

	fun getPivotsXandY(): Pair<Float, Float>
	{
		return pairOfPivotsXandY
	}

	private fun compareAndCombineLists(list1: List<String>, list2: List<String>): List<String>
	{
		return list1.filterNot { it in list2 } // Фильтруем первый список, оставляя только элементы, которых нет во втором
	}

	fun postParamsOfScreen(gridNum: Int, width: Int, height: Int, density: Float)
	{
		gridQuantity = gridNum
		screenHeight = height
		screenWidth = width
		densityOfScreen = density
	}

	fun updateGridSpan(newSpan: Int)
	{
		Log.d("calculator1", "updated grid num")
		gridQuantity = newSpan
	}

	fun calculatePosition(url: String)
	{
		urlForCalculation = url
		val list = picturesUiState.value.picturesUrl
		val gridQuantity = gridQuantity
		//вычисляем позицию в формате таблицы
		if(mapOfColumns.isEmpty())
		{
			viewModelScope.launch {
				for(i in list.indices)
				{
					var column: Int
					val line = ceil((i + 1).toFloat() / gridQuantity.toFloat()).toInt()
					column = (i) % gridQuantity
					if(column == 0)
					{
						column = gridQuantity
					}
					Log.d("column", "$column")
					mapOfColumns[list[i]] = column
					mapOfLines[list[i]] = line
				}
			}
		}
		calculatePixelPosition(url)
	}

	private fun calculatePixelPosition(
		url: String,
	)
	{
		val urls = picturesUiState.value.picturesUrl
		val positionInPx = listOfPositions[urls.indexOf(url)]

		Log.d("watafak x", "${positionInPx.first} / ${screenWidth.toFloat()} = ${positionInPx.first / screenWidth.toFloat()}")
		Log.d("watafak y", "$${positionInPx.second} / ${screenHeight.toFloat()} = ${positionInPx.second / screenHeight.toFloat()}")
		var x = positionInPx.first / screenWidth.toFloat()
		var y = positionInPx.second / screenHeight.toFloat()
		if(screenWidth < screenHeight)
		{
			postPivotsXandY(Pair(x * 1.4f, y * 1.37f))
		}
		else
		{
			if(cutouts.first != 0f) {
				if(positionInPx.first < 100f)
				{
					x = -0.05f
				}
				else if(positionInPx.first <= 2126f && positionInPx.first>1826f)
				{
					x = 1.12f
				}
				else if(positionInPx.first <= 1826f && positionInPx.first>1226f)
				{
					x = 0.96f
				}
				else if(positionInPx.first <= 1226f && positionInPx.first>926f)
				{
					x = 0.795f
				}
				else if(positionInPx.first <= 926f && positionInPx.first>626f )
				{
					x = 0.455f
				}
				else if(positionInPx.first <= 626f && positionInPx.first>326f)
				{
					x = 0.29f
				}
				else if(positionInPx.first <= 326f && positionInPx.first> 100f)
				{
					x = 0.12f
				}
				else
				{
					x = 0.6f
				}
				if(positionInPx.second <= 26)
				{
					y += 0.2f
				}
				else
				{
					y *= 2f
				}
				postPivotsXandY(Pair(x, y))
				Log.d("cutout sleva", "${cutouts.first}")
			} else {
				cutouts.second
				Log.d("cutout sprava", "${cutouts.second}")
			}
		}
	}

	fun calculateListPosition(url: String)
	{
		val list = picturesUiState.value.picturesUrl
		val column = mapOfColumns[url]
		val positionInPx = listOfPositions[column!!]
		//val minVisibleLine = mapOfLines[list[firstVisibleItemIndex]]!!
		//val maxVisibleLine = minVisibleLine + (numOfVisibleElements / gridQuantity)
		if(url != urlForCalculation)
		{
			clickOnPicture(list.indexOf(url), 0)
			var x = positionInPx.first / screenWidth.toFloat()
			val y = positionInPx.second / screenHeight.toFloat()
			if(screenWidth < screenHeight)
			{
				postPivotsXandY(Pair(x * 1.4f, y * 1.37f))
			}
			else
			{
				if(positionInPx.first < 100f)
				{
					x = -0.08f
				}
				else
				{
					x += 0.05f * (mapOfColumns[url]?.minus(1)!!)
				}
				postPivotsXandY(Pair(x, y + 0.25f))
			}
		}
		Log.d("wtfwtf", "$column, $positionInPx")
	}

	fun postInitialPage(page: Int)
	{
		initialPage = page
	}

	fun postGridSize(sizeInPx: Int)
	{
		sizeOfGridInPixels = sizeInPx
	}

	fun postPosition(url: String, position: Pair<Float, Float>)
	{
		val urls = picturesUiState.value.picturesUrl
		if(position != Pair(0f, 0f))
		{
			listOfPositions.add(urls.indexOf(url), Pair(1f, 1f))
			listOfPositions[urls.indexOf(url)] = position
		}
	}

	fun postGridParams(index: Int, maxVisibleElements: Int)
	{
		firstVisibleItemIndex = index
		numOfVisibleElements = maxVisibleElements
	}

	fun postCutouts(left: Float, right: Float)
	{
		cutouts = Pair(left, right)
	}
}