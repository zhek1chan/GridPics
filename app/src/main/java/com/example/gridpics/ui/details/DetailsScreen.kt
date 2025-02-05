package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import androidx.navigation.navOptions
import coil3.compose.SubcomposeAsyncImage
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
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
	animationHasBeenStarted: MutableState<Boolean>,
	postPivot: () -> Unit,
	postCutouts: (Float, Float) -> Unit,
	orientationWasChanged: MutableState<Boolean>,
	postBars: (Float, Float) -> Unit,
)
{
	val cutouts = WindowInsets.displayCutout
	val direction = LocalLayoutDirection.current
	val conf = LocalConfiguration.current.orientation
	val paddingForCutouts = if(cutouts.asPaddingValues() == PaddingValues(0.dp))
	{
		PaddingValues(600.dp)
	}
	else
	{
		cutouts.asPaddingValues()
	}

	LaunchedEffect(conf) {
		val left = paddingForCutouts.calculateLeftPadding(direction)
		val right = paddingForCutouts.calculateRightPadding(direction)
		val top = paddingForCutouts.calculateTopPadding().value
		val bottom = paddingForCutouts.calculateBottomPadding().value
		Log.d("proverka cutouts", "$left $right")
		postBars(top, bottom)
		Log.d("proverka cutov", "${left.value} ${right.value}")
		if(orientationWasChanged.value)
		{
			postCutouts(left.value, right.value)
		}
	}
	val color = MaterialTheme.colorScheme.background
	val backgroundColor = remember { mutableStateOf(color) }
	val animationIsRunningLocal = remember(false) { mutableStateOf(true) }
	LaunchedEffect(false) {
		if(state.value.isSharedImage)
		{
			animationIsRunningLocal.value = false
		}
		else
		{
			delay(1000)
			animationIsRunningLocal.value = false
			animationHasBeenStarted.value = false
		}
	}
	LaunchedEffect(animationIsRunningLocal.value) {
		if(animationIsRunningLocal.value)
		{
			backgroundColor.value = color.copy(0.0001f)
		}
		else
		{
			backgroundColor.value = color
		}
	}
	val value = state.value
	val context = LocalContext.current
	val currentPicture = value.currentPicture
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
	val errorPicture = remember(Unit) { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }
	val pleaseWaitString = stringResource(R.string.please_wait_the_pic_is_loading)
	BackHandler {
		if(!animationIsRunningLocal.value)
		{
			Log.d("activated", "activated")
			navigateToHome(
				changeBarsVisability = changeBarsVisability,
				postUrl = postUrl,
				navController = navController,
				setImageSharedStateToFalse = setImageSharedState,
				animationIsRunning = animationIsRunningLocal,
				wasDeleted = false,
				animationHasBeenStarted = animationHasBeenStarted,
				postPivot = postPivot,
				state = state,
				checkOnErrorExists = getErrorMessageFromErrorsList
			)
		}
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
		containerColor = backgroundColor.value,
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
				animationIsRunning = animationIsRunningLocal,
				animationHasBeenStarted = animationHasBeenStarted,
				postPivot = postPivot,
				checkOnErrorExists = getErrorMessageFromErrorsList
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
				animationIsRunning = animationIsRunningLocal,
				setFalseToWasDeletedFromNotification = setFalseToWasDeletedFromNotification,
				animationHasBeenStarted = animationHasBeenStarted,
				postPivot = postPivot
			)
		}
	)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShowDetails(
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
	animationIsRunning: MutableState<Boolean>,
	setFalseToWasDeletedFromNotification: () -> Unit,
	animationHasBeenStarted: MutableState<Boolean>,
	postPivot: () -> Unit,
)
{
	val isScreenInPortraitState = picturesState.value.isPortraitOrientation
	val isSharedImage = state.value.isSharedImage
	Log.d("checkCheck", "$isSharedImage")
	val topBarHeight = 64.dp
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	HorizontalPager(
		state = pagerState,
		pageSize = PageSize.Fill,
		contentPadding = PaddingValues(0.dp, statusBarHeightFixed + topBarHeight, 0.dp, padding.calculateBottomPadding()),
		userScrollEnabled = !isSharedImage && !animationIsRunning.value,
		pageSpacing = 10.dp
	) { page ->
		val url = list[page]
		val errorMessage = checkOnErrorExists(url)
		Log.d("checkUp", "is error $errorMessage")
		Box(modifier = Modifier
			.fillMaxSize()
			.background(Color.Transparent)) {
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
					context = context,
					changeBarsVisability = changeBarsVisability,
					postUrl = postUrl,
					isScreenInPortraitState = isScreenInPortraitState,
					setImageSharedStateToFalse = setImageSharedState,
					animationIsRunning = animationIsRunning,
					animationHasBeenStarted = animationHasBeenStarted,
					postPivot = postPivot,
					checkOnErrorExists = checkOnErrorExists
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
									animationIsRunning = animationIsRunning,
									wasDeleted = false,
									animationHasBeenStarted = animationHasBeenStarted,
									postPivot = postPivot,
									state = state,
									checkOnErrorExists = checkOnErrorExists
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
											animationIsRunning = animationIsRunning,
											wasDeleted = false,
											animationHasBeenStarted = animationHasBeenStarted,
											postPivot = postPivot,
											state = state,
											checkOnErrorExists = checkOnErrorExists
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
					AnimatedVisibility(!animationIsRunning.value, enter = EnterTransition.None, exit = ExitTransition.None) {
						val rippleConfig = remember { RippleConfiguration(color = Color.LightGray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
						CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
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
							navigateToHome(
								changeBarsVisability = changeBarsVisability,
								postUrl = postUrl,
								navController = navController,
								setImageSharedStateToFalse = setImageSharedState,
								animationIsRunning = animationIsRunning,
								wasDeleted = true,
								animationHasBeenStarted = animationHasBeenStarted,
								postPivot = postPivot,
								state = state,
								checkOnErrorExists = checkOnErrorExists
							)
							deleteCurrentPicture(url)
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

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ShowAsynchImage(
	img: String,
	addError: (String, String) -> Unit,
	removeSpecialError: (String) -> Unit,
	navController: NavController,
	state: MutableState<DetailsScreenUiState>,
	context: Context,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	isScreenInPortraitState: Boolean,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	animationIsRunning: MutableState<Boolean>,
	animationHasBeenStarted: MutableState<Boolean>,
	postPivot: () -> Unit,
	checkOnErrorExists: (String) -> String?,
)
{
	val expanded = remember { mutableStateOf(false) }
	if(animationIsRunning.value)
	{
		expanded.value = true
	}
	Box(Modifier.fillMaxSize()) {
		Box(Modifier
			.animateContentSize()
			.align(Alignment.Center)
			.height(if(expanded.value) 400.dp else 1000.dp)
			.fillMaxWidth()
		) {
			val width = remember { mutableIntStateOf(0) }
			val height = remember { mutableIntStateOf(0) }
			val scale = if(animationIsRunning.value)
			{
				if(isScreenInPortraitState)
				{
					if(width.intValue > height.intValue)
					{
						ContentScale.FillWidth
					}
					else if(width.intValue < height.intValue)
					{
						ContentScale.FillHeight
					}
					else
					{
						ContentScale.Crop
					}
				}
				else
				{
					if(width.intValue > height.intValue)
					{
						ContentScale.FillHeight
					}
					else if(width.intValue < height.intValue)
					{
						ContentScale.FillHeight
					}
					else
					{
						ContentScale.FillHeight
					}
				}
			}
			else if(state.value.isMultiWindowed)
			{
				ContentScale.Fit
			}
			else
			{
				if(isScreenInPortraitState)
				{
					if(width.intValue > height.intValue)
					{
						ContentScale.FillWidth
					}
					else if(width.intValue < height.intValue)
					{
						ContentScale.FillHeight
					}
					else
					{
						ContentScale.Crop
					}
				}
				else
				{
					if(width.intValue > height.intValue)
					{
						ContentScale.FillHeight
					}
					else if(width.intValue < height.intValue)
					{
						ContentScale.FillHeight
					}
					else
					{
						ContentScale.FillHeight
					}
				}
			}
			val zoom = rememberZoomState(15f, Size.Zero)
			var imageSize by remember { mutableStateOf(Size.Zero) }
			val imgRequest = remember(img) {
				ImageRequest.Builder(context)
					.data(img)
					.error(R.drawable.error)
					.diskCacheKey(img)
					.build()
			}
			val mod = remember(animationIsRunning.value) { mutableStateOf(Modifier.fillMaxSize()) }
			if(animationIsRunning.value)
			{
				mod.value = Modifier
					.fillMaxSize()
					.clip(RoundedCornerShape(8.dp))
					.alpha(0.5f)
					.align(Alignment.Center)
			}
			else
			{
				mod.value = Modifier
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
											animationIsRunning = animationIsRunning,
											wasDeleted = false,
											animationHasBeenStarted = animationHasBeenStarted,
											postPivot = postPivot,
											state = state,
											checkOnErrorExists = checkOnErrorExists
										)
									}
								}
								countLastThree.clear()
								count.add(changes.size)
							}
						}
					}
			}
			val scope = rememberCoroutineScope()
			SubcomposeAsyncImage(
				model = imgRequest,
				contentDescription = null,
				contentScale = scale,
				onSuccess = {
					val resultImage = it.result.image
					imageSize = Size(resultImage.width.toFloat(), resultImage.height.toFloat())
					width.intValue = resultImage.width
					height.intValue = resultImage.height
					removeSpecialError(img)
				},
				loading = {
					Box(Modifier.fillMaxSize()) {
						CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
					}
				},
				onError = {
					addError(img, it.result.throwable.message.toString())
					navController.navigate(Screen.Details.route)
				},
				modifier = mod.value
					.align(Alignment.TopStart)
			)

			scope.launch {
				zoom.setContentSize(imageSize)
			}
		}
	}
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
	animationIsRunning: MutableState<Boolean>,
	animationHasBeenStarted: MutableState<Boolean>,
	postPivot: () -> Unit,
	checkOnErrorExists: (String) -> String?,
)
{
	if(state.value.wasSharedFromNotification)
	{
		share(currentPicture)
		postWasSharedState()
	}
	val navBack = remember { mutableStateOf(false) }
	val screenWidth = LocalConfiguration.current.screenWidthDp.dp
	Log.d("shared pic url", currentPicture)
	val sharedImgCase = state.value.isSharedImage
	Log.d("wahwah", "$screenWidth")
	AnimatedVisibility(!animationIsRunning.value, enter = EnterTransition.None, exit = ExitTransition.None) {
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
	}
	if(navBack.value)
	{
		navBack.value = false
		navigateToHome(
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = nc,
			setImageSharedStateToFalse = setImageSharedStateToFalse,
			animationIsRunning = animationIsRunning,
			wasDeleted = false,
			animationHasBeenStarted = animationHasBeenStarted,
			postPivot = postPivot,
			state = state,
			checkOnErrorExists = checkOnErrorExists
		)
	}
}

@SuppressLint("RestrictedApi")
fun navigateToHome(
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	navController: NavController,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	animationIsRunning: MutableState<Boolean>,
	wasDeleted: Boolean,
	animationHasBeenStarted: MutableState<Boolean>,
	postPivot: () -> Unit,
	state: MutableState<DetailsScreenUiState>,
	checkOnErrorExists: (String) -> String?,
)
{
	animationIsRunning.value = true
	animationHasBeenStarted.value = true
	changeBarsVisability(true)
	if(wasDeleted || state.value.isSharedImage || checkOnErrorExists(state.value.currentPicture) != null)
	{
		postPivot()
		setImageSharedStateToFalse(false)
		animationHasBeenStarted.value = false
		navController.navigate(Screen.Home.route,
			navOptions = navOptions {
				anim {
					enter = 0
					exit = 0
					popEnter = 0
					popExit = 0
				}
			})
	}
	else
	{
		setImageSharedStateToFalse(false)
		navController.navigateUp()
	}
	postUrl(null, null)
}