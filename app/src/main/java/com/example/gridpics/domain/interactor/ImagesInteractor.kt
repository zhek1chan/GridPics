package com.example.gridpics.domain.interactor

import android.graphics.Bitmap
import com.example.gridpics.data.network.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

interface ImagesInteractor
{
	suspend fun getPics(): Flow<Resource<String>>
	suspend fun getPictureBitmap(url: String, job: Job): Bitmap?
}