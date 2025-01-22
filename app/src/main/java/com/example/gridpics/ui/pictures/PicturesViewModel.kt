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
		val index = list.indexOf(url) + 1
		//вычисляем позицию в формате таблицы
		var column: Int
		val line: Float = ceil(index.toFloat() / gridQuantity.toFloat())
		column = index % gridQuantity
		if(column == 0)
		{
			column = gridQuantity
		}
		Log.d("column", "$column")
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
		val gridQuantity = gridQuantity
		val x = if((screenWidth > screenHeight) && (column == 0))
		{
			0.3f
		}
		else if((screenWidth > screenHeight) && (column == 1))
		{
			1.3f
		}
		else if(screenWidth > screenHeight)
		{
			3.5f * (column - 1) + 1.3f
		}
		else if(column == 1)
		{
			0.02f
		}
		else if(column == 2)
		{
			1.1f
		}
		else if(column == 3)
		{
			2.2f
		}
		else
		{
			column * 1.7f
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
		if(useReCalc)
		{
			clickOnPicture(firstVisibleIndex, 0)
		}
		val indexOfClickedPic = value.picturesUrl.indexOf(urlForCalculation)
		if(nLine == 0 || line * gridQuantity >= value.picturesUrl.size)
		{
			nLine = line - (numOfVisibleLines - 1) * (maxK - 1)
			if(value.picturesUrl.size - indexOfClickedPic <= 3)
			{
				Log.d("calccalc", "clicked on last")
				clickOnPicture(value.picturesUrl.size - 1, 0)
			}
		}
		Log.d("CalcCalcСalc", "firstVisibleIndex = $firstVisibleIndex , maxK = $maxK")
		if((firstVisibleIndex < indexOfClickedPic) && (indexOfClickedPic - firstVisibleIndex > 2) && useReCalc && maxK != 0)
		{
			Log.d("qazwsx", "line in range ${line in value.picturesUrl.size / gridQuantity - numOfVisibleLines - 1 .. value.picturesUrl.size / gridQuantity}")
			val nY = ((indexOfClickedPic - firstVisibleIndex) / gridQuantity)
			nLine -= 1
			nLine = if(nY == 1)
			{
				Log.d("qazwsx", "nLine = 1")
				1
			}
			else if(nY < nLine && (nY + 1) != nLine && nLine - nY > 2 && line !in value.picturesUrl.size / gridQuantity - numOfVisibleLines - 1 .. value.picturesUrl.size / gridQuantity)
			{
				nY
			}
			else if((nY < nLine && (nY + 1) != nLine && nLine - nY > 2 && line in value.picturesUrl.size / gridQuantity - numOfVisibleLines - 1 .. value.picturesUrl.size / gridQuantity))
			{
				Log.d("qazwsx", "sidim s bobrim za stolom")
				nLine - 5
			}
			else
			{
				Log.d("qazwsx", "nLine = nY")
				nY
			}
		}
		else if(useReCalc && maxK != 0)
		{
			nLine = 0
		}
		else if(firstVisibleIndex != 0 && maxK == 0 && useReCalc)
		{
			if(indexOfClickedPic - firstVisibleIndex <= 2)
			{
				nLine = 0
			}
			else if(indexOfClickedPic - firstVisibleIndex > 2)
			{
				ceil(((indexOfClickedPic - firstVisibleIndex) / gridQuantity).toDouble()).toInt()
				nLine = ceil(((indexOfClickedPic - firstVisibleIndex) / gridQuantity).toDouble()).toInt()
			}
		}
		else if(maxK != 0)
		{
			Log.d("CalcCalcСalc", "Srabotalo nLine = 0 v1")
			nLine = 0
			clickOnPicture(indexOfClickedPic, 0)
		}
		else if(!useReCalc && indexOfClickedPic - firstVisibleIndex <= 2)
		{
			Log.d("CalcCalcСalc", "Srabotalo nLine = 0 v2")
			nLine = 0
			clickOnPicture(indexOfClickedPic, 0)
		}
		else if(!useReCalc && indexOfClickedPic - firstVisibleIndex > 2)
		{
			Log.d("CalcCalcСalc", "Srabotalo nLine = 0 v3")
			nLine = 0
			clickOnPicture(indexOfClickedPic, 0)
		}
		else if(!useReCalc && indexOfClickedPic - firstVisibleIndex > 2)
		{
			Log.d("CalcCalcСalc", "Srabotalo nLine = 1")
			nLine = 1
			clickOnPicture(indexOfClickedPic, 0)
		}
		else if(useReCalc)
		{
			Log.d("CalcCalcСalc", "nLine - 1")
			nLine -= 1
		}
		Log.d("CalcCalc", "numOfVisibleLines = $numOfVisibleLines")
		if(nLine == numOfVisibleLines - 1)
		{
			clickOnPicture(firstVisibleIndex + 3, 0)
			nLine -= 1
		}
		if (line == 99999999 && !useReCalc){
			nLine = 4
		}
		val y = if(screenWidth > screenHeight && nLine <= 0)
		{
			0.8f
		}
		else if(screenWidth > screenHeight)
		{
			(nLine) * 3.4f
		}
		else if(nLine == 0 && screenWidth < screenHeight)
		{
			0.6f
		}
		else if(nLine == 1 && screenWidth < screenHeight)
		{
			1.53f
		}
		else if(nLine == 2 && screenWidth < screenHeight)
		{
			2.55f
		}
		else
		{
			(nLine + 1) * 0.9f
		}
		Log.d("CalcCalcСalc", "calculated line = $nLine")
		postPivotsXandY(Pair(x, y))
	}

	fun calculateListPosition(url: String)
	{
		val value = picturesUiState.value
		val pics = value.picturesUrl
		val gridQuantity = gridQuantity
		val numOfVisibleLines = (sizeOfGridInPixels / densityOfScreen / 110).toInt()
		var numOfLastLines = pics.size / gridQuantity - numOfVisibleLines
		val index = pics.indexOf(url)
		urlForCalculation = url
		if(pics.size % gridQuantity != 0)
		{
			numOfLastLines += 1
		}
		if(initialPage != index)
		{
			val line = ceil(((pics.indexOf(url)) / gridQuantity).toDouble())
			val maxLine = ceil(((pics.size - 1) / gridQuantity).toDouble())
			Log.d("CalcCalc", "maxLine = $maxLine, line = $line")
			Log.d("CalcCalc", "line = $line, size = ${pics.size}")
			if((line + numOfVisibleLines - 1) * gridQuantity >= pics.size)
			{
				Log.d("CalcCalc", "line = $line, size = ${pics.size}")
				var k: Int
				var maxK = 0
				for(i in 1 .. 9000)
				{
					k = line.toInt() / (numOfVisibleLines * i)
					if(k > maxK)
					{
						maxK = k
					}
					if(k <= 1)
					{
						break
					}
				}
				val endOfLines = if(line.toInt() - numOfVisibleLines * (maxK - 1) > numOfVisibleLines)
				{
					line.toInt() - numOfVisibleLines * (maxK)
				}
				else
				{
					line.toInt() - numOfVisibleLines * (maxK - 1)
				}
				Log.d("CalcCalc", "line1 = ${endOfLines + 1}, size = ${pics.size}")
				if(line == maxLine)
				{
					Log.d("CalcCalc", "britney spyrs")
					calculatePixelPosition(99999999, index % gridQuantity + 1, false)
				}
				else
				{
					calculatePixelPosition(endOfLines + 1, index % gridQuantity + 1, true)
				}
			}
			else if((index + 1) / gridQuantity >= numOfVisibleLines)
			{
				Log.d("CalcCalc", "v2 line = $line, size = ${pics.size}")
				val cof = ((index + 1) / gridQuantity / numOfVisibleLines)
				val currRealLine = if(cof <= 1)
				{
					(index + 1) / gridQuantity - (numOfVisibleLines + 1)
				}
				else
				{
					(index + 1) / gridQuantity - numOfVisibleLines * cof
				}
				clickOnPicture(pics.indexOf(url) + 1, 0)
				calculatePixelPosition(currRealLine, index % gridQuantity + 1, false)
			}
			else if(index > gridQuantity)
			{
				Log.d("CalcCalc", "v3 line = $line, size = ${pics.size}")
				Log.d("Calc check", "another type 1")
				var column = 0
				for(i in 0 ..< gridQuantity)
				{
					if(index % gridQuantity == i)
					{
						clickOnPicture(pics.indexOf(url) - gridQuantity + 1 + i, 0)
						column = i + 1
					}
				}
				calculatePixelPosition(0, column, false)
				clickOnPicture(index = pics.indexOf(url), offset = 0)
			}
			else
			{
				calculatePixelPosition(0, index + 1, false)
				clickOnPicture(index = pics.indexOf(url), offset = 0)
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