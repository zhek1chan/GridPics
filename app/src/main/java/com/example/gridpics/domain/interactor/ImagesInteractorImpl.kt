package com.example.gridpics.domain.interactor

import android.content.Context
import android.graphics.Bitmap
import com.example.gridpics.data.network.Resource
import com.example.gridpics.domain.repository.ImagesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

class ImagesInteractorImpl(
	private val repository: ImagesRepository,
	private val context: Context,
): ImagesInteractor
{
	override suspend fun getPics(): Flow<Resource<String>> = repository.getPics()
	override suspend fun getPictureBitmap(url: String, job: Job): Bitmap? = repository.getPictureBitmap(url, context, job)
}