package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import coil3.imageLoader
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavigationBar
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFERENCE_GRIDPICS
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SHARED_PREFERENCE
import com.example.gridpics.ui.pictures.AlertDialogMain
import com.example.gridpics.ui.pictures.state.PicturesScreenUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
	navController: NavController,
	option: MutableState<ThemePick>,
	changeTheme: (Int) -> Unit,
	isScreenInPortraitState: MutableState<PicturesScreenUiState>,
	clearImageCache: () -> Unit,
	postStartOfPager: () -> Unit
)
{
	postStartOfPager()
	val windowInsets = if(!isScreenInPortraitState.value.isPortraitOrientation)
	{
		WindowInsets.displayCutout.union(WindowInsets.statusBarsIgnoringVisibility)
	}
	else
	{
		WindowInsets.statusBarsIgnoringVisibility
	}
	Scaffold(
		contentWindowInsets = windowInsets,
		bottomBar = { BottomNavigationBar(navController) },
		topBar = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsPadding(windowInsets)
					.padding(16.dp, 0.dp)
					.height(60.dp)
			) {
				Text(
					textAlign = TextAlign.Center,
					text = stringResource(R.string.settings),
					fontSize = 21.sp,
					color = MaterialTheme.colorScheme.onPrimary
				)
			}
		},
		content = { padding ->
			Column(
				modifier = Modifier
					.padding(padding)
					.consumeWindowInsets(padding)
					.verticalScroll(rememberScrollState())
					.fillMaxSize()
			) {
				SettingsCompose(option = option, changeTheme = changeTheme, clearImageCache = clearImageCache)
			}
		}
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCompose(
	option: MutableState<ThemePick>,
	changeTheme: (Int) -> Unit,
	clearImageCache: () -> Unit,
)
{
	Log.d("option", "got option $option")
	var showDialog by remember { mutableStateOf(false) }
	val context = LocalContext.current
	ConstraintLayout {
		val (settings, _) = createRefs()
		Column(modifier = Modifier.constrainAs(settings) {
			top.linkTo(parent.top)
			start.linkTo(parent.start)
			end.linkTo(parent.end)
		}) {
			val listOfThemeOptions = remember(LocalConfiguration) { mutableListOf<String>() }
			if(listOfThemeOptions.isEmpty())
			{
				listOfThemeOptions.add(stringResource(R.string.light_theme))
				listOfThemeOptions.add(stringResource(R.string.dark_theme))
				listOfThemeOptions.add(stringResource(R.string.synch_with_sys))
			}
			val (selectedOption, onOptionSelected) = remember { mutableStateOf(listOfThemeOptions[option.value.intValue]) }
			val rippleConfig = remember { RippleConfiguration(color = Color.Gray, rippleAlpha = RippleAlpha(0.1f, 0f, 0.5f, 0.6f)) }
			CompositionLocalProvider(LocalRippleConfiguration provides rippleConfig) {
				Column {
					listOfThemeOptions.forEach { text ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier
								.fillMaxWidth()
								.padding(18.dp, 10.dp, 18.dp, 0.dp)
								.clickable {
									if(text != selectedOption)
									{
										Log.d("i clicked on", "selected option $selectedOption")
										onOptionSelected(text)
										saveThemeState(context, listOfThemeOptions.indexOf(text))
										changeTheme(listOfThemeOptions.indexOf(text))
									}
								}
						) {
							val painter = when(text)
							{
								listOfThemeOptions[0] ->
								{
									rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_day))
								}
								listOfThemeOptions[1] ->
								{
									rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_night))
								}
								else ->
								{
									rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_sys_theme))
								}
							}
							Icon(
								modifier = Modifier.padding(0.dp, 0.dp),
								painter = painter,
								contentDescription = "CommentIcon",
								tint = MaterialTheme.colorScheme.onPrimary
							)
							Text(
								text = text,
								fontSize = 18.sp,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
								color = MaterialTheme.colorScheme.onPrimary,
								modifier = Modifier
									.padding(16.dp, 0.dp)
									.width(250.dp)
							)
							Spacer(
								Modifier
									.weight(1f)
									.fillMaxWidth()
							)
							RadioButton(
								selected = (text == selectedOption),
								onClick = {
									if(text != selectedOption)
									{
										onOptionSelected(text)
										saveThemeState(context, listOfThemeOptions.indexOf(text))
										changeTheme(listOfThemeOptions.indexOf(text))
									}
								},
								colors = RadioButtonColors(
									Color.Green,
									MaterialTheme.colorScheme.onSecondary,
									MaterialTheme.colorScheme.error,
									MaterialTheme.colorScheme.onSecondary
								)
							)
						}
					}
				}
			}
			HorizontalDivider(modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onPrimary, thickness = 1.dp)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp, 10.dp, 0.dp, 16.dp)
					.clickable {
						showDialog = true
					}
			) {
				Icon(
					modifier = Modifier.padding(0.dp, 0.dp),
					painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_delete)),
					contentDescription = "CommentIcon",
					tint = MaterialTheme.colorScheme.onPrimary
				)
				Text(
					text = stringResource(id = R.string.clear_cache),
					fontSize = 18.sp,
					color = MaterialTheme.colorScheme.onPrimary,
					modifier = Modifier.padding(16.dp, 0.dp)
				)
				Spacer(Modifier
					.weight(1f)
					.fillMaxWidth()
				)
			}
			if(showDialog)
			{
				val textClear = stringResource(R.string.you_have_cleared_cache)
				AlertDialogMain(
					dialogText = null,
					dialogTitle = stringResource(R.string.delete_all_question),
					onConfirmation = {
						val imageLoader = context.imageLoader
						imageLoader.diskCache?.clear()
						imageLoader.memoryCache?.clear()
						clearImageCache()
						showDialog = false
						Toast.makeText(context, textClear, Toast.LENGTH_SHORT).show()
					},
					onDismissRequest = { showDialog = false },
					icon = Icons.Default.Delete,
					textButtonCancel = stringResource(R.string.cancel),
					textButtonConfirm = stringResource(R.string.confirm))
			}
		}
	}
}

private fun saveThemeState(context: Context, chosenOption: Int)
{
	val editor = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE).edit()
	Log.d("option saved", "$chosenOption")
	editor.putInt(THEME_SHARED_PREFERENCE, chosenOption)
	editor.apply()
}
