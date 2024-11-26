package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavItem
import com.example.gridpics.ui.activity.MainActivity
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.activity.MainActivity.Companion.HTTP_ERROR
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURE
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFERENCE_GRIDPICS
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@SuppressLint("RestrictedApi", "CommitPrefEdits", "ApplySharedPref", "UseCompatLoadingForDrawables", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
	navController: NavController,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	removeSpecialError: (String) -> Unit,
	postDefaultUrl: () -> Unit,
	changeVisabilityState: () -> Unit,
	postUrl: (String, String) -> Unit,
	postPositiveState: () -> Unit,
	picturesScreenState: MutableState<PicturesScreenUiState>,
	pic: String?,
	isValidUrl: (String) -> Boolean,
	convertPicture: (Bitmap) -> String,
	window: Window,
)
{
	val context = LocalContext.current
	BackHandler {
		if(!state.value.barsAreVisible)
		{
			Log.d("we are out", "We are out")
			postDefaultUrl.invoke()
			changeVisabilityState.invoke()
			navController.navigateUp()
		}
		else
		{
			Log.d("we are out", "We are without changing state")
			postDefaultUrl.invoke()
			navController.navigateUp()
		}
	}
	val isVisible = remember { mutableStateOf(true) }
	val pictures = remember { picturesScreenState.value.picturesUrl }
	if(pic != null && pictures != null)
	{
		Log.d("pic", "$pic")
		val list = remember(pic) { pictures.split("\n").toMutableList() }
		val pagerState = rememberPagerState(initialPage = list.indexOf(pic), pageCount = { list.size })
		val bitmapString = remember(pagerState) { mutableStateOf(DEFAULT_STRING_VALUE) }
		if(checkIfExists(list[pagerState.currentPage]))
		{
			val picture = ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap()
			Log.d("checkMa", "gruzim oshibku")
			bitmapString.value = convertPicture(picture!!)
		}
		else
		{
			val imgRequest = remember(list[pagerState.currentPage]) {
				ImageRequest.Builder(context)
					.data(list[pagerState.currentPage])
					.placeholder(R.drawable.loading)
					.error(R.drawable.error)
					.allowHardware(false)
					.target {
						val picture = it.toBitmap()
						Log.d("checkMa", "gruzim pic")
						bitmapString.value = convertPicture(picture)
					}
					.networkCachePolicy(CachePolicy.ENABLED)
					.diskCachePolicy(CachePolicy.ENABLED)
					.diskCacheKey(list[pagerState.currentPage])
					.memoryCachePolicy(CachePolicy.ENABLED)
					.build()
			}
			ImageLoader(context).newBuilder().build().enqueue(imgRequest)
		}
		postUrl(list[pagerState.currentPage], bitmapString.value)
		Scaffold(
			contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
			topBar = { AppBar(isVisible, context, navController, list, pagerState, postDefaultUrl) },
			content = { padding ->
				ShowDetails(
					img = pic,
					navController = navController,
					isVisible = isVisible,
					list = list,
					pagerState = pagerState,
					context = context,
					padding = padding,
					checkIfExists = checkIfExists,
					addError = addError,
					removeSpecialError = removeSpecialError,
					changeVisabilityState = changeVisabilityState,
					postPositiveState = postPositiveState,
					multiWindowed = state,
					isValidUrl = isValidUrl,
					window = window
				)
			}
		)
	}
}

@SuppressLint("CoroutineCreationDuringComposition", "UseCompatLoadingForDrawables")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowDetails(
	img: String,
	navController: NavController,
	isVisible: MutableState<Boolean>,
	list: MutableList<String>,
	pagerState: PagerState,
	context: Context,
	padding: PaddingValues,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	multiWindowed: MutableState<DetailsScreenUiState>,
	changeVisabilityState: () -> Unit,
	postPositiveState: () -> Unit,
	isValidUrl: (String) -> Boolean,
	window: Window,
)
{
	padding.calculateBottomPadding()
	val firstPage = remember { mutableStateOf(true) }
	val startPage = list.indexOf(img)
	val exit = remember { mutableStateOf(false) }
	val topBarHeight = 64.dp
	val scope = rememberCoroutineScope()
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()

	HorizontalPager(
		state = pagerState,
		pageSize = PageSize.Fill,
		contentPadding = PaddingValues(0.dp, topBarHeight + statusBarHeightFixed, 0.dp, WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()),
		userScrollEnabled = true
	) { page ->
		if(firstPage.value)
		{
			scope.launch {
				pagerState.animateScrollToPage(startPage)
			}
		}
		firstPage.value = false
		when
		{
			checkIfExists(list[page]) ->
			{
				ShowError(
					context = context,
					list = list,
					currentPage = page,
					pagerState = pagerState,
					isValidUrl = isValidUrl
				)
			}
			!checkIfExists(list[page]) ->
			{
				ShowAsynchImage(
					list = list,
					page = page,
					addError = addError,
					removeSpecialError = removeSpecialError,
					changeVisabilityState = changeVisabilityState,
					postPositiveState = postPositiveState,
					navController = navController,
					isVisible = isVisible,
					exit = exit,
					multiWindow = multiWindowed,
					context = context,
					window = window
				)
			}
		}
		MainActivity.countExitNavigation++
		saveToSharedPrefs(context, list[pagerState.currentPage])
	}
}

@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun ShowAsynchImage(
	list: MutableList<String>,
	page: Int,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	changeVisabilityState: () -> Unit,
	postPositiveState: () -> Unit,
	navController: NavController,
	isVisible: MutableState<Boolean>,
	exit: MutableState<Boolean>,
	multiWindow: MutableState<DetailsScreenUiState>,
	context: Context,
	window: Window,
)
{
	val orientation = context.resources.configuration.orientation
	val scale = if(!multiWindow.value.isMultiWindowed)
	{
		if(orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			ContentScale.FillWidth
		}
		else
		{
			ContentScale.FillHeight
		}
	}
	else
	{
		ContentScale.Fit
	}
	val zoom = rememberZoomState(2.8f, Size.Zero)
	val count = remember { listOf(0).toMutableList() }
	val countLastThree = remember { listOf(0).toMutableList() }
	val imgRequest = remember(list[page]) {
		ImageRequest.Builder(context)
			.data(list[page])
			.placeholder(R.drawable.loading)
			.error(R.drawable.loading)
			.allowHardware(false)
			.diskCacheKey(list[page])
			.networkCachePolicy(CachePolicy.ENABLED)
			.build()
	}
	val scope = rememberCoroutineScope()
	AsyncImage(
		model = imgRequest,
		contentDescription = "",
		contentScale = scale,
		onSuccess = {
			removeSpecialError(list[page])
		},
		onError = {
			addError(list[page])
			navController.navigate(Screen.Details.route)
		},
		modifier = Modifier
			.fillMaxSize()
			.zoomable(zoom, enableOneFingerZoom = false, onTap = {
				changeVisabilityState.invoke()
				val controller = WindowCompat.getInsetsController(window, window.decorView)
				scope.launch {
					if(!isVisible.value)
					{
						controller.hide(WindowInsetsCompat.Type.statusBars())
						controller.hide(WindowInsetsCompat.Type.navigationBars())
					}
					else
					{
						controller.show(WindowInsetsCompat.Type.statusBars())
						controller.show(WindowInsetsCompat.Type.navigationBars())
					}
				}
				isVisible.value = !isVisible.value
			})
			.pointerInput(Unit) {
				awaitEachGesture {
					while(true)
					{
						val event = awaitPointerEvent()
						exit.value = !event.changes.any {
							it.isConsumed
						}
						if(count.size >= 3)
						{
							countLastThree.add(count[count.lastIndex])
							countLastThree.add(count[count.lastIndex - 1])
							countLastThree.add(count[count.lastIndex - 2])
						}
						if(event.changes.any { !it.pressed })
						{
							if(zoom.scale < 0.92.toFloat() && exit.value && countLastThree.max() == 2)
							{
								postPositiveState.invoke()
								navController.navigateUp()
							}
						}
						countLastThree.clear()
						count.add(event.changes.size)
					}
				}
			}
	)
}

@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun ShowError(
	context: Context,
	list: MutableList<String>,
	currentPage: Int,
	pagerState: PagerState,
	isValidUrl: (String) -> Boolean,
)
{
	val errorMessage = if(isValidUrl(list[currentPage]))
	{
		HTTP_ERROR
	}
	else
	{
		stringResource(R.string.link_is_not_valid)
	}
	Column(
		modifier = Modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = stringResource(R.string.error_ocurred_loading_img),
			modifier = Modifier.padding(5.dp),
			color = MaterialTheme.colorScheme.onPrimary)

		Text(
			text = errorMessage,
			modifier = Modifier.padding(10.dp),
			color = MaterialTheme.colorScheme.onPrimary
		)
		if(errorMessage != stringResource(R.string.link_is_not_valid))
		{
			val textButton = stringResource(R.string.reload_pic)
			Button(
				onClick =
				{
					Toast.makeText(context, textButton, Toast.LENGTH_LONG).show()
					CoroutineScope(Dispatchers.Main).launch {
						pagerState.animateScrollToPage(currentPage)
					}
				},
				colors = ButtonColors(Color.LightGray, Color.Black, Color.Black, Color.White)) {
				Text(stringResource(R.string.update_loading))
			}
		}
	}
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
	isVisible: MutableState<Boolean>,
	context: Context, nc: NavController,
	list: MutableList<String>,
	pagerState: PagerState,
	postDefaultUrl: () -> Unit,
)
{
	var navBack by remember { mutableStateOf(false) }
	AnimatedVisibility(visible = isVisible.value, enter = EnterTransition.None, exit = ExitTransition.None) {
		Box(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.background)
				.height(WindowInsets.systemBarsIgnoringVisibility
					.asPaddingValues()
					.calculateTopPadding())
				.fillMaxWidth())
		TopAppBar(
			modifier = Modifier
				.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
				.wrapContentSize()
				.clickable {
					navBack = true
				},
			title = {
				Text(
					text = list[pagerState.currentPage],
					fontSize = 18.sp,
					maxLines = 2,
					modifier = Modifier
						.clickable { navBack = true }
						.padding(0.dp, 3.dp, 0.dp, 0.dp),
					overflow = TextOverflow.Ellipsis,
				)
			},
			navigationIcon = {
				IconButton({ navBack = true }) {
					Icon(
						Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = "back",
						modifier = Modifier.wrapContentSize()
					)
				}
			},
			colors = TopAppBarDefaults.topAppBarColors(
				titleContentColor = MaterialTheme.colorScheme.onPrimary,
				navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
				actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
				containerColor = MaterialTheme.colorScheme.background
			),
			actions = {
				val plain = stringResource(R.string.text_plain)
				IconButton(
					onClick =
					{
						share(list[pagerState.currentPage], context, plain)
					}
				) {
					Icon(
						Icons.Default.Share,
						contentDescription = "share",
						tint = MaterialTheme.colorScheme.onPrimary,
					)
				}
			}
		)
		if(navBack)
		{
			postDefaultUrl.invoke()
			navBack = false
			nc.navigate(BottomNavItem.Home.route)
		}
	}
}

private fun saveToSharedPrefs(context: Context, pictureUrl: String)
{
	val pic = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
	val editor = pic.edit()
	editor.putString(PICTURE, pictureUrl)
	editor.apply()
}

fun share(text: String, context: Context, plain: String)
{
	val sendIntent = Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = plain
	}
	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(context, shareIntent, null)
}