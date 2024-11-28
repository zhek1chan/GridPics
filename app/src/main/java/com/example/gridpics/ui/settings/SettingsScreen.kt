package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.example.gridpics.ui.activity.MainActivity.Companion.SHARED_PREFS_PICTURES
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SHARED_PREFERENCE
import com.example.gridpics.ui.pictures.AlertDialogMain

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
	navController: NavController,
	option: Int,
	postDefaultUrl: () -> Unit,
	changeTheme: (Int) -> Unit,
	justChangedTheme: () -> Unit,
	postCacheWasCleared: (Boolean) -> Unit
)
{
	postDefaultUrl.invoke()
	val orientation = LocalContext.current.resources.configuration.orientation
	val windowInsets = if(orientation == Configuration.ORIENTATION_LANDSCAPE)
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
				SettingsCompose(option, changeTheme, justChangedTheme, postCacheWasCleared)
			}
		}
	)
}

@Composable
fun SettingsCompose(
	option: Int,
	changeTheme: (Int) -> Unit,
	justChangedTheme: () -> Unit,
	postCacheWasCleared: (Boolean) -> Unit
)
{
	var showDialog by remember { mutableStateOf(false) }
	val context = LocalContext.current
	ConstraintLayout {
		val (settings, _) = createRefs()
		Column(modifier = Modifier.constrainAs(settings) {
			top.linkTo(parent.top)
			start.linkTo(parent.start)
			end.linkTo(parent.end)
		}) {
			val listOfThemeOptions = listOf(
				stringResource(R.string.light_theme),
				stringResource(R.string.dark_theme),
				stringResource(R.string.synch_with_sys)
			)
			val (selectedOption, onOptionSelected) = remember { mutableStateOf(listOfThemeOptions[option]) }
			Column {
				Column(Modifier.selectableGroup()) {
					listOfThemeOptions.forEach { text ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier
								.fillMaxWidth()
								.padding(18.dp, 10.dp, 18.dp, 0.dp)
								.clickable {
									onOptionSelected(text)
									saveThemeState(context, listOfThemeOptions.indexOf(text))
									changeTheme(listOfThemeOptions.indexOf(text))
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
								onClick =
								{
									justChangedTheme.invoke()
									onOptionSelected(text)
									saveThemeState(context, listOfThemeOptions.indexOf(text))
									changeTheme(listOfThemeOptions.indexOf(text))
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
					stringResource(R.string.clear_cache),
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
					dialogText = "",
					dialogTitle = stringResource(R.string.delete_all_question),
					onConfirmation = {
						postCacheWasCleared(true)
						val imageLoader = context.imageLoader
						imageLoader.diskCache?.clear()
						imageLoader.memoryCache?.clear()
						val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE)
						val editor = sharedPreferences.edit()
						editor.putString(SHARED_PREFS_PICTURES, null)
						editor.apply()
						showDialog = false
						Toast.makeText(context, textClear, Toast.LENGTH_SHORT).show()
					},
					onDismissRequest = { showDialog = false },
					icon = Icons.Default.Delete,
					textButtonCancel = stringResource(R.string.cancel),
					textButtonConfirm = stringResource(R.string.delete)
				)
			}
		}
	}
}

private fun saveThemeState(context: Context, chosenOption: Int)
{
	val editor = context.getSharedPreferences(SHARED_PREFERENCE_GRIDPICS, MODE_PRIVATE).edit()
	editor.putInt(THEME_SHARED_PREFERENCE, chosenOption)
	editor.apply()
}
