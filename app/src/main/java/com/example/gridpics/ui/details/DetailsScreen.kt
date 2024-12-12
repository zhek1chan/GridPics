package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
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
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.error
import coil3.request.placeholder
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavItem
import com.example.gridpics.ui.activity.MainActivity.Companion.DEFAULT_STRING_VALUE
import com.example.gridpics.ui.activity.MainActivity.Companion.HTTP_ERROR
import com.example.gridpics.ui.activity.MainActivity.Companion.TEXT_PLAIN
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
	navController: NavController,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	removeSpecialError: (String) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	postPositiveState: () -> Unit,
	picturesScreenState: MutableState<PicturesScreenUiState>,
	isValidUrl: (String) -> Boolean,
	changeBarsVisability: (Boolean) -> Unit,
	postNewBitmap: (String) -> Unit,
	saveCurrentPictureUrl: (String) -> Unit,
)
{
	val context = LocalContext.current
	BackHandler {
		changeBarsVisability(true)
		postUrl(DEFAULT_STRING_VALUE, null)
		navController.navigate(Screen.Home.route) {
			popUpTo(navController.graph.findStartDestination().id)
			restoreState = true
			launchSingleTop = true
		}
	}
	val valuePicUi = picturesScreenState.value
	val currentPicture = valuePicUi.currentPicture
	val pictures = remember { valuePicUi.picturesUrl }
	if(pictures != null)
	{
		Log.d("pic", currentPicture)
		val list = remember { pictures.split("\n") }
		Log.d("list", "$list")
		val pagerState = rememberPagerState(initialPage = list.indexOf(currentPicture), initialPageOffsetFraction = 0f, pageCount = { list.size })
		val currentPage = pagerState.currentPage
		val errorPicture = remember { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }
		val value = state.value

		LaunchedEffect(currentPage) {
			val pic = list[currentPage]
			saveCurrentPictureUrl(pic)
			if(checkIfExists(pic))
			{
				Log.d("checkMa", "gruzim oshibku")
				postUrl(pic, errorPicture)
			}
			else
			{
				postNewBitmap(pic)
			}
		}
		Scaffold(
			contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
			topBar = {
				AppBar(
					isVisible = value.barsAreVisible,
					context = context,
					nc = navController,
					list = list,
					pagerState = pagerState,
					postUrl = postUrl,
				)
			},
			content = { padding ->
				ShowDetails(
					navController = navController,
					list = list,
					pagerState = pagerState,
					context = context,
					checkIfExists = checkIfExists,
					addError = addError,
					removeSpecialError = removeSpecialError,
					state = state,
					postPositiveState = postPositiveState,
					isValidUrl = isValidUrl,
					padding = padding,
					changeBarsVisability = changeBarsVisability,
					postUrl = postUrl
				)
			}
		)
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowDetails(
	navController: NavController,
	list: List<String>,
	pagerState: PagerState,
	context: Context,
	checkIfExists: (String) -> Boolean,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	postPositiveState: () -> Unit,
	isValidUrl: (String) -> Boolean,
	padding: PaddingValues,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
)
{
	val topBarHeight = 64.dp
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	Log.d("padding1", "sverhu ${padding.calculateTopPadding().value.dp}")
	Log.d("padding11", "sverhu 2 $statusBarHeightFixed")

	HorizontalPager(
		state = pagerState,
		pageSize = PageSize.Fill,
		contentPadding = PaddingValues(0.dp, statusBarHeightFixed + topBarHeight, 0.dp, padding.calculateBottomPadding()),
		userScrollEnabled = true,
		pageSpacing = 10.dp
	) { page ->
		if(checkIfExists(list[page]))
		{
			ShowError(
				context = context,
				list = list,
				currentPage = page,
				pagerState = pagerState,
				isValidUrl = isValidUrl
			)
		}
		else
		{
			ShowAsynchImage(
				list = list,
				page = page,
				addError = addError,
				removeSpecialError = removeSpecialError,
				postPositiveState = postPositiveState,
				navController = navController,
				state = state,
				context = context,
				changeBarsVisability = changeBarsVisability,
				postUrl = postUrl
			)
		}
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ShowAsynchImage(
	list: List<String>,
	page: Int,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	postPositiveState: () -> Unit,
	navController: NavController,
	state: MutableState<DetailsScreenUiState>,
	context: Context,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
)
{
	val orientation = context.resources.configuration.orientation
	val scale = if(state.value.isMultiWindowed)
	{
		ContentScale.Fit
	}
	else
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
	val zoom = rememberZoomState(15f, Size.Zero)
	var imageSize by remember { mutableStateOf(Size.Zero) }
	val img = list[page]
	val imgRequest = remember(img) {
		ImageRequest.Builder(context)
			.data(img)
			.placeholder(R.drawable.loading)
			.error(R.drawable.loading)
			.allowHardware(false)
			.diskCacheKey(img)
			.networkCachePolicy(CachePolicy.ENABLED)
			.build()
	}
	val scope = rememberCoroutineScope()

	AsyncImage(
		model = imgRequest,
		contentDescription = null,
		contentScale = scale,
		onSuccess = {
			val resultImage = it.result.image
			imageSize = Size(resultImage.width.toFloat(), resultImage.height.toFloat())
			removeSpecialError(list[page])
		},
		onError = {
			addError(list[page])
			navController.navigate(Screen.Details.route)
		},
		modifier = Modifier
			.fillMaxSize()
			.zoomable(
				zoomState = zoom,
				enableOneFingerZoom = false,
				onTap =
				{
					val visibility = state.value.barsAreVisible
					changeBarsVisability(visibility)
				}
			)
			.pointerInput(Unit) {
				awaitEachGesture {
					val count = mutableListOf(0)
					val countLastThree = mutableListOf(0)
					while(true)
					{
						val event = awaitPointerEvent()
						val changes = event.changes
						val exit = !changes.any {
							it.isConsumed
						}
						if(count.size >= 3)
						{
							countLastThree.add(count[count.lastIndex])
							countLastThree.add(count[count.lastIndex - 1])
							countLastThree.add(count[count.lastIndex - 2])
						}
						if(changes.any { !it.pressed })
						{
							if(zoom.scale < 0.92.toFloat() && exit && countLastThree.max() == 2)
							{
								postUrl(DEFAULT_STRING_VALUE, null)
								postPositiveState()
								changeBarsVisability(true)
								navController.navigate(Screen.Home.route)
							}
						}
						countLastThree.clear()
						count.add(changes.size)
					}
				}
			}
	)

	scope.launch {
		zoom.setContentSize(imageSize)
	}
}

@Composable
fun ShowError(
	context: Context,
	list: List<String>,
	currentPage: Int,
	pagerState: PagerState,
	isValidUrl: (String) -> Boolean,
)
{
	val linkIsNotValid = stringResource(R.string.link_is_not_valid)
	val scope = rememberCoroutineScope()
	val errorMessage = if(isValidUrl(list[currentPage]))
	{
		HTTP_ERROR
	}
	else
	{
		linkIsNotValid
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
		if(errorMessage != linkIsNotValid)
		{
			val textButton = stringResource(R.string.reload_pic)
			Button(
				onClick =
				{
					Toast.makeText(context, textButton, Toast.LENGTH_LONG).show()
					scope.launch {
						pagerState.animateScrollToPage(currentPage)
					}
				},
				colors = ButtonColors(Color.LightGray, Color.Black, Color.Black, Color.White))
			{
				Text(text = stringResource(R.string.update_loading))
			}
		}
	}
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
	isVisible: Boolean,
	context: Context, nc: NavController,
	list: List<String>,
	pagerState: PagerState,
	postUrl: (String, Bitmap?) -> Unit,
)
{
	val navBack = remember { mutableStateOf(false) }
	val screenWidth = LocalConfiguration.current.screenWidthDp.dp
	Log.d("wahwah", "$screenWidth")
	AnimatedVisibility(visible = isVisible, enter = EnterTransition.None, exit = ExitTransition.None) {
		Box(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.background)
				.height(
					WindowInsets.systemBarsIgnoringVisibility
						.asPaddingValues()
						.calculateTopPadding() + 64.dp
				)
				.fillMaxWidth())
		TopAppBar(
			modifier = Modifier
				.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout))
				.wrapContentSize(),
			title = {
				Text(
					text = list[pagerState.currentPage],
					fontSize = 18.sp,
					maxLines = 2,
					modifier = Modifier
						.clickable { navBack.value = true }
						.padding(10.dp, 0.dp, 0.dp, 0.dp),
					overflow = TextOverflow.Ellipsis,
				)
			},
			navigationIcon = {
				IconButton(
					modifier = Modifier
						.size(30.dp, 30.dp)
						.padding(5.dp, 0.dp, 0.dp, 0.dp),
					onClick = { navBack.value = true }
				)
				{
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = "back",
						modifier = Modifier
							.size(50.dp, 24.dp)
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
				IconButton(
					onClick =
					{
						share(list[pagerState.currentPage], context, TEXT_PLAIN)
					}
				) {
					Icon(
						imageVector = Icons.Default.Share,
						contentDescription = "share",
						tint = MaterialTheme.colorScheme.onPrimary,
					)
				}
			}
		)
		val rippleConfig = remember { RippleConfiguration(color = Color.Gray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
		CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
			Box(modifier = Modifier
				.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
				.height(64.dp)
				.width(screenWidth - 50.dp)
				.clickable {
					navBack.value = true
				})
			Box(modifier = Modifier
				.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
				.height(64.dp)
				.fillMaxWidth()
				.padding(screenWidth - 50.dp, 0.dp, 0.dp, 0.dp)
				.clickable {
					share(list[pagerState.currentPage], context, TEXT_PLAIN)
				})
		}
		if(navBack.value)
		{
			postUrl(DEFAULT_STRING_VALUE, null)
			navBack.value = false
			nc.navigate(BottomNavItem.Home.route)
		}
	}
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