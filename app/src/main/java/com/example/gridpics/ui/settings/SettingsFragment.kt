package com.example.gridpics.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import com.example.gridpics.R
import com.example.gridpics.ui.themes.ComposeTheme

class SettingsFragment : Fragment() {

    private var changedTheme: String = "null"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        changedTheme =
            sharedPref.getString(getString(R.string.changed_theme), changedTheme).toString()

        return ComposeView(requireContext()).apply {
            setContent {
                SettingsCompose()
            }
        }
    }

    private fun changeTheme() {
        val darkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkModeOn = darkMode == Configuration.UI_MODE_NIGHT_YES
        if (isDarkModeOn) {
            changedTheme = "white"
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            changedTheme = "black"
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    @Preview
    @Composable
    fun SettingsCompose() {
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
                        if (changedTheme == "black") {
                            GradientSwitch(
                                checked = true,
                                onCheckedChange = {
                                    checkedState = it
                                    changeTheme()
                                })
                        } else {
                            GradientSwitch(
                                checked = checkedState,
                                onCheckedChange = {
                                    checkedState = it
                                    changeTheme()
                                })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(getString(R.string.changed_theme), changedTheme)
            apply()
        }
    }

    companion object {
        const val SEARCH_SHARED_PREFS_KEY = "SEARCH_SHARED_PREFS_KEY"
    }

}

@Composable
fun SettingsScreen(
)
{
    AndroidFragment<SettingsFragment>()
}