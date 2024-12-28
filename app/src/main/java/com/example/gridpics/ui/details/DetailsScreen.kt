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
	checkOnErrorExists: (String) -> Boolean,
	addError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	removeError: (String) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	postVisibleBarsState: () -> Unit,
	isValidUrl: (String) -> Boolean,
	changeBarsVisability: (Boolean) -> Unit,
	postNewBitmap: (String) -> Unit,
	addPicture: (String) -> Unit,
	saveToSharedPrefs: (List<String>) -> Unit,
	setImageSharedStateToFalse: () -> Unit,
	picsUiState: MutableState<PicturesScreenUiState>,
	setCurrentPictureUrl: (String) -> Unit,
)
{
	val value = state.value
	val context = LocalContext.current
	val picsStateValue = picsUiState.value
	val currentPicture = value.currentPicture
	val isScreenInPortrait = picsStateValue.isPortraitOrientation
	BackHandler {
		navigateToHome(
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = navController,
			setImageSharedStateToFalse = setImageSharedStateToFalse
		)
	}
	val list = value.picturesUrl
	Log.d("check", "$list")
	Scaffold(
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		topBar = {
			AppBar(
				isVisible = value.barsAreVisible,
				context = context,
				nc = navController,
				currentPicture = currentPicture,
				postUrl = postUrl,
				state = state,
				changeBarsVisability = changeBarsVisability,
				setImageSharedStateToFalse = setImageSharedStateToFalse
			)
		},
		content = { padding ->
			ShowDetails(
				navController = navController,
				list = list,
				context = context,
				checkOnErrorExists = checkOnErrorExists,
				addError = addError,
				removeSpecialError = removeError,
				state = state,
				postPositiveState = postVisibleBarsState,
				isValidUrl = isValidUrl,
				padding = padding,
				changeBarsVisability = changeBarsVisability,
				postUrl = postUrl,
				addPicture = addPicture,
				isScreenInPortraitState = isScreenInPortrait,
				saveToSharedPrefs = saveToSharedPrefs,
				setImageSharedStateToFalse = setImageSharedStateToFalse,
				postNewBitmap = postNewBitmap,
				setCurrentPictureUrl = setCurrentPictureUrl,
				currentPicture = currentPicture
			)
		}
	)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShowDetails(
	navController: NavController,
	list: List<String>,
	context: Context,
	checkOnErrorExists: (String) -> Boolean,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	postPositiveState: () -> Unit,
	isValidUrl: (String) -> Boolean,
	padding: PaddingValues,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	addPicture: (String) -> Unit,
	isScreenInPortraitState: Boolean,
	saveToSharedPrefs: (List<String>) -> Unit,
	setImageSharedStateToFalse: () -> Unit,
	postNewBitmap: (String) -> Unit,
	setCurrentPictureUrl: (String) -> Unit,
	currentPicture: String,
)
{
	var initialPage = list.indexOf(currentPicture)
	var size = list.size
	if(list.isEmpty()) {
		initialPage = 0
		size = 1
	}
	val pagerState = rememberPagerState(initialPage = initialPage, initialPageOffsetFraction = 0f, pageCount = { size })
	val isSharedImage = state.value.isSharedImage
	Log.d("checkCheck", "$isSharedImage")
	val topBarHeight = 64.dp
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	HorizontalPager(
		state = pagerState,
		pageSize = PageSize.Fill,
		contentPadding = PaddingValues(0.dp, statusBarHeightFixed + topBarHeight, 0.dp, padding.calculateBottomPadding()),
		userScrollEnabled = !isSharedImage,
		pageSpacing = 10.dp
	) { page ->
		val errorPicture = remember { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }
		val realCurrentPage = pagerState.currentPage
		LaunchedEffect(realCurrentPage) {
			val pic = list[realCurrentPage]
			setCurrentPictureUrl(pic)
			if(checkOnErrorExists(pic))
			{
				Log.d("checkMa", "gruzim oshibku")
				postUrl(pic, errorPicture)
			}
			else
			{
				postNewBitmap(pic)
			}
		}
		Box(modifier = Modifier.fillMaxSize()) {
			val isError = checkOnErrorExists(list[page])
			if(isError)
			{
				ShowError(
					context = context,
					currentUrl = list[page],
					pagerState = pagerState,
					isValidUrl = isValidUrl
				)
			}
			else
			{
				ShowAsynchImage(
					img = list[page],
					addError = addError,
					removeSpecialError = removeSpecialError,
					postPositiveState = postPositiveState,
					navController = navController,
					state = state,
					context = context,
					changeBarsVisability = changeBarsVisability,
					postUrl = postUrl,
					isScreenInPortraitState = isScreenInPortraitState,
					setImageSharedStateToFalse = setImageSharedStateToFalse
				)
			}
			if(isSharedImage)
			{
				val addString = stringResource(R.string.add)
				val cancelString = stringResource(R.string.cancel)
				Log.d("case shared", "show buttons")
				Row(
					modifier = Modifier
						.height(80.dp)
						.padding(0.dp, 0.dp, 0.dp, 0.dp)
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
									changeBarsVisability = changeBarsVisability,
									postUrl = postUrl,
									navController = navController,
									setImageSharedStateToFalse = setImageSharedStateToFalse
								)
							},
							border = BorderStroke(3.dp, Color.Red),
							colors = ButtonColors(MaterialTheme.colorScheme.background, Color.Black, Color.Black, Color.White)
						) {
							Text(text = cancelString, color = Color.Red)
						}
						if(!isError)
						{
							Button(
								modifier = Modifier
									.align(Alignment.CenterVertically)
									.padding(30.dp, 0.dp, 0.dp, 0.dp)
									.size(130.dp, 60.dp),
								onClick = {
									saveToSharedPrefs(list)
									if (pagerState.pageCount == 1) {
										navigateToHome(
											changeBarsVisability = changeBarsVisability,
											postUrl = postUrl,
											navController = navController,
											setImageSharedStateToFalse = setImageSharedStateToFalse
										)
									}
									addPicture(list[page])
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
		}
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ShowAsynchImage(
	img: String,
	addError: (String) -> Unit,
	removeSpecialError: (String) -> Unit,
	postPositiveState: () -> Unit,
	navController: NavController,
	state: MutableState<DetailsScreenUiState>,
	context: Context,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	isScreenInPortraitState: Boolean,
	setImageSharedStateToFalse: () -> Unit,
)
{
	val scale = if(state.value.isMultiWindowed)
	{
		ContentScale.Fit
	}
	else
	{
		if(isScreenInPortraitState)
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
			removeSpecialError(img)
		},
		onError = {
			addError(img)
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
							val lastIndex = count.lastIndex
							countLastThree.add(count[lastIndex])
							countLastThree.add(count[lastIndex - 1])
							countLastThree.add(count[lastIndex - 2])
						}
						if(changes.any { !it.pressed })
						{
							if(zoom.scale < 0.92.toFloat() && exit && countLastThree.max() == 2)
							{
								postUrl(DEFAULT_STRING_VALUE, null)
								postPositiveState()
								navigateToHome(
									changeBarsVisability = changeBarsVisability,
									postUrl = postUrl,
									navController = navController,
									setImageSharedStateToFalse = setImageSharedStateToFalse
								)
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
	currentUrl: String,
	pagerState: PagerState,
	isValidUrl: (String) -> Boolean,
)
{
	val linkIsNotValid = stringResource(R.string.link_is_not_valid)
	val scope = rememberCoroutineScope()
	val errorMessage = if(isValidUrl(currentUrl))
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
			Button(
				onClick =
				{
					Toast.makeText(context, R.string.reload_pic, Toast.LENGTH_LONG).show()
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage)
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
	currentPicture: String,
	postUrl: (String, Bitmap?) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	changeBarsVisability: (Boolean) -> Unit,
	setImageSharedStateToFalse: () -> Unit,
)
{
	val navBack = remember { mutableStateOf(false) }
	val screenWidth = LocalConfiguration.current.screenWidthDp.dp
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
						screenWidth
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
								share(currentPicture, context, TEXT_PLAIN)
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
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = nc,
			setImageSharedStateToFalse = setImageSharedStateToFalse
		)
	}
}

fun navigateToHome(
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String, Bitmap?) -> Unit,
	navController: NavController,
	setImageSharedStateToFalse: () -> Unit,
)
{
	changeBarsVisability(true)
	postUrl(DEFAULT_STRING_VALUE, null)
	setImageSharedStateToFalse()
	navController.navigate(Screen.Home.route) {
		popUpTo(navController.graph.findStartDestination().id)
	}
}

fun share(text: String, context: Context, plain: String)
{
	val sendIntent = Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		flags = Intent.FLAG_ACTIVITY_NEW_TASK
		type = plain
	}
	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(context, shareIntent, null)
}