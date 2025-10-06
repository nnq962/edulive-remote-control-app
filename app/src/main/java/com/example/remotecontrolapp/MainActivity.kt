package com.example.remotecontrolapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.example.remotecontrolapp.utils.DeviceUtils
import android.content.Intent
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lấy các TextView từ layout
        val tvDeviceName = findViewById<TextView>(R.id.tvDeviceName)
        val tvAndroidId = findViewById<TextView>(R.id.tvAndroidId)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Lấy thông tin thiết bị từ DeviceUtils
        val deviceName = DeviceUtils.getDeviceName()
        val androidId = DeviceUtils.getAndroidId(this)

        // Cập nhật lên UI
        tvDeviceName.text = "Device name: $deviceName"
        tvAndroidId.text = "Android ID: $androidId"
        tvStatus.text = "Status: Connected"

        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)

        btnAccessibility.setOnClickListener {
            try {
                // Mở trang quyền Trợ năng
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Không mở được cài đặt Trợ năng 😅", Toast.LENGTH_SHORT).show()
            }
        }
    }
}