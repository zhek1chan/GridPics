package com.example.gridpics.ui.settings

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.gridpics.R
import com.example.gridpics.ui.activity.MainActivity.Companion.BLACK
import com.example.gridpics.ui.activity.MainActivity.Companion.THEME_SP_KEY
import com.example.gridpics.ui.activity.MainActivity.Companion.WHITE
import com.example.gridpics.ui.themes.ComposeTheme

@Composable
fun SettingsScreen()
{
	val changedTheme = LocalContext.current.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE).getString(THEME_SP_KEY, WHITE)
	Log.d("Saved theme is now", "Theme now is $changedTheme")
	SettingsCompose(changedTheme!!)
}

private fun changeTheme(context: Context)
{
	val darkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
	val isDarkModeOn = darkMode == Configuration.UI_MODE_NIGHT_YES
	val sharedPreferences = context.getSharedPreferences(THEME_SP_KEY, MODE_PRIVATE)
	val editor = sharedPreferences.edit()
	val whiteOrBlack: String
	if(isDarkModeOn)
	{
		whiteOrBlack = WHITE
		editor.putString(THEME_SP_KEY, whiteOrBlack)
		editor.apply()
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
	}
	else
	{
		whiteOrBlack = BLACK
		editor.putString(THEME_SP_KEY, whiteOrBlack)
		editor.apply()
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
	}
	Log.d("Saved theme", "saved $whiteOrBlack theme")
}

@Composable
fun SettingsCompose(changedTheme: String)
{
	ComposeTheme {
		var checkedState by remember { mutableStateOf(false) }
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
				) {
					Icon(
						modifier = Modifier.padding(0.dp, 0.dp),
						painter = painterResource(R.drawable.ic_theme),
						contentDescription = "CommentIcon",
						tint = Color.Unspecified
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
					val context = LocalContext.current
					if(changedTheme.contains(BLACK))
					{
						GradientSwitch(
							checked = true,
							onCheckedChange = {
								checkedState = it
								changeTheme(context)
							})
					}
					else
					{
						GradientSwitch(
							checked = checkedState,
							onCheckedChange = {
								checkedState = it
								changeTheme(context)
							})
					}
				}
			}
		}
	}
}