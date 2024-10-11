package com.example.gridpics.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val stateLiveData = MutableLiveData<Boolean>(true)
    fun observeState(): LiveData<Boolean> = stateLiveData

    fun setTrueState() {
        viewModelScope.launch(Dispatchers.IO) {
            stateLiveData.postValue(true)
        }
    }

    fun setFalseState() {
        viewModelScope.launch(Dispatchers.IO) {
            stateLiveData.postValue(false)
        }
    }
}