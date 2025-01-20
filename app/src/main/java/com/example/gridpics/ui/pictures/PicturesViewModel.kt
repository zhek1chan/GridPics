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
		state.value = state.value.copy(index = index, offset = offset)
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
		val index = list.indexOf(url) + 1
		//вычисляем позицию в формате таблицы
		var column: Int
		val line: Float = ceil(index.toFloat() / gridQuantity.toFloat())
		column = index % gridQuantity
		if(column == 0)
		{
			column = gridQuantity
		}
		calculatePixelPosition(line.toInt(), column, true)
	}

	private fun calculatePixelPosition(
		line: Int,
		column: Int,
		useReCalc: Boolean,
	)
	{
		Log.d("Calc check", "${densityOfScreen / 2 - 0.05}")
		val screenWidth = screenWidth
		val screenHeight = screenHeight
		val x = if ((screenWidth > screenHeight) && (column == 1))
		{
			Log.d("calc", "fsbfusbfsmfsmlf;sfs")
			1f
		} else if (screenWidth > screenHeight) {
			2.5f * (column - 1) + 1.0f
		} else if (column == 1) {
			0.00f
		} else if (column == 2) {
			2.5f
		} else
		{
			column * 1.8f
		}
		val numOfVisibleLines = (sizeOfGridInPixels / densityOfScreen / 110).toInt()
		var k = 0
		var maxK = 0
		for(i in 1 .. 9000)
		{
			k = line / (numOfVisibleLines * i)
			if(k > maxK)
			{
				maxK = k
			}
			if(k <= 1)
			{
				break
			}
		}
		Log.d("Calc check", "max k = $maxK")
		val value = picturesUiState.value
		Log.d("Calc check", "k = $k")
		var nLine = if(!useReCalc)
		{
			line
		}
		else if(line > numOfVisibleLines)
		{
			line - numOfVisibleLines * maxK
		}
		else
		{
			line
		}
		val firstVisibleIndex = value.index
		picturesUiState.value = value.copy(index = firstVisibleIndex, offset = 0)
		val indexOfClickedPic = value.picturesUrl.indexOf(urlForCalculation)
		Log.d("Calc", "seichas nLine = $nLine")
		if(nLine == 0 || line * gridQuantity >= value.picturesUrl.size)
		{
			nLine = line - numOfVisibleLines * (maxK - 1)
		}
		if ((firstVisibleIndex < indexOfClickedPic) && (indexOfClickedPic - firstVisibleIndex > 2) && useReCalc && maxK != 0)
		{
			Log.d("Calc check", "indexOfClickedPic - firstVisibleIndex / gridQuantity")
			Log.d("Calc2 check", "($indexOfClickedPic - $firstVisibleIndex )/ $gridQuantity")
			val nY = ((indexOfClickedPic - firstVisibleIndex) / gridQuantity)
			nLine -= 1
			if(nY == 1)
			{
				Log.d("Calc2", "$nLine = 1")
				nLine = 1
			} else if (nY < nLine && (nY + 1) != nLine && nLine - nY > 2)
			{
				Log.d("Calc2", "$nLine - $nY + 1")
				nLine = nLine - nY - 1
			}
			else
			{
				Log.d("Calc2", "$nLine")
				nLine = nY
			}
			Log.d("Calc check", "nLine recalculated")
		} else if (maxK != 0) {
			nLine = 0
		} else {
			nLine -= 1
		}

		val y = if (nLine == 0)
		{
			0.4f
		}
		else if(screenWidth > screenHeight)
		{
			Log.d("Calc check", "screenWidth > screenHeight")
			(nLine) * 1.5f
		} else if (nLine == 1) {
			2.2f
		}
		else
		{
			(nLine + 1) * 1.4f
		}
		Log.d("Calc2 check", "we have got line = $nLine")
		Log.d("calc check", "x = $x, y = $y")
		postPivotsXandY(Pair(x, y))
	}

	fun calculateListPosition(url: String)
	{
		val pics = picturesUiState.value.picturesUrl
		val gridQuantity = gridQuantity
		val numOfVisibleLines = (sizeOfGridInPixels / densityOfScreen / 110).toInt()
		var numOfLastLines = pics.size / gridQuantity - numOfVisibleLines
		val index = pics.indexOf(url)
		if (pics.size % gridQuantity != 0) {
			numOfLastLines += 1
		}
		if (initialPage != index) {
			if ((index + 1) / gridQuantity >= numOfVisibleLines) {
				val cof = ((index + 1) / gridQuantity / numOfVisibleLines)
				val currRealLine = if (cof <= 1) {
					(index + 1) / gridQuantity - (numOfVisibleLines + 1)
				} else {
					(index + 1) / gridQuantity - numOfVisibleLines * cof
				}
				clickOnPicture(pics.indexOf(url) + 1, 0)
				Log.d("Calc check", "line = $currRealLine")
				calculatePixelPosition(currRealLine, index % gridQuantity + 1, false)
			} else if (index > gridQuantity) {
				Log.d("Calc check", "another type 1")
				var column = 0
				for (i in 0..<gridQuantity) {
					if (index % gridQuantity == i) {
						clickOnPicture(pics.indexOf(url) - gridQuantity + 1 + i, 0)
						column = i + 1
					}
				}
				calculatePixelPosition(0, column, false)
			} else {
				Log.d("Calc check", "another type 2")
				calculatePixelPosition(0, index + 1, false)
			}
		}
	}

	fun postInitialPage(page: Int)
	{
		initialPage = page
	}

	fun postGridSize(sizeInPx: Int)
	{
		sizeOfGridInPixels = sizeInPx
	}
}