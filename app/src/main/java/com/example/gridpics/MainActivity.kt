package com.example.gridpics

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.gridpics.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var changedTheme: String = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GridPics)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_dashboard) {
                binding.navView.visibility = View.GONE
            } else {
                binding.navView.visibility = View.VISIBLE
            }
        }

        binding.navView.setupWithNavController(navController)
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        changedTheme = sharedPref.getString(getString(R.string.changed_theme), changedTheme).toString()
        if ((Configuration.UI_MODE_NIGHT_NO == resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) && (changedTheme == "black")){
            changeTheme()
        } else if ((Configuration.UI_MODE_NIGHT_YES == resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) && (changedTheme == "white")){
            changeTheme()
        }
    }

    private fun changeTheme() {
        val darkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkModeOn = darkMode == Configuration.UI_MODE_NIGHT_YES
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

}