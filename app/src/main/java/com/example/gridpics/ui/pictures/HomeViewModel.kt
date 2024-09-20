package com.example.gridpics.ui.pictures

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridpics.R
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.interactor.NewsInteractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel(
    private val interactor: NewsInteractor
) : ViewModel() {

    private val stateLiveData = MutableLiveData<PictureState>()
    fun observeState(): LiveData<PictureState> = stateLiveData
    fun getPics() {
        viewModelScope.launch {
            interactor.getPics().collect { news ->
                when (news) {
                    is Resource.Data -> stateLiveData.postValue(PictureState.SearchIsOk(news.value))
                    is Resource.ConnectionError -> stateLiveData.postValue(PictureState.ConnectionError)
                    is Resource.NotFound -> stateLiveData.postValue(PictureState.NothingFound)
                }
            }
        }
    }
}