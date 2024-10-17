package com.example.gridpics.ui.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.PIC
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.pictures.AlertDialogMain
import com.example.gridpics.ui.pictures.AlertDialogSecondary
import com.example.gridpics.ui.pictures.isValidUrl
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

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

@SuppressLint("UnrememberedMutableState", "CoroutineCreationDuringComposition")
@Composable
fun ShowDetails(img: String, vm: DetailsViewModel, nc: NavController, pictures: String)
{
	val isVisible = remember { mutableStateOf(true) }
	ComposeTheme {
		val list = pictures.split("\n").toMutableList()
		val pagerState = rememberPagerState(pageCount = {
			list.size
		})
		val context = LocalContext.current
		val firstPage = remember { mutableStateOf(true) }
		val startPage = list.indexOf(img)
		val currentPage = remember { mutableIntStateOf(startPage) }
		HorizontalPager(state = pagerState, modifier = Modifier
			.fillMaxSize()) { page ->
			val scope = rememberCoroutineScope()
			if(firstPage.value)
			{
				scope.launch {
					pagerState.scrollToPage(startPage)
				}.isActive
			}
			firstPage.value = false
			currentPage.intValue = page
			val openAlertDialog = remember { mutableStateOf(false) }
			when
			{
				openAlertDialog.value ->
				{
					if(!isValidUrl(list[currentPage.intValue]))
					{
						AlertDialogMain(onDismissRequest = {
							openAlertDialog.value = false
							scope.launch {
								pagerState.scrollToPage(currentPage.intValue + 1)
							}
						}, onConfirmation = {
							openAlertDialog.value = false
						}, dialogTitle = stringResource(R.string.error_ocurred_loading_img), dialogText = "Произошла ошибка при загрузке :(" + "\nПопробовать загрузить повторно?", icon = Icons.Default.Warning)
					}
					else
					{
						AlertDialogSecondary(onDismissRequest = { openAlertDialog.value = false }, onConfirmation = {
							openAlertDialog.value = false
							scope.launch {
								pagerState.scrollToPage(currentPage.intValue + 1)
							}
						}, dialogTitle = stringResource(R.string.error_ocurred_loading_img), dialogText = stringResource(R.string.link_is_not_valid), icon = Icons.Default.Warning)
					}
				}
				!openAlertDialog.value ->
				{
					Image(
						painter = rememberAsyncImagePainter(list[page], onError = { openAlertDialog.value = true }, onSuccess = { openAlertDialog.value = false }),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize()
							.zoomable(rememberZoomState(), enableOneFingerZoom = false, onTap = {
								vm.changeState()
								isVisible.value = !isVisible.value
							}))
				}
			}
		}
		LaunchedEffect(key1 = Unit, block = {
			var initPage = Int.MAX_VALUE / 2
			while(initPage % list.size != 0)
			{
				initPage++
			}
			pagerState.scrollToPage(initPage)
		})
		AnimatedVisibility(visible = isVisible.value) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(40.dp)) {
				var navBack by remember { mutableStateOf(false) }
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