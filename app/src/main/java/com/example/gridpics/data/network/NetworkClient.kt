package com.example.gridpics.data.network

interface NetworkClient
{
	suspend fun getPics(): Resource<String>
}