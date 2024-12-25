package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
	postFalseToSharedImageState: () -> Unit,
	removeUrl: (String) -> Unit,
	saveToSharedPrefs: (String) -> Unit,
	changeAddedState: (Boolean?) -> Unit,
	postIsFirstPage: (Boolean) -> Unit,
)
{
	val context = LocalContext.current
	val valuePicUi = picturesScreenState.value
	val currentPicture = valuePicUi.currentPicture
	Log.d("Case shared", currentPicture)
	val pictures = valuePicUi.picturesUrl
	val value = state.value
	Log.d("pictures", pictures)
	val scrollIsEnabled = remember { mutableStateOf(true) }
	BackHandler {
		navigateToHome(
			isSharedImage = state.value.isSharedImage,
			removeUrl = removeUrl,
			currentPicture = currentPicture,
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = navController
		)
	}
	val list = remember(currentPicture) {
		if(pictures.isNotEmpty())
		{
			pictures.split("\n").toSet().toList()
		}
		else
		{
			listOf(currentPicture)
		}
	}
	val index = list.indexOf(currentPicture)
	val startPage = if(index > -1)
	{
		index
	}
	else
	{
		0
	}
	Log.d("index list", list.toString())
	Log.d("index currentPic", currentPicture)
	Log.d("index current", index.toString())
	val pagerState = rememberPagerState(initialPage = startPage, initialPageOffsetFraction = 0f, pageCount = { list.size })
	val currentPage = pagerState.currentPage
	val errorPicture = remember { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }

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
				state = state,
				removeUrl = removeUrl,
				changeBarsVisability = changeBarsVisability,
				postIsFirstPage = postIsFirstPage
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
				postUrl = postUrl,
				scrollIsEnabled = scrollIsEnabled,
				postFalseToSharedImageState = postFalseToSharedImageState,
				removeUrl = removeUrl,
				isScreenInPortraitState = picturesScreenState,
				saveToSharedPrefs = saveToSharedPrefs,
				changeAddedState = changeAddedState
			)
		}
	)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
	scrollIsEnabled: MutableState<Boolean>,
	postFalseToSharedImageState: () -> Unit,
	removeUrl: (String) -> Unit,
	isScreenInPortraitState: MutableState<PicturesScreenUiState>,
	saveToSharedPrefs: (String) -> Unit,
	changeAddedState: (Boolean?) -> Unit
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
		userScrollEnabled = scrollIsEnabled.value,
		pageSpacing = 10.dp
	) { page ->
		val showButtonAdd = remember { mutableStateOf(true) }
		Box(modifier = Modifier.fillMaxSize()) {
			if(checkIfExists(list[page]))
			{
				showButtonAdd.value = false
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
				showButtonAdd.value = true
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
					postUrl = postUrl,
					isScreenInPortraitState = isScreenInPortraitState
				)
			}
			val isSharedImage = state.value.isSharedImage
			if(isSharedImage)
			{
				scrollIsEnabled.value = false
				val addString = stringResource(R.string.add)
				val cancelString = stringResource(R.string.cancel)
				Log.d("case shared", "show buttons")
				Row(
					modifier = Modifier
						.height(80.dp)
						.padding(0.dp, 20.dp, 0.dp, 0.dp)
						.align(Alignment.BottomCenter)
				) {
					val rippleConfig = remember { RippleConfiguration(color = Color.LightGray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
					CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
						Button(
							modifier = Modifier
								.align(Alignment.CenterVertically)
								.size(130.dp, 60.dp),
							onClick = {
								navigateToHome(
									isSharedImage = isSharedImage,
									removeUrl = removeUrl,
									currentPicture = list[page],
									changeBarsVisability = changeBarsVisability,
									postUrl = postUrl,
									navController = navController
								)
							},
							border = BorderStroke(3.dp, Color.Red),
							colors = ButtonColors(MaterialTheme.colorScheme.background, Color.Black, Color.Black, Color.White)
						) {
							Text(text = cancelString, color = Color.Red)
						}
						if(showButtonAdd.value)
						{
							Button(
								modifier = Modifier
									.align(Alignment.CenterVertically)
									.padding(30.dp, 0.dp, 0.dp, 0.dp)
									.size(130.dp, 60.dp),
								onClick = {
									if(list.size != 1)
									{
										scrollIsEnabled.value = true
									}
									saveToSharedPrefs(isScreenInPortraitState.value.picturesUrl)
									postFalseToSharedImageState()
									changeAddedState(true)
								},
								border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
								colors = ButtonColors(MaterialTheme.colorScheme.background, Color.Black, Color.Black, Color.White)
							) {
								Text(text = addString, color = MaterialTheme.colorScheme.primary)
							}
						}
					}
				}
			}
			else changeAddedState(null)
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
	isScreenInPortraitState: MutableState<PicturesScreenUiState>,
)
{
	val scale = if(state.value.isMultiWindowed)
	{
		ContentScale.Fit
	}
	else
	{
		if(isScreenInPortraitState.value.isPortraitOrientation)
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
		},
		modifier = Modifier
			.fillMaxSize()
			.zoomable(
				zoomState = zoom,
				enableOneFingerZoom = false,
				onTap =
				{
					val visibility = state.value.barsAreVisible
					changeBarsVisability(!visibility)
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
	state: MutableState<DetailsScreenUiState>,
	removeUrl: (String) -> Unit,
	changeBarsVisability: (Boolean) -> Unit,
	postIsFirstPage: (Boolean) -> Unit,
)
{
	val navBack = remember { mutableStateOf(false) }
	val screenWidth = LocalConfiguration.current.screenWidthDp.dp
	val currentPicture = list[pagerState.currentPage]
	Log.d("shared pic url", currentPicture)
	val sharedImgCase = state.value.isSharedImage
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
		val rippleConfig = remember { RippleConfiguration(color = Color.Gray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
		CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
			TopAppBar(
				modifier = Modifier
					.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout))
					.wrapContentSize(),
				title = {
					val width = if(sharedImgCase)
					{
						screenWidth - 0.dp
					}
					else
					{
						screenWidth - 50.dp
					}
					Box(modifier = Modifier
						.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
						.height(64.dp)
						.width(width)
						.clickable {
							navBack.value = true
						}) {
						Text(
							text = currentPicture,
							fontSize = 18.sp,
							maxLines = 2,
							modifier = Modifier
								.align(Alignment.Center),
							overflow = TextOverflow.Ellipsis,
						)
					}
				},
				navigationIcon = {
					Box(modifier = Modifier
						.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
						.height(64.dp)
						.width(50.dp)
						.clickable {
							navBack.value = true
						}) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "back",
							modifier = Modifier
								.align(Alignment.Center)
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
					AnimatedVisibility(!sharedImgCase) {
						Box(modifier = Modifier
							.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
							.height(64.dp)
							.width(50.dp)
							.clickable {
								share(currentPicture, context, TEXT_PLAIN, pagerState.currentPage, postIsFirstPage)
							}
						) {
							Icon(
								modifier = Modifier.align(Alignment.Center),
								imageVector = Icons.Default.Share,
								contentDescription = "share",
								tint = MaterialTheme.colorScheme.onPrimary,
							)
						}
					}
				}
			)
		}
	}
	if(navBack.value)
	{
		navBack.value = false
		navigateToHome(
			isSharedImage = state.value.isSharedImage,
			removeUrl = removeUrl,
			currentPicture = currentPicture,
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = nc
		)
	}
}

fun navigateToHome(
	isSharedImage: Boolean,
	removeUrl: (String) -> Unit,
	currentPicture: String,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	navController: NavController,
)
{
	if(isSharedImage)
	{
		removeUrl(currentPicture)
		Log.d("ahaha", "ya vizval")
	}
	changeBarsVisability(true)
	postUrl(DEFAULT_STRING_VALUE, null)
	navController.navigate(Screen.Home.route) {
		popUpTo(navController.graph.findStartDestination().id)
	}
}

fun share(text: String, context: Context, plain: String, page: Int, postIsFirstPage: (Boolean) -> Unit)
{
	if(page == 0)
	{
		postIsFirstPage(true)
	}
	else
	{
		postIsFirstPage(false)
	}
	val sendIntent = Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = plain
	}
	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(context, shareIntent, null)
}