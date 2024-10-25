package com.example.gridpics.ui.settings

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import coil3.imageLoader
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.CACHE
import com.example.gridpics.ui.activity.MainActivity.Companion.PICTURES
import com.example.gridpics.ui.pictures.AlertDialogMain
import com.example.gridpics.ui.themes.ComposeTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel)
{
	SettingsCompose(vm)
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun SettingsCompose(vm: SettingsViewModel)
{
	ComposeTheme {
		var checkedStateForTheme by remember { mutableStateOf(false) }
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
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp, 10.dp)
						.clickable {
							checkedStateForTheme = true
							vm.changeTheme(context)
						}
				) {
					Icon(
						modifier = Modifier.padding(0.dp, 0.dp),
						painter = painterResource(R.drawable.ic_theme),
						contentDescription = "CommentIcon",
						tint = MaterialTheme.colorScheme.onPrimary
					)
					Text(
						stringResource(R.string.dark_theme),
						fontSize = 18.sp,
						color = MaterialTheme.colorScheme.onPrimary,
						modifier = Modifier.padding(16.dp, 0.dp)
					)
					Spacer(
						Modifier
							.weight(1f)
							.fillMaxWidth()
					)
					val scope = rememberCoroutineScope()
					scope.launch {
						vm.observeTheme().collectLatest {
							checkedStateForTheme = it
						}
					}
					GradientSwitch(
						checked = checkedStateForTheme,
						onCheckedChange = {
							checkedStateForTheme = it
							vm.changeTheme(context)
						})
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp, 10.dp)
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