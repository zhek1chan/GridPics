package com.example.gridpics.domain.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.gridpics.data.network.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

interface ImagesRepository
{
	suspend fun getPics(): Flow<Resource<String>>
	suspend fun getPictureBitmap(url: String, context: Context, job: Job): Bitmap?
}