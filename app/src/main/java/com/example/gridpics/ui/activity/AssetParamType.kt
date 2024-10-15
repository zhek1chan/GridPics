package com.example.gridpics.ui.activity

import android.os.Bundle
import androidx.navigation.NavType

class AssetParamType: NavType<String>(isNullableAllowed = false)
{
	override fun get(bundle: Bundle, key: String): String?
	{
		return bundle.getString(key)
	}

	override fun parseValue(value: String): String
	{
		return "ne znau chto tut dolznho bit'"
	}

	override fun put(bundle: Bundle, key: String, value: String)
	{
		bundle.putString(key, value)
	}
}