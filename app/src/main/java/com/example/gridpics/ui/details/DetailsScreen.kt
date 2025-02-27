package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import coil3.toBitmap
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.HTTP_ERROR
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.details.state.DetailsScreenUiState
import com.example.gridpics.ui.pictures.AlertDialogMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@SuppressLint("RestrictedApi")
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
	setCurrentPictureUrl: (String) -> Unit,
	share: (String) -> Unit,
	deleteCurrentPicture: (String) -> Unit,
	postWasSharedState: () -> Unit,
	setFalseToWasDeletedFromNotification: () -> Unit,
	animatedVisibilityScope: AnimatedVisibilityScope,
	fromNotification: MutableState<Boolean>,
	animationIsRunning: MutableState<Boolean>,
	changeAnimation: MutableState<Boolean>,
	disposable: MutableState<Boolean>,
	shareLocal: (String) -> Unit,
)
{
	changeAnimation.value = false
	val scope = rememberCoroutineScope()
	val value = state.value
	val currentPicture = remember(Unit) { value.currentPicture }
	var list = remember(value.isSharedImage) { value.picturesUrl }
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
	val context = LocalContext.current
	val errorPicture = remember(Unit) { ContextCompat.getDrawable(context, R.drawable.error)?.toBitmap() }
	val pleaseWaitString = stringResource(R.string.please_wait_the_pic_is_loading)
	val isExit = remember { mutableStateOf(false) }
	// setting up the right time of animation
	LaunchedEffect(Unit) {
		if(!value.isSharedImage && !value.wasDeletedFromNotification && !value.wasSharedFromNotification)
		{
			animationIsRunning.value = true
			val animatorScale = Settings.Global.getFloat(
				context.contentResolver,
				Settings.Global.ANIMATOR_DURATION_SCALE,
				1f
			)
			delay((animatedVisibilityScope.transition.totalDurationNanos.toFloat() * animatorScale / 1000000).toLong()) //перевод в милисекунды
		}
		animationIsRunning.value = false
	}
	val wasDeleted = remember(pagerState.currentPage) { mutableStateOf(false) }
	val wasCalledDelete = remember { mutableStateOf(false) }
	BackHandler {
		if(value.isSharedImage)
		{
			changeAnimation.value = true
			wasCalledDelete.value = true
			setImageSharedState(false)
		}
		navigateToHome(
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = navController,
			setImageSharedStateToFalse = setImageSharedState,
			state = state,
			animationIsRunning = animationIsRunning,
			isExit = isExit,
			wasDeleted = wasDeleted,
			scope = scope,
			disposable = disposable
		)
	}
	val mutableStateValBitmap = remember { mutableStateOf(errorPicture?.let { Bitmap.createBitmap(it) }) }
	val updatePicture = remember { mutableStateOf(false) }
	LaunchedEffect(updatePicture.value) {
		snapshotFlow { pagerState.currentPage }.collect { page ->
			Log.d("checkMa", "send")
			val pic = if(list.size >= page)
			{
				list[page]
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
	}
	Scaffold(
		modifier = Modifier.wrapContentSize(),
		contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
		topBar = {
			AppBar(
				isVisible = value.barsAreVisible,
				nc = navController,
				currentPicture = value.currentPicture,
				postUrl = postUrl,
				state = state,
				changeBarsVisability = changeBarsVisability,
				setImageSharedStateToFalse = setImageSharedState,
				share = share,
				postWasSharedState = postWasSharedState,
				animationIsRunning = animationIsRunning,
				isExit = isExit,
				wasDeleted = wasDeleted,
				fromNotification = fromNotification,
				scope = scope,
				wasCalledDelete = wasCalledDelete,
				changeAnimation = changeAnimation,
				setImageSharedState = setImageSharedState,
				disposable = disposable,
				shareLocal = shareLocal,
				mutableStateValBitmap = mutableStateValBitmap
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
				pagerState = pagerState,
				list = list.toMutableList(),
				deleteCurrentPicture = deleteCurrentPicture,
				setFalseToWasDeletedFromNotification = setFalseToWasDeletedFromNotification,
				animationIsRunning = animationIsRunning,
				animatedVisibilityScope = animatedVisibilityScope,
				isExit = isExit,
				wasDeleted = wasDeleted,
				fromNotification = fromNotification,
				updatePicture = updatePicture,
				scope = scope,
				changeAnimation = changeAnimation,
				wasCalledDelete = wasCalledDelete,
				disposable = disposable,
				mutableStateValBitmap = mutableStateValBitmap
			)
		}
	)
}

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
	pagerState: PagerState,
	list: MutableList<String>,
	deleteCurrentPicture: (String) -> Unit,
	setFalseToWasDeletedFromNotification: () -> Unit,
	animationIsRunning: MutableState<Boolean>,
	animatedVisibilityScope: AnimatedVisibilityScope,
	isExit: MutableState<Boolean>,
	wasDeleted: MutableState<Boolean>,
	fromNotification: MutableState<Boolean>,
	updatePicture: MutableState<Boolean>,
	scope: CoroutineScope,
	changeAnimation: MutableState<Boolean>,
	wasCalledDelete: MutableState<Boolean>,
	disposable: MutableState<Boolean>,
	mutableStateValBitmap: MutableState<Bitmap?>,
)
{
	val value = state.value
	val isSharedImage = value.isSharedImage
	Log.d("checkCheck", "$isSharedImage")
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	AnimatedVisibility(!wasDeleted.value) {
		HorizontalPager(
			state = pagerState,
			pageSize = PageSize.Fill,
			contentPadding = PaddingValues(0.dp, statusBarHeightFixed, 0.dp, padding.calculateBottomPadding()),
			userScrollEnabled = !isSharedImage && !animationIsRunning.value,
			beyondViewportPageCount = 0
		) { page ->
			val url = list[page]
			var errorMessage = checkOnErrorExists(url)
			if(updatePicture.value)
			{
				LaunchedEffect(Unit) {
					errorMessage = checkOnErrorExists(url)
				}
				updatePicture.value = false
			}
			val mod = if(value.barsAreVisible)
			{
				Modifier
					.fillMaxSize()
					.clipToBounds()
					.background(Color.Transparent)
			}
			else
			{
				Modifier
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
						removeError = removeSpecialError,
						updatePicture = updatePicture
					)
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
						setImageSharedStateToFalse = setImageSharedState,
						animationIsRunning = animationIsRunning,
						wasDeleted = wasDeleted,
						pagerState = pagerState,
						page = page,
						animatedVisibilityScope = animatedVisibilityScope,
						isExit = isExit,
						fromNotification = fromNotification,
						updatePicture = updatePicture,
						scope = scope,
						wasDeletedCalled = wasCalledDelete,
						disposable = disposable,
						mutableStateValBitmap = mutableStateValBitmap
					)
				}
				if(isSharedImage && !wasCalledDelete.value)
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
									changeAnimation.value = true
									wasCalledDelete.value = true
									setImageSharedState(false)
									navigateToHome(
										changeBarsVisability = changeBarsVisability,
										postUrl = postUrl,
										navController = navController,
										setImageSharedStateToFalse = setImageSharedState,
										wasDeleted = wasDeleted,
										state = state,
										animationIsRunning = animationIsRunning,
										isExit = isExit,
										scope = scope,
										disposable = disposable
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
										scope.launch {
											if(pagerState.pageCount == 1)
											{
												navigateToHome(
													changeBarsVisability = changeBarsVisability,
													postUrl = postUrl,
													navController = navController,
													setImageSharedStateToFalse = setImageSharedState,
													state = state,
													animationIsRunning = animationIsRunning,
													isExit = isExit,
													wasDeleted = wasDeleted,
													scope = scope,
													disposable = disposable
												)
											}
											addPicture(url)
											delay(200)
											setImageSharedState(false)
										}
									},
									border = BorderStroke(
										width = 3.dp,
										color = MaterialTheme.colorScheme.primary
									),
									colors = ButtonColors(
										containerColor = MaterialTheme.colorScheme.background,
										contentColor = Color.Black,
										disabledContainerColor = Color.Black,
										disabledContentColor = Color.White
									)
								) {
									Text(
										text = addString,
										color = MaterialTheme.colorScheme.primary
									)
								}
							}
						}
					}
				}
				else
				{
					val openDialog = remember { mutableStateOf(value.wasDeletedFromNotification) }
					val cancelString = stringResource(R.string.delete_picture)
					Row(
						modifier = Modifier
							.height(80.dp)
							.align(Alignment.BottomCenter)
					) {
						val rippleConfig = remember {
							RippleConfiguration(
								color = Color.LightGray,
								rippleAlpha = RippleAlpha(
									draggedAlpha = 0.1f,
									focusedAlpha = 0f,
									hoveredAlpha = 0.5f,
									pressedAlpha = 0.6f)
							)
						}
						AnimatedVisibility(
							visible = (!animationIsRunning.value && disposable.value || fromNotification.value) && value.barsAreVisible,
							enter = EnterTransition.None,
							exit = ExitTransition.None
						) {
							CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
								Button(
									modifier = Modifier
										.align(Alignment.CenterVertically)
										.size(130.dp, 60.dp)
										.renderInSharedTransitionScopeOverlay(renderInOverlay = { false })
										.zIndex(1f),
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

					AnimatedVisibility(visible = openDialog.value) {
						AlertDialogMain(
							dialogText = null,
							dialogTitle = stringResource(R.string.do_you_really_want_to_delete_it),
							onConfirmation = {
								scope.launch {
									wasCalledDelete.value = true
									changeAnimation.value = true
									navigateToHome(
										changeBarsVisability = changeBarsVisability,
										postUrl = postUrl,
										navController = navController,
										setImageSharedStateToFalse = setImageSharedState,
										state = state,
										animationIsRunning = animationIsRunning,
										isExit = isExit,
										wasDeleted = wasDeleted,
										scope = scope,
										disposable = disposable
									)
									openDialog.value = false
									deleteCurrentPicture(url)
								}
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ShowAsynchImage(
	img: String,
	addError: (String, String) -> Unit,
	removeSpecialError: (String) -> Unit,
	navController: NavController,
	state: MutableState<DetailsScreenUiState>,
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	animationIsRunning: MutableState<Boolean>,
	wasDeleted: MutableState<Boolean>,
	pagerState: PagerState,
	page: Int,
	animatedVisibilityScope: AnimatedVisibilityScope,
	isExit: MutableState<Boolean>,
	fromNotification: MutableState<Boolean>,
	updatePicture: MutableState<Boolean>,
	scope: CoroutineScope,
	wasDeletedCalled: MutableState<Boolean>,
	disposable: MutableState<Boolean>,
	mutableStateValBitmap: MutableState<Bitmap?>,
)
{
	val zoom = rememberZoomState(5f, Size.Zero)
	if(isExit.value)
	{
		LaunchedEffect(Unit) {
			zoom.reset()
		}
	}
	val context = LocalContext.current
	val headers = remember {
		NetworkHeaders.Builder()
			.set("Cache-Control", "max-age=604800, must-revalidate, stale-while-revalidate=86400")
			.build()
	}
	val placeHolderM = remember { mutableIntStateOf(R.drawable.loading) }
	if(img.contains(".gif"))
	{
		placeHolderM.intValue = R.drawable.empty
		LaunchedEffect(Unit) {
			while(animationIsRunning.value)
			{
				delay(200)
			}
			placeHolderM.intValue = R.drawable.loading
		}
	}
	val imgRequest =
		ImageRequest.Builder(context)
			.data(img)
			.httpHeaders(headers)
			.error(R.drawable.error)
			.memoryCacheKey(img)
			.placeholderMemoryCacheKey(img)
			.placeholder(placeHolderM.intValue)
			.diskCacheKey(img)
			.build()
	if (img.startsWith("content://"))
	{
		val imgRequestLocalImage =
			ImageRequest.Builder(context)
				.data(img)
				.networkCachePolicy(CachePolicy.DISABLED)
				.error(R.drawable.error)
				.memoryCacheKey(img)
				.target(
					onSuccess = { result ->
						if(img.startsWith("content://"))
						{
							mutableStateValBitmap.value = result.toBitmap()
						}
					}
				)
				.placeholderMemoryCacheKey(img)
				.placeholder(placeHolderM.intValue)
				.diskCacheKey(img)
				.build()
		val imageLoader = ImageLoader(context).newBuilder().build()
		imageLoader.enqueue(imgRequestLocalImage)
	}
	//setting up the size of zoomable space on screen to avoid zooming in empty places
	var imageSize by remember { mutableStateOf(Size.Zero) }
	LaunchedEffect(imageSize) {
		if(imageSize != Size.Zero && imageSize.width >= 0 && imageSize.height >= 0)
		{
			zoom.setContentSize(imageSize)
			Log.d("imageSize 2", "$imageSize")
		}
	}
	val value = state.value
	val isSharedImage = value.isSharedImage
	Box(Modifier
		.fillMaxSize()
		.zoomable(
			zoomState = zoom,
			zoomEnabled = !animationIsRunning.value,
			enableOneFingerZoom = false,
			onTap =
			{
				if(!animationIsRunning.value)
				{
					val visibility = state.value.barsAreVisible
					changeBarsVisability(!visibility)
				}
			}
		)
		.pointerInput(Unit) {
			if(!isSharedImage)
			{
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
									state = state,
									wasDeleted = wasDeleted,
									animationIsRunning = animationIsRunning,
									isExit = isExit,
									scope = scope,
									disposable = disposable
								)
							}
						}
						countLastThree.clear()
						count.add(changes.size)
					}
				}
			}
		}) {
		if(isExit.value)
		{
			fromNotification.value = false
		}
		val mod = if(isSharedImage || wasDeleted.value || page != pagerState.currentPage || fromNotification.value || wasDeletedCalled.value)
		{
			Modifier
				.fillMaxSize()
				.padding(8.dp, 64.dp, 8.dp, 8.dp)
				.clip(RoundedCornerShape(8.dp))
				.align(Alignment.Center)
				.background(Color.Transparent)
		}
		else
		{
			Modifier
				.padding(8.dp, 64.dp, 8.dp, 8.dp)
				.sharedElement(
					state = rememberSharedContentState(
						key = pagerState.currentPage
					),
					animatedVisibilityScope = animatedVisibilityScope)
				.clip(RoundedCornerShape(8.dp))
				.align(Alignment.Center)
				.fillMaxSize()
		}
		val width = remember { mutableFloatStateOf(0f) }
		val height = remember { mutableFloatStateOf(0f) }
		Log.d("recompose", "details $img")
		AsyncImage(
			model = imgRequest,
			filterQuality = FilterQuality.Low,
			contentDescription = null,
			contentScale = ContentScale.Fit,
			onSuccess = {
				removeSpecialError(img)
				val image = it.result.image
				width.floatValue = image.width.toFloat()
				height.floatValue = image.height.toFloat()
			},
			onError = {
				addError(img, it.result.throwable.message.toString())
				updatePicture.value = true
			},
			modifier = mod
				.onGloballyPositioned { layoutCoordinates ->
					run {
						val size = layoutCoordinates.size
						var w = size.width.toFloat()
						var h = size.height.toFloat()
						val heightLocal = height.floatValue
						val widthLocal = width.floatValue
						if(h > w)
						{
							h = heightLocal * w / widthLocal
							imageSize = Size(w, h)
						}
						else
						{
							w = widthLocal * h / heightLocal
							imageSize = Size(w, h)
						}
					}
				}
		)
	}
}

@Composable
fun ShowError(
	context: Context,
	currentUrl: String,
	isValidUrl: (String) -> Boolean,
	removeError: (String) -> Unit,
	updatePicture: MutableState<Boolean>,
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
			val color = colorResource(R.color.orange)
			val gradientColor = remember { listOf(color, Color.Yellow) }
			GradientButton(
				gradientColors = gradientColor,
				cornerRadius = 30.dp,
				nameButton = stringResource(R.string.try_again),
				roundedCornerShape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp),
				context = context,
				url = currentUrl,
				removeError = removeError,
				updatePicture = updatePicture
			)
		}
	}
}

@Composable
fun GradientButton(
	gradientColors: List<Color>,
	cornerRadius: Dp,
	nameButton: String,
	roundedCornerShape: RoundedCornerShape,
	context: Context,
	url: String,
	removeError: (String) -> Unit,
	updatePicture: MutableState<Boolean>,
)
{
	Button(
		modifier = Modifier
			.widthIn(max = 600.dp)
			.padding(start = 32.dp, end = 32.dp),
		onClick = {
			removeError(url)
			updatePicture.value = true
			Toast.makeText(context, R.string.reload_pic, Toast.LENGTH_LONG).show()
		},
		colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
		shape = RoundedCornerShape(cornerRadius)
	)
	{
		Box(modifier = Modifier
			.fillMaxWidth()
			.background(
				brush = Brush.horizontalGradient(colors = gradientColors),
				shape = roundedCornerShape
			)
			.clip(roundedCornerShape)
			.background(
				brush = Brush.linearGradient(colors = gradientColors),
				shape = RoundedCornerShape(cornerRadius)
			)
			.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center)
		{
			Text(
				text = nameButton,
				fontSize = 20.sp,
				color = Color.White
			)
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
	isExit: MutableState<Boolean>,
	wasDeleted: MutableState<Boolean>,
	fromNotification: MutableState<Boolean>,
	scope: CoroutineScope,
	changeAnimation: MutableState<Boolean>,
	wasCalledDelete: MutableState<Boolean>,
	setImageSharedState: (Boolean) -> Unit,
	disposable: MutableState<Boolean>,
	shareLocal: (String) -> Unit,
	mutableStateValBitmap: MutableState<Bitmap?>,
)
{
	val value = state.value
	if(value.wasSharedFromNotification && !currentPicture.startsWith("content://"))
	{
		share(currentPicture)
		postWasSharedState()
	}
	val navBack = remember { mutableStateOf(false) }
	Log.d("shared pic url", currentPicture)
	val sharedImgCase = value.isSharedImage
	val sysBarsInsets = WindowInsets.systemBarsIgnoringVisibility
	val sysBarsWithCutoutsInsets = sysBarsInsets.union(WindowInsets.displayCutout)
	AnimatedVisibility(visible = (isVisible && !animationIsRunning.value && disposable.value) || fromNotification.value, enter = EnterTransition.None, exit = ExitTransition.None) {
		Box(modifier = Modifier
			.background(MaterialTheme.colorScheme.background)
			.height(
				sysBarsInsets
					.asPaddingValues()
					.calculateTopPadding() + 64.dp
			)
			.fillMaxWidth())
		val rippleConfig = remember {
			RippleConfiguration(
				color = Color.Gray,
				rippleAlpha = RippleAlpha(
					draggedAlpha = 0.1f,
					focusedAlpha = 0f,
					hoveredAlpha = 0.5f,
					pressedAlpha = 0.6f
				)
			)
		}
		CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
			Box(modifier = Modifier
				.wrapContentSize()
				.windowInsetsPadding(sysBarsWithCutoutsInsets)) {
				TopAppBar(
					modifier = Modifier
						.align(Alignment.Center)
						.windowInsetsPadding(sysBarsWithCutoutsInsets)
						.wrapContentSize(),
					title = {
						Box(modifier = Modifier
							.windowInsetsPadding(sysBarsWithCutoutsInsets)
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
							.windowInsetsPadding(sysBarsWithCutoutsInsets)
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
						AnimatedVisibility(visible = !sharedImgCase) {
							Box(modifier = Modifier
								.windowInsetsPadding(sysBarsWithCutoutsInsets)
								.height(64.dp)
								.width(50.dp)
								.clickable {
									if(currentPicture.startsWith("content://"))
									{
										val bitmap = mutableStateValBitmap.value
										if(bitmap != null)
										{
											shareLocal(currentPicture)
										}
									}
									else
									{
										share(currentPicture)
									}
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
				HorizontalDivider(
					modifier = Modifier
						.fillMaxWidth()
						.alpha(0.12f)
						.align(Alignment.BottomCenter),
					color = MaterialTheme.colorScheme.onPrimary,
					thickness = 1.5.dp
				)
			}
		}
	}
	if(navBack.value)
	{
		if(value.isSharedImage)
		{
			changeAnimation.value = true
			wasCalledDelete.value = true
			setImageSharedState(false)
		}
		navBack.value = false
		navigateToHome(
			changeBarsVisability = changeBarsVisability,
			postUrl = postUrl,
			navController = nc,
			setImageSharedStateToFalse = setImageSharedStateToFalse,
			wasDeleted = wasDeleted,
			state = state,
			animationIsRunning = animationIsRunning,
			isExit = isExit,
			scope = scope,
			disposable = disposable
		)
	}
}

fun navigateToHome(
	changeBarsVisability: (Boolean) -> Unit,
	postUrl: (String?, Bitmap?) -> Unit,
	navController: NavController,
	setImageSharedStateToFalse: (Boolean) -> Unit,
	state: MutableState<DetailsScreenUiState>,
	wasDeleted: MutableState<Boolean>,
	animationIsRunning: MutableState<Boolean>,
	isExit: MutableState<Boolean>,
	scope: CoroutineScope,
	disposable: MutableState<Boolean>,
)
{
	scope.launch {
		if(!animationIsRunning.value && disposable.value)
		{
			isExit.value = true

			if(state.value.isSharedImage)
			{
				wasDeleted.value = true
			}
			Log.d("activated", "activated")
			animationIsRunning.value = true
			delay(100)
			navController.navigate(Screen.Home.route) {
				this.launchSingleTop = true
			}
			navController.clearBackStack(Screen.Details.route)
			changeBarsVisability(true)
			postUrl(null, null)
			delay(300)
			setImageSharedStateToFalse(false)
		}
	}
}
