package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.error
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.HTTP_ERROR
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import com.example.gridpics.ui.pictures.AlertDialogMain
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.DetailsScreen(
	navController: NavController,
	getErrorMessageFromErrorsList: (String) -> String?,
	addError: (String, String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	removeError: (String) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	isValidUrl: (String) -> Boolean,
	changeBarsVisability: (Boolean) -> Unit,
	postNewBitmap: (String, String) -> Unit,
	addPicture: (String) -> Unit,
	setImageSharedState: (Boolean) -> Unit,
	picsUiState: MutableState<PicturesScreenUiState>,
	setCurrentPictureUrl: (String) -> Unit,
	share: (String) -> Unit,
	deleteCurrentPicture: (String) -> Unit,
	postWasSharedState: () -> Unit,
	setFalseToWasDeletedFromNotification: () -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
)
{
	val value = state.value
	val currentPicture = remember(Unit) { value.currentPicture }
	var list = remember(state.value.isSharedImage) { state.value.picturesUrl }
	var initialPage = list.indexOf(currentPicture)
	val size: Int
	if(initialPage >= 0)
	{
		size = list.size
	}
	else
	{
		list = listOf(currentPicture)
		initialPage = 0
		size = 1
	}
	Log.d("index currentPage", currentPicture)
	Log.d("index initialPage", "$initialPage")
	Log.d("index size of list", "$size")
	val pagerState = rememberPagerState(initialPage = initialPage, initialPageOffsetFraction = 0f, pageCount = { size })
	val currentPage = pagerState.currentPage
	val context = LocalContext.current
	val errorPicture = remember(Unit) { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }
	val pleaseWaitString = stringResource(R.string.please_wait_the_pic_is_loading)
	val animationIsRunning = remember { mutableStateOf(true) }
	LaunchedEffect(Unit) {
		animationIsRunning.value = true
		val animatorScale = Settings.Global.getFloat(
			context.contentResolver,
			Settings.Global.ANIMATOR_DURATION_SCALE,
			1f
		)
		Log.d("animation is running", "${animatedVisibilityScope.transition.totalDurationNanos} $animatorScale")
		delay((animatedVisibilityScope.transition.totalDurationNanos.toFloat() * animatorScale / 1000000).toLong()) //перевод в милисекунды
		animationIsRunning.value = false
	}
	val exitIsStarted = remember { mutableStateOf(false) }
	BackHandler {
		navigateToHome(
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = navController,
			setImageSharedStateToFalse = setImageSharedState,
			wasDeleted = false,
			state = state,
			checkOnErrorExists = getErrorMessageFromErrorsList,
			exitIsStarted = exitIsStarted
		)
	}
	LaunchedEffect(currentPage) {
		val pic = if(list.size >= currentPage)
		{
			list[currentPage]
		}
		else
		{
			""
		}
		if(pic.isNotEmpty())
		{
			setCurrentPictureUrl(pic)
			if(getErrorMessageFromErrorsList(pic) != null)
			{
				Log.d("checkMa", "gruzim oshibku")
				postUrl(pic, errorPicture)
			}
			else
			{
				postNewBitmap(pic, pleaseWaitString)
			}
		}
	}
	Scaffold(
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		topBar = {
			AppBar(
				isVisible = value.barsAreVisible,
				nc = navController,
				currentPicture = currentPicture,
				postUrl = postUrl,
				state = state,
				changeBarsVisability = changeBarsVisability,
				setImageSharedStateToFalse = setImageSharedState,
				share = share,
				postWasSharedState = postWasSharedState,
				checkOnErrorExists = getErrorMessageFromErrorsList,
				exitIsStarted = exitIsStarted
			)
		},
		content = { padding ->
			ShowDetails(
				navController = navController,
				context = context,
				checkOnErrorExists = getErrorMessageFromErrorsList,
				addError = addError,
				removeSpecialError = removeError,
				state = state,
				isValidUrl = isValidUrl,
				padding = padding,
				changeBarsVisability = changeBarsVisability,
				postUrl = postUrl,
				addPicture = addPicture,
				setImageSharedState = setImageSharedState,
				picturesState = picsUiState,
				pagerState = pagerState,
				list = list.toMutableList(),
				deleteCurrentPicture = deleteCurrentPicture,
				setFalseToWasDeletedFromNotification = setFalseToWasDeletedFromNotification,
				animationIsRunning = animationIsRunning,
				animatedVisibilityScope = animatedVisibilityScope,
				exitIsStarted = exitIsStarted,
			)
		}
	)
}

@SuppressLint("CoroutineCreationDuringComposition", "RestrictedApi")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ShowDetails(
	navController: NavController,
	context: Context,
	checkOnErrorExists: (String) -> String?,
	addError: (String, String) -> Unit,
	removeSpecialError: (String) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	isValidUrl: (String) -> Boolean,
	padding: PaddingValues,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	addPicture: (String) -> Unit,
	setImageSharedState: (Boolean) -> Unit,
	picturesState: MutableState<PicturesScreenUiState>,
	pagerState: PagerState,
	list: MutableList<String>,
	deleteCurrentPicture: (String) -> Unit,
	setFalseToWasDeletedFromNotification: () -> Unit,
	animationIsRunning: MutableState<Boolean>,
	animatedVisibilityScope: AnimatedVisibilityScope,
	exitIsStarted: MutableState<Boolean>,
)
{
	val isScreenInPortraitState = picturesState.value.isPortraitOrientation
	val isSharedImage = state.value.isSharedImage
	Log.d("checkCheck", "$isSharedImage")
	val wasDeleted = remember(pagerState.currentPage) { mutableStateOf(false) }
	val topBarHeight = 64.dp
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	AnimatedVisibility(!wasDeleted.value) {
		HorizontalPager(
			state = pagerState,
			pageSize = PageSize.Fill,
			contentPadding = PaddingValues(0.dp, statusBarHeightFixed + topBarHeight, 0.dp, padding.calculateBottomPadding()),
			userScrollEnabled = !isSharedImage && !animationIsRunning.value && !exitIsStarted.value,
			pageSpacing = 10.dp,
			beyondViewportPageCount = 0
		) { page ->
			val url = list[page]
			val errorMessage = checkOnErrorExists(url)
			val mod = if(errorMessage != null || isSharedImage || wasDeleted.value || page != pagerState.currentPage)
			{
				Modifier
					.fillMaxSize()
					.background(Color.Transparent)
			}
			else
			{
				Modifier
					.sharedElement(
						state = rememberSharedContentState(key = pagerState.currentPage),
						placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
						animatedVisibilityScope = animatedVisibilityScope
					)
					.fillMaxSize()
					.background(Color.Transparent)
			}
			Box(modifier = mod) {
				if(errorMessage != null)
				{
					ShowError(
						context = context,
						currentUrl = url,
						isValidUrl = isValidUrl,
						removeSpecialError = removeSpecialError)
				}
				else
				{
					ShowAsynchImage(
						img = url,
						addError = addError,
						removeSpecialError = removeSpecialError,
						navController = navController,
						state = state,
						changeBarsVisability = changeBarsVisability,
						postUrl = postUrl,
						isScreenInPortraitState = isScreenInPortraitState,
						setImageSharedStateToFalse = setImageSharedState,
						checkOnErrorExists = checkOnErrorExists,
						exitIsStarted = exitIsStarted
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
										setImageSharedStateToFalse = setImageSharedState,
										wasDeleted = false,
										state = state,
										checkOnErrorExists = checkOnErrorExists,
										exitIsStarted = exitIsStarted
									)
								},
								border = BorderStroke(3.dp, Color.Red),
								colors = ButtonColors(MaterialTheme.colorScheme.background, Color.Black, Color.Black, Color.White)
							) {
								Text(text = cancelString, color = Color.Red, textAlign = TextAlign.Center)
							}
							if(errorMessage == null)
							{
								Button(
									modifier = Modifier
										.align(Alignment.CenterVertically)
										.padding(30.dp, 0.dp, 0.dp, 0.dp)
										.size(130.dp, 60.dp),
									onClick = {
										if(pagerState.pageCount == 1)
										{
											navigateToHome(
												changeBarsVisability = changeBarsVisability,
												postUrl = postUrl,
												navController = navController,
												setImageSharedStateToFalse = setImageSharedState,
												wasDeleted = false,
												state = state,
												checkOnErrorExists = checkOnErrorExists,
												exitIsStarted = exitIsStarted
											)
										}
										setImageSharedState(false)
										addPicture(url)
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
				else
				{
					val openDialog = remember { mutableStateOf(state.value.wasDeletedFromNotification) }
					val cancelString = stringResource(R.string.delete_picture)
					Row(
						modifier = Modifier
							.height(80.dp)
							.padding(0.dp, 0.dp, 0.dp, 0.dp)
							.align(Alignment.BottomCenter)
					) {
						val rippleConfig = remember { RippleConfiguration(color = Color.LightGray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
						CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
							AnimatedVisibility(!animatedVisibilityScope.transition.isRunning && !exitIsStarted.value, enter = EnterTransition.None, exit = ExitTransition.None) {
								Button(
									modifier = Modifier
										.align(Alignment.CenterVertically)
										.size(130.dp, 60.dp),
									onClick = {
										openDialog.value = true
									},
									border = BorderStroke(3.dp, Color.Red),
									colors = ButtonColors(MaterialTheme.colorScheme.background, Color.Black, Color.Black, Color.White)
								) {
									Text(text = cancelString, color = Color.Red, textAlign = TextAlign.Center)
								}
							}
						}
					}
					AnimatedVisibility(openDialog.value) {
						AlertDialogMain(
							dialogText = null,
							dialogTitle = stringResource(R.string.do_you_really_want_to_delete_it),
							onConfirmation = {
								wasDeleted.value = true
								deleteCurrentPicture(url)
								navigateToHome(
									changeBarsVisability = changeBarsVisability,
									postUrl = postUrl,
									navController = navController,
									setImageSharedStateToFalse = setImageSharedState,
									wasDeleted = true,
									state = state,
									checkOnErrorExists = checkOnErrorExists,
									exitIsStarted = exitIsStarted
								)
								openDialog.value = false
							},
							onDismissRequest = {
								openDialog.value = false
								setFalseToWasDeletedFromNotification()
							},
							icon = Icons.Default.Delete,
							textButtonCancel = stringResource(R.string.cancel),
							textButtonConfirm = stringResource(R.string.confirm))
					}
				}
			}
		}
	}
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun ShowAsynchImage(
	img: String,
	addError: (String, String) -> Unit,
	removeSpecialError: (String) -> Unit,
	navController: NavController,
	state: MutableState<DetailsScreenUiState>,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	isScreenInPortraitState: Boolean,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	checkOnErrorExists: (String) -> String?,
	exitIsStarted: MutableState<Boolean>,
)
{
	val width = remember { mutableIntStateOf(0) }
	val height = remember { mutableIntStateOf(0) }
	// если в портретном режиме картинка длинная или в горизонтальном положении картинка широкая, то включаем доп анимацию
	//Изменение параметров изображения
	val scale = if(state.value.isMultiWindowed)
	{
		ContentScale.Fit
	}
	// если идёт анимация перехода (стандартная не дополнительная)
	else
	{
		getScale(isScreenInPortraitState, width.intValue, height.intValue)
	}
	println("fnsfnsfns")
	val zoom = rememberZoomState(15f, Size.Zero)
	val context = LocalContext.current
	val headers = remember {
		NetworkHeaders.Builder()
			.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
			.build()
	}
	val imgRequest = remember(img) {
		ImageRequest.Builder(context)
			.data(img)
			.httpHeaders(headers)
			.error(R.drawable.error)
			.memoryCacheKey(img)
			.diskCacheKey(img)
			.build()
	}
	SubcomposeAsyncImage(
		model = imgRequest,
		filterQuality = FilterQuality.Low,
		contentDescription = null,
		contentScale = scale,
		onSuccess = {
			val resultImage = it.result.image
			val widthImage = resultImage.width
			val heightImage = resultImage.height
			width.intValue = widthImage
			height.intValue = heightImage
			removeSpecialError(img)
		},
		loading = {
			val showLoading = remember { mutableStateOf(false) }
			LaunchedEffect(Unit) {
				delay(200)
				showLoading.value = true
			}
			AnimatedVisibility(visible = (showLoading.value), enter = EnterTransition.None, exit = ExitTransition.None) {
				Box(Modifier.fillMaxSize()) {
					CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
				}
			}
		},
		onError = {
			addError(img, it.result.throwable.message.toString())
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
								navigateToHome(
									changeBarsVisability = changeBarsVisability,
									postUrl = postUrl,
									navController = navController,
									setImageSharedStateToFalse = setImageSharedStateToFalse,
									wasDeleted = false,
									state = state,
									checkOnErrorExists = checkOnErrorExists,
									exitIsStarted = exitIsStarted
								)
							}
						}
						countLastThree.clear()
						count.add(changes.size)
					}
				}
			}
	)
}

@Composable
fun ShowError(
	context: Context,
	currentUrl: String,
	isValidUrl: (String) -> Boolean,
	removeSpecialError: (String) -> Unit,
)
{
	val linkIsNotValid = stringResource(R.string.link_is_not_valid)
	val errorMessage = if(isValidUrl(currentUrl))
	{
		HTTP_ERROR
	}
	else
	{
		linkIsNotValid
	}
	val scope = rememberCoroutineScope()
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
			Log.d("TEST111", "error")
			Button(
				onClick =
				{
					Toast.makeText(context, R.string.reload_pic, Toast.LENGTH_LONG).show()
					scope.launch {
						delay(500)
						removeSpecialError(currentUrl)
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
	nc: NavController,
	currentPicture: String,
	postUrl: (String?, Bitmap?) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	changeBarsVisability: (Boolean) -> Unit,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	share: (String) -> Unit,
	postWasSharedState: () -> Unit,
	checkOnErrorExists: (String) -> String?,
	exitIsStarted: MutableState<Boolean>,
)
{
	if(state.value.wasSharedFromNotification)
	{
		share(currentPicture)
		postWasSharedState()
	}
	val navBack = remember { mutableStateOf(false) }
	Log.d("shared pic url", currentPicture)
	val sharedImgCase = state.value.isSharedImage
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
					Box(modifier = Modifier
						.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout))
						.height(64.dp)
						.fillMaxWidth()
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
								share(currentPicture)
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
			setImageSharedStateToFalse = setImageSharedStateToFalse,
			wasDeleted = false,
			state = state,
			checkOnErrorExists = checkOnErrorExists,
			exitIsStarted = exitIsStarted
		)
	}
}

fun navigateToHome(
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	navController: NavController,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	wasDeleted: Boolean,
	state: MutableState<DetailsScreenUiState>,
	checkOnErrorExists: (String) -> String?,
	exitIsStarted: MutableState<Boolean>,
)
{
	exitIsStarted.value = true
	Log.d("activated", "activated")
	if(wasDeleted || state.value.isSharedImage || checkOnErrorExists(state.value.currentPicture) != null)
	{
		navController.navigate(Screen.Home.route)
		setImageSharedStateToFalse(false)
	}
	else
	{
		navController.navigateUp()
		setImageSharedStateToFalse(false)
	}
	changeBarsVisability(true)
	postUrl(null, null)
}

fun getScale(isScreenInPortraitState: Boolean, width: Int, height: Int): ContentScale
{
	//вариант для вертикальной ориентации
	return if(isScreenInPortraitState)
	{
		if(width >= height)
		{
			ContentScale.FillWidth
		}
		else if(height - width > 50)
		{
			ContentScale.FillHeight
		}
		else
		{
			ContentScale.Fit
		}
	}
	//вариант для горизонтальной ориентации
	else
	{
		if(width > height)
		{
			ContentScale.Fit
		}
		else if(width < height)
		{
			ContentScale.FillHeight
		}
		else
		{
			ContentScale.FillHeight
		}
	}
}