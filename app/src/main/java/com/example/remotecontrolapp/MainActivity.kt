package com.example.remotecontrolapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.remotecontrolapp.utils.DeviceUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // === TEST: Log thông tin thiết bị ===
        val uniqueId = DeviceUtils.getUniqueDeviceId(this)
        val androidId = DeviceUtils.getAndroidId(this)
        val deviceName = DeviceUtils.getDeviceName()
        val manufacturer = DeviceUtils.getManufacturer()
        val model = DeviceUtils.getModel()

        Log.i("DeviceInfo", "Unique ID: $uniqueId")
        Log.i("DeviceInfo", "Android ID: $androidId")
        Log.i("DeviceInfo", "Device Name: $deviceName")
        Log.i("DeviceInfo", "Manufacturer: $manufacturer")
        Log.i("DeviceInfo", "Model: $model")
    }
}