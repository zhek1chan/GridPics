package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.NULL_STRING
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.pictures.isValidUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(nc: NavController, vmDetails: DetailsViewModel, vmPictures: PicturesViewModel)
{
	val scope = rememberCoroutineScope()
	BackHandler {
		scope.launch {
			vmDetails.observeFlow().collectLatest {
				if(it)
				{
					vmDetails.changeState()
					nc.navigateUp()
				}
				else
				{
					nc.navigateUp()
				}
			}
		}
	}
	val context = LocalContext.current
	val pictures = context.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, NULL_STRING)
	val pic = context.getSharedPreferences(PIC, MODE_PRIVATE).getString(PIC, NULL_STRING)
	if(pic != NULL_STRING && pictures != NULL_STRING)
	{
		val list = remember { pictures!!.split("\n").toMutableList() }
		val pagerState = rememberPagerState(pageCount = { list.size })
		val isVisible = remember { mutableStateOf(true) }
		Scaffold(
			contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
			topBar = { AppBar(isVisible, context, nc, list, pagerState) },
			content = { padding ->
				ShowDetails(pic!!, vmDetails, nc, isVisible, list, pagerState, context, padding, vmPictures)
			}
		)
	}
}

@SuppressLint("CoroutineCreationDuringComposition", "UseCompatLoadingForDrawables")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowDetails(
	img: String,
	vm: DetailsViewModel,
	nc: NavController,
	isVisible: MutableState<Boolean>,
	list: MutableList<String>,
	pagerState: PagerState,
	context: Context,
	padding: PaddingValues,
	vmPictures: PicturesViewModel,
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
		contentPadding = PaddingValues(0.dp, topBarHeight + statusBarHeightFixed, 0.dp, 0.dp),
		userScrollEnabled = true
	) { page ->
		if(firstPage.value)
		{
			scope.launch(Dispatchers.Main) {
				pagerState.scrollToPage(startPage)
			}
		}
		firstPage.value = false
		when
		{
			vmPictures.checkOnErrorExists(list[page]) ->
			{
				ShowError(context, list, page)
			}
			!vmPictures.checkOnErrorExists(list[page]) ->
			{
				ShowAsynchImage(
					list = list,
					page = page,
					vm = vm,
					nc = nc,
					isVisible = isVisible,
					exit = exit
				)
			}
		}
	}
}

@Composable
fun ShowAsynchImage(
	list: MutableList<String>,
	page: Int,
	vm: DetailsViewModel,
	nc: NavController,
	isVisible: MutableState<Boolean>,
	exit: MutableState<Boolean>,
)
{
	val orientation = LocalContext.current.resources.configuration.orientation
	val scale = if(orientation == Configuration.ORIENTATION_PORTRAIT)
	{
		ContentScale.FillWidth
	}
	else
	{
		ContentScale.FillHeight
	}
	val zoom = rememberZoomState()
	val count = remember { listOf(0).toMutableList() }
	val countLastFive = remember { listOf(0).toMutableList() }
	val imgRequest = ImageRequest.Builder(LocalContext.current)
		.data(list[page])
		.networkCachePolicy(CachePolicy.DISABLED)
		.build()
	AsyncImage(
		model = imgRequest,
		contentDescription = "",
		contentScale = scale,
		modifier = Modifier
			.fillMaxSize()
			.zoomable(zoom, enableOneFingerZoom = false, onTap = {
				vm.changeState()
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
							countLastFive.add(count[count.lastIndex])
							countLastFive.add(count[count.lastIndex - 1])
							countLastFive.add(count[count.lastIndex - 2])
						}
						if(zoom.scale < 0.92.toFloat() && exit.value && countLastFive.max() == 2 && count[count.lastIndex] < 2)
						{
							if(!isVisible.value)
							{
								vm.changeState()
							}
							nc.navigateUp()
						}
						countLastFive.clear()
						count.add(event.changes.size)
					}
				}
			}
	)
}

@Composable
fun ShowError(
	context: Context,
	list: MutableList<String>,
	currentPage: Int,
)
{
	val errorMessage = if(isValidUrl(list[currentPage]))
	{
		"HTTP error: 404"
	}
	else
	{
		context.getString(R.string.link_is_not_valid)
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
		if(errorMessage != context.getString(R.string.link_is_not_valid))
		{
			Button(
				onClick =
				{
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
				.clickable {
					navBack = true
				},
			title = {
				Text(
					text = list[pagerState.currentPage],
					fontSize = 18.sp,
					maxLines = 2,
					modifier = Modifier
						.clickable { navBack = true },
					overflow = TextOverflow.Ellipsis,
				)
			},
			navigationIcon = {
				IconButton({ navBack = true }) {
					Icon(
						Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = "back"
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
						share(list[pagerState.currentPage], context)
					}
				) {
					Icon(
						painter = rememberVectorPainter(Icons.Default.Share),
						contentDescription = "share",
						tint = MaterialTheme.colorScheme.onPrimary
					)
				}
			}
		)

		if(navBack)
		{
			navBack = false
			nc.navigateUp()
		}
	}
}

fun share(text: String, context: Context)
{
	val sendIntent = Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = context.resources.getString(R.string.text_plain)
	}
	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(context, shareIntent, null)
}