package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(nc: NavController, viewModel: DetailsViewModel)
{
	BackHandler {
		if(viewModel.observeState().value == true)
		{
			viewModel.changeState()
			nc.navigateUp()
		}
		else
		{
			nc.navigateUp()
		}
	}
	val context = LocalContext.current
	val pictures = context.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, "null")
	val pic = context.getSharedPreferences(PIC, MODE_PRIVATE).getString(PIC, "null")
	if(pic != null)
	{
		ShowDetails(pic, viewModel, nc, pictures!!)
	}
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition")
@Composable
fun ShowDetails(img: String, vm: DetailsViewModel, nc: NavController, pictures: String)
{
	val isVisible = remember { mutableStateOf(true) }
	ComposeTheme {
		val list = pictures.split("\n")
		val pagerState = rememberPagerState(pageCount = {
			list.size
		})
		val firstPage = remember { mutableStateOf(true) }
		val startPage = list.indexOf(img)
		val currentPage = remember { mutableIntStateOf(startPage) }
		HorizontalPager(state = pagerState, userScrollEnabled = true) { page ->
			val scope = rememberCoroutineScope()
			if(firstPage.value)
			{
				scope.launch {
					pagerState.scrollToPage(startPage)
				}.isActive
			}
			firstPage.value = false
			currentPage.intValue = page
			var scale by remember { mutableFloatStateOf(1f) }
			var offset by remember { mutableStateOf(Offset(0f, 0f)) }
			Log.d("DetailsScreen", "We are on the ${currentPage.intValue} page")
			Image(
				painter = rememberAsyncImagePainter(list[page]),
				contentDescription = null,
				modifier = Modifier
					.fillMaxSize()
					.combinedClickable(
						onDoubleClick = {
							scope.launch {
								pagerState.scrollToPage(currentPage.intValue - 1)
							}
						},
						onClick = {
							scope.launch {
								pagerState.scrollToPage(currentPage.intValue + 1)
							}
						},
						onLongClick = {
							vm.changeState()
							isVisible.value = !isVisible.value
							scope.launch {
								pagerState.scrollToPage(currentPage.intValue)
							}
						})
					.pointerInput(Unit) {
						detectTransformGestures { _, pan, zoom, _ -> // Update the scale based on zoom gestures.
							scale *= zoom   // Limit the zoom levels within a certain range (optional).
							scale = scale.coerceIn(-1f, 3f) // Update the offset to implement panning when zoomed.
							offset = if(scale == 1f) Offset(0f, 0f) else offset + pan
							if(scale < 0.5f)
							{
								Log.d("DetailsScreen", "Zoomed out, navigating back")
								if(vm.observeState().value == true)
								{
									vm.changeState()
								}
								nc.navigateUp()
							}
						}
					}
					.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y))
		}
		AnimatedVisibility(visible = isVisible.value) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(40.dp)) {
				var navBack by remember { mutableStateOf(false) }
				val context = LocalContext.current
				@OptIn(ExperimentalMaterial3Api::class) TopAppBar(title = {
					Text(
						list[currentPage.intValue],
						fontSize = 18.sp,
						maxLines = 2,
						modifier = Modifier.padding(0.dp, 5.dp, 30.dp, 0.dp),
						overflow = TextOverflow.Ellipsis,
					)
				},
					navigationIcon = {
						IconButton({ navBack = true }) {
							Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 0.dp))
						}
					}, colors = TopAppBarDefaults.topAppBarColors(titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
						actionIconContentColor = MaterialTheme.colorScheme.onPrimary))
				Icon(
					modifier = Modifier
						.align(Alignment.TopEnd)
						.padding(0.dp, 10.dp, 15.dp, 0.dp)
						.clickable {
							share(list[currentPage.intValue], context)
						},
					painter = rememberVectorPainter(Icons.Default.Share),
					contentDescription = "share",
					tint = MaterialTheme.colorScheme.onPrimary
				)
				if(navBack)
				{
					navBack = false
					nc.navigateUp()
				}
			}
		}
	}
}

fun share(text: String, context: Context)
{
	val sendIntent = Intent(Intent.ACTION_SEND).apply {
		putExtra(Intent.EXTRA_TEXT, text)
		type = "text/plain"
	}
	val shareIntent = Intent.createChooser(sendIntent, null)
	startActivity(context, shareIntent, null)
}
