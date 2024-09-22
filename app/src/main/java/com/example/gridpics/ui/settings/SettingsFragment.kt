package com.example.gridpics.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gridpics.databinding.FragmentNotificationsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private lateinit var binding: FragmentNotificationsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingsViewModel.getOnBackLiveData()
        binding.themeSwitcher.isChecked = !(settingsViewModel.getThemeLiveData().value!!)
        binding.themeSwitcher.setOnClickListener {
            settingsViewModel.themeSwitch()
            binding.themeSwitcher.isChecked = !(settingsViewModel.getThemeLiveData().value!!)
        }
    }

    companion object {
        const val SEARCH_SHARED_PREFS_KEY = "SEARCH_SHARED_PREFS_KEY"
    }
}