package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.pictures.isValidUrl
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun DetailsScreen(nc: NavController, viewModel: DetailsViewModel)
{
	val scope = rememberCoroutineScope()
	BackHandler {
		scope.launch {
			viewModel.observeState().collectLatest {
				if(it)
				{
					viewModel.changeState()
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
	val pictures = context.getSharedPreferences(PICTURES, MODE_PRIVATE).getString(PICTURES, stringResource(R.string.null_null))
	val pic = context.getSharedPreferences(PIC, MODE_PRIVATE).getString(PIC, stringResource(R.string.null_null))
	if(pic != null)
	{
		ShowDetails(pic, viewModel, nc, pictures!!)
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowDetails(img: String, vm: DetailsViewModel, nc: NavController, pictures: String)
{
	val isVisible = remember { mutableStateOf(true) }
	ComposeTheme {
		val list = remember { pictures.split("\n").toMutableList() }
		val pagerState = rememberPagerState(pageCount = {
			list.size
		})
		val context = LocalContext.current
		val firstPage = remember { mutableStateOf(true) }
		val startPage = list.indexOf(img)
		val currentPage = remember { mutableIntStateOf(startPage) }
		var padding = remember { PaddingValues(0.dp, 0.dp, 0.dp, 0.dp) }
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !isVisible.value)
		{
			padding = PaddingValues(0.dp, 0.dp, 0.dp, 24.dp)
		}
		Log.d("WINDOW", "${WindowInsets.systemBarsIgnoringVisibility}")
		HorizontalPager(state = pagerState, pageSize = PageSize.Fill, modifier = Modifier
			.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
			.padding(padding), contentPadding = PaddingValues(0.dp, 30.dp)) { page ->
			val scope = rememberCoroutineScope()
			if(firstPage.value)
			{
				scope.launch {
					pagerState.scrollToPage(startPage)
				}
			}
			firstPage.value = false
			currentPage.intValue = page
			Log.d("DetailsFragment", "current page ${currentPage.intValue}")
			val openAlertDialog = remember { mutableStateOf(false) }
			if(!isValidUrl(list[currentPage.intValue]))
			{
				openAlertDialog.value = true
			}
			when
			{
				openAlertDialog.value ->
				{
					val errorMessage = if(isValidUrl(list[currentPage.intValue]))
					{
						"HTTP error: 404"
					}
					else
					{
						context.getString(R.string.link_is_not_valid)
					}
					Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
						Text(text = stringResource(R.string.error_ocurred_loading_img), modifier = Modifier.padding(5.dp), color = MaterialTheme.colorScheme.onPrimary)
						Text(text = errorMessage, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onPrimary)
						if(errorMessage != context.getString(R.string.link_is_not_valid))
						{
							Button(onClick = {
								scope.launch {
									pagerState.scrollToPage(page)
								}
							}, colors = ButtonColors(Color.LightGray, Color.Black, Color.Black, Color.White)) {
								Text(stringResource(R.string.update_loading))
							}
						}
					}
				}
				!openAlertDialog.value ->
				{
					val zoom = rememberZoomState()
					Image(painter = rememberAsyncImagePainter(list[page], onError = {
						openAlertDialog.value = true
					}, onSuccess = { openAlertDialog.value = false }), contentDescription = null, modifier = Modifier
						.fillMaxSize()
						.zoomable(zoom, enableOneFingerZoom = false, onTap = {
							vm.changeState()
							isVisible.value = !isVisible.value
						}))
					if(zoom.scale < 0.9)
					{
						scope.launch {
							vm.observeState().collectLatest {
								if(it)
								{
									vm.changeState()
									nc.navigateUp()
								}
								else
								{
									nc.navigateUp()
								}
							}
						}
					}
				}
			}
		}
		AnimatedVisibility(visible = isVisible.value, enter = EnterTransition.None, exit = ExitTransition.None) {
			Box(modifier = Modifier
				.fillMaxWidth()
				.height(60.dp)) {
				var navBack by remember { mutableStateOf(false) }
				ConstraintLayout(modifier = Modifier.clickable(onClick = {
					navBack = true
				}, interactionSource = remember { MutableInteractionSource() },
					indication = ripple(color = MaterialTheme.colorScheme.onPrimary, bounded = true))) {
					val (icon, text) = createRefs()
					@OptIn(ExperimentalMaterial3Api::class)
					TopAppBar(title = {
						Text(
							list[pagerState.currentPage],
							fontSize = 18.sp,
							maxLines = 2,
							modifier = Modifier
								.padding(0.dp, 0.dp, 40.dp, 0.dp)
								.constrainAs(text) {
									top.linkTo(parent.top)
									bottom.linkTo(parent.bottom)
								},
							overflow = TextOverflow.Ellipsis,
						)
					}, navigationIcon = {
						IconButton({ navBack = true }) {
							Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))
						}
					}, colors = TopAppBarDefaults.topAppBarColors(titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
						actionIconContentColor = MaterialTheme.colorScheme.onPrimary, containerColor = MaterialTheme.colorScheme.background))

					IconButton({ share(list[pagerState.currentPage], context) }, modifier = Modifier
						.constrainAs(icon) {
							end.linkTo(parent.end)
							top.linkTo(parent.top)
							bottom.linkTo(parent.bottom)
						}
						.padding(0.dp, 0.dp, 10.dp, 0.dp)) {
						Icon(painter = rememberVectorPainter(Icons.Default.Share), contentDescription = "share", tint = MaterialTheme.colorScheme.onPrimary)
					}
					if(navBack)
					{
						navBack = false
						nc.navigateUp()
					}
				}
			}
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