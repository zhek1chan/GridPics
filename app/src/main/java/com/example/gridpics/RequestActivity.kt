package com.example.gridpics

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gridpics.databinding.ActivityEmptyBinding


class RequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmptyBinding.inflate(layoutInflater)
        binding.request.setOnClickListener {
            navToSettings()
        }
        navToSettings()
        setContentView(binding.root)
    }

    private fun navToSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val text = "Для корректной работы требуется разрешение на работу с файлами"
                val duration = Toast.LENGTH_LONG

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
                val getpermission = Intent()
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(getpermission)
            } else {
                val intent = Intent(
                    this,
                    MainActivity::class.java
                )
                ContextCompat.startActivity(this, intent, null)
                Log.d("PERMISSION", "GRANTED")
            }
        }
    }

}