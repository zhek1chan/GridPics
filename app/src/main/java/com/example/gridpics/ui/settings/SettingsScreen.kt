package com.example.gridpics.ui.settings

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import coil3.imageLoader
import com.example.gridpics.R
import com.example.gridpics.ui.activity.BottomNavigationBar
import com.example.gridpics.ui.activity.MainActivity.Companion.CACHE
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.details.DetailsViewModel
import com.example.gridpics.ui.pictures.AlertDialogMain
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel, navController: NavController, detailsViewModel: DetailsViewModel, option: Int)
{
	detailsViewModel.postUrl("default")
	val orientation = LocalContext.current.resources.configuration.orientation
	if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
		Box(modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
		) {
			Scaffold(modifier = Modifier
				.fillMaxWidth()
				.displayCutoutPadding(),
				bottomBar = { BottomNavigationBar(navController) },
				content = { padding ->
					Column(
						modifier = Modifier
							.padding(padding)
							.consumeWindowInsets(padding)
							.verticalScroll(rememberScrollState())
							.fillMaxSize()
					) {
						SettingsCompose(vm, option)
					}
				}
			)
		}
	} else {
		Scaffold(modifier = Modifier
			.fillMaxWidth(),
			bottomBar = { BottomNavigationBar(navController) },
			content = { padding ->
				Column(
					modifier = Modifier
						.padding(padding)
						.consumeWindowInsets(padding)
						.verticalScroll(rememberScrollState())
						.fillMaxSize()
				) {
					SettingsCompose(vm, option)
				}
			}
		)
	}
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SettingsCompose(vm: SettingsViewModel, option: Int)
{
	ComposeTheme {
		val scope = rememberCoroutineScope()
		var setOption = option
		scope.launch(Dispatchers.Main) {
			vm.observeFlow().collectLatest {
				setOption = it
			}
		}
		var showDialog by remember { mutableStateOf(false) }
		val context = LocalContext.current
		ConstraintLayout {
			val (settings, _) = createRefs()
			Column(modifier = Modifier.constrainAs(settings) {
				top.linkTo(parent.top)
				start.linkTo(parent.start)
				end.linkTo(parent.end)
			}) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp, 10.dp)
				) {
					Text(
						stringResource(R.string.settings),
						fontSize = 21.sp,
						color = MaterialTheme.colorScheme.onPrimary
					)
				}
				val listOfThemeOptions = listOf(
					context.getString(R.string.light_theme),
					context.getString(R.string.dark_theme),
					context.getString(R.string.synch_with_sys)
				)
				val (selectedOption, onOptionSelected) = remember { mutableStateOf(listOfThemeOptions[setOption]) }
				Column {
					Column(Modifier.selectableGroup()) {
						listOfThemeOptions.forEach { text ->
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier
									.fillMaxWidth()
									.padding(18.dp, 10.dp, 6.dp, 0.dp)
									.clickable {
										scope.launch(Dispatchers.Main) {
											onOptionSelected(text)
											vm.changeFromSettings(context)
											vm.changeTheme(context, listOfThemeOptions.indexOf(text))
										}
									}
							) {
								val painter = when(text)
								{
									listOfThemeOptions[0] ->
									{
										painterResource(R.drawable.ic_day)
									}
									listOfThemeOptions[1] ->
									{
										painterResource(R.drawable.ic_night)
									}
									else ->
									{
										painterResource(R.drawable.ic_sys_theme)
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
									color = MaterialTheme.colorScheme.onPrimary,
									modifier = Modifier.padding(16.dp, 0.dp)
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
										onOptionSelected(text)
										vm.changeFromSettings(context)
										vm.changeTheme(context, listOfThemeOptions.indexOf(text))
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
						painter = painterResource(R.drawable.ic_delete),
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
					AlertDialogMain(
						dialogText = "",
						dialogTitle = stringResource(R.string.delete_all_question),
						onConfirmation = {
							val imageLoader = context.imageLoader
							imageLoader.diskCache?.clear()
							imageLoader.memoryCache?.clear()
							val sharedPreferences = context.getSharedPreferences(PICTURES, MODE_PRIVATE)
							val editor = sharedPreferences.edit()
							editor.putString(PICTURES, null)
							editor.apply()
							val sharedPreferencesCache = context.getSharedPreferences(CACHE, MODE_PRIVATE)
							val editorCache = sharedPreferencesCache.edit()
							editorCache.putBoolean(CACHE, true)
							editorCache.apply()
							showDialog = false
							Toast.makeText(context, context.getString(R.string.you_have_cleared_cache), Toast.LENGTH_SHORT).show()
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
}