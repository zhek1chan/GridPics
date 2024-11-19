package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Base64
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
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
import com.example.gridpics.ui.activity.MainActivity.Companion.NULL_STRING
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURE
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFS_PICTURES
import com.example.gridpics.ui.activity.MainActivity.Companion.TOP_BAR_VISABILITY_SHARED_PREFERENCE
import com.example.gridpics.ui.activity.MainActivity.Companion.WE_WERE_HERE_BEFORE
import com.example.gridpics.ui.activity.Screen
import com.example.gridpics.ui.pictures.PicturesViewModel
import com.example.gridpics.ui.pictures.isValidUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import java.io.ByteArrayOutputStream

@SuppressLint("RestrictedApi", "CommitPrefEdits", "ApplySharedPref", "UseCompatLoadingForDrawables", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(nc: NavController, vmDetails: DetailsViewModel, vmPictures: PicturesViewModel)
{
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	BackHandler {
		scope.launch(Dispatchers.Main) {
			vmDetails.observeVisabilityFlow().collectLatest {
				if(it)
				{
					Log.d("we are out", "We are out")
					vmDetails.postUrl("default", "")
					vmDetails.changeVisabilityState()
					nc.navigateUp()
				}
				else
				{
					Log.d("we are out", "We are without changing state")
					vmDetails.postUrl("default", "")
					nc.navigateUp()
				}
			}
		}
	}
	val pictures = context.getSharedPreferences(SHARED_PREFS_PICTURES, MODE_PRIVATE).getString(SHARED_PREFS_PICTURES, NULL_STRING)
	val pic = context.getSharedPreferences(PICTURE, MODE_PRIVATE).getString(PICTURE, NULL_STRING)
	var visible = context.getSharedPreferences(TOP_BAR_VISABILITY_SHARED_PREFERENCE, MODE_PRIVATE).getBoolean(TOP_BAR_VISABILITY_SHARED_PREFERENCE, true)
	val weWereHere = context.getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE).getBoolean(WE_WERE_HERE_BEFORE, false)
	if(weWereHere)
	{
		visible = true
	}
	val vis = context.getSharedPreferences(WE_WERE_HERE_BEFORE, MODE_PRIVATE)
	val editorVis = vis.edit()
	editorVis.putBoolean(WE_WERE_HERE_BEFORE, true)
	editorVis.apply()
	if(pic != NULL_STRING && pictures != NULL_STRING)
	{
		val list = remember { pictures!!.split("\n").toMutableList() }
		val pagerState = rememberPagerState(initialPage = list.indexOf(pic), pageCount = { list.size })
		val bitmapString = remember { mutableStateOf("") }
		CoroutineScope(Dispatchers.Default).launch {
			val imgRequest =
				ImageRequest.Builder(context)
					.data(list[pagerState.currentPage])
					.placeholder(R.drawable.loading)
					.error(R.drawable.error)
					.allowHardware(false)
					.target {
						val picture = it.toBitmap()
						val baos = ByteArrayOutputStream()
						if (picture.byteCount > 2*1024*1024)
						{
							picture.compress(Bitmap.CompressFormat.JPEG, 5, baos)
						} else {
							picture.compress(Bitmap.CompressFormat.JPEG, 50, baos)
						}
						val b = baos.toByteArray()
						bitmapString.value = Base64.encodeToString(b, Base64.DEFAULT)
						Log.d("checkMa", "tipa zagruzilos'")
					}
					.networkCachePolicy(CachePolicy.ENABLED)
					.diskCachePolicy(CachePolicy.ENABLED)
					.diskCacheKey(list[pagerState.currentPage])
					.memoryCachePolicy(CachePolicy.ENABLED)
					.build()

			ImageLoader(context).newBuilder().build().enqueue(imgRequest)
		}
		vmDetails.postUrl(list[pagerState.currentPage], bitmapString.value)
		val isVisible = remember { mutableStateOf(visible) }
		Scaffold(
			contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
			topBar = { AppBar(isVisible, context, nc, list, pagerState) },
			content = { padding ->
				ShowDetails(
					pic!!,
					vmDetails,
					nc,
					isVisible,
					list,
					pagerState,
					context,
					padding,
					vmPictures,
					bitmapString
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
	vm: DetailsViewModel,
	nc: NavController,
	isVisible: MutableState<Boolean>,
	list: MutableList<String>,
	pagerState: PagerState,
	context: Context,
	padding: PaddingValues,
	vmPictures: PicturesViewModel,
	bitmapString: MutableState<String>
)
{
	padding.calculateBottomPadding()
	val firstPage = remember { mutableStateOf(true) }
	val startPage = list.indexOf(img)
	val exit = remember { mutableStateOf(false) }
	val topBarHeight = 64.dp
	val scope = rememberCoroutineScope()
	val statusBarHeightFixed = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
	var multiWindowed by remember { mutableStateOf(false) }
	scope.launch {
		vm.observeMultiWindowFlow().collectLatest {
			multiWindowed = it
		}
	}

	HorizontalPager(
		state = pagerState,
		pageSize = PageSize.Fill,
		contentPadding = PaddingValues(0.dp, topBarHeight + statusBarHeightFixed, 0.dp, WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()),
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
				ShowError(
					context = context,
					list = list,
					currentPage = page,
					pagerState = pagerState,
					bitmapString = bitmapString)
			}
			!vmPictures.checkOnErrorExists(list[page]) ->
			{
				ShowAsynchImage(
					list = list,
					page = page,
					vm = vm,
					nc = nc,
					isVisible = isVisible,
					exit = exit,
					multiWindow = multiWindowed,
					picturesViewModel = vmPictures,
					context = context
				)
			}
		}
		MainActivity.countExitNavigation++
		saveToSharedPrefs(context, list[pagerState.currentPage], isVisible.value)
	}
}

@SuppressLint("UseCompatLoadingForDrawables")
@Composable
fun ShowAsynchImage(
	list: MutableList<String>,
	page: Int,
	vm: DetailsViewModel,
	picturesViewModel: PicturesViewModel,
	nc: NavController,
	isVisible: MutableState<Boolean>,
	exit: MutableState<Boolean>,
	multiWindow: Boolean,
	context: Context
)
{
	val orientation = context.resources.configuration.orientation
	val scale = if(!multiWindow)
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
	val imgRequest = ImageRequest.Builder(context)
		.data(list[page])
		.placeholder(R.drawable.loading)
		.error(R.drawable.loading)
		.allowHardware(false)
		.diskCacheKey(list[page])
		.networkCachePolicy(CachePolicy.ENABLED)
		.build()
	AsyncImage(
		model = imgRequest,
		contentDescription = "",
		contentScale = scale,
		onSuccess = {
			picturesViewModel.removeSpecialError(list[page])
		},
		onError = {
			picturesViewModel.addError(list[page])
			nc.navigate(Screen.Details.route)
		},
		modifier = Modifier
			.fillMaxSize()
			.zoomable(zoom, enableOneFingerZoom = false, onTap = {
				vm.changeVisabilityState()
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
								vm.postNegativeVisabilityState()
								nc.navigateUp()
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
	bitmapString: MutableState<String>
)
{
	val picture = context.resources.getDrawable(R.drawable.error, null).toBitmap()
	val baos = ByteArrayOutputStream()
	if (picture.byteCount > 2*1024*1024)
	{
		picture.compress(Bitmap.CompressFormat.JPEG, 5, baos)
	} else {
		picture.compress(Bitmap.CompressFormat.JPEG, 50, baos)
	}
	val b = baos.toByteArray()
	bitmapString.value = Base64.encodeToString(b, Base64.DEFAULT)
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
					Toast.makeText(context, context.getString(R.string.reload_pic), Toast.LENGTH_LONG).show()
					CoroutineScope(Dispatchers.Main).launch {
						pagerState.scrollToPage(currentPage)
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
				IconButton(
					onClick =
					{
						share(list[pagerState.currentPage], context)
					}
				) {
					Icon(
						painter = rememberVectorPainter(Icons.Default.Share),
						contentDescription = "share",
						tint = MaterialTheme.colorScheme.onPrimary,
					)
				}
			}
		)
		if(navBack)
		{
			navBack = false
			nc.navigate(BottomNavItem.Home.route)
		}
	}
}

private fun saveToSharedPrefs(context: Context, s: String, vBoolean: Boolean)
{
	val pic = context.getSharedPreferences(PICTURE, MODE_PRIVATE)
	val editor = pic.edit()
	editor.putString(PICTURE, s)
	editor.apply()
	val vis = context.getSharedPreferences(TOP_BAR_VISABILITY_SHARED_PREFERENCE, MODE_PRIVATE)
	val editorVis = vis.edit()
	editorVis.putBoolean(TOP_BAR_VISABILITY_SHARED_PREFERENCE, vBoolean)
	editorVis.apply()
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