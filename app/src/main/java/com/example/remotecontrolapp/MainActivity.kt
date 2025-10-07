package com.example.remotecontrolapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.remotecontrolapp.data.WsService
import com.example.remotecontrolapp.utils.DeviceUtils
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.remotecontrolapp.streaming.ScreenCaptureActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDeviceName: TextView
    private val wsStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("status") ?: return
            runOnUiThread {
                tvStatus.text = when (status) {
                    "connected" -> "Status: Connected"
                    "disconnected" -> "Status: Disconnected"
                    else -> "Status: Unknown"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDeviceName = findViewById(R.id.tvDeviceName)
        tvStatus = findViewById(R.id.tvStatus)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)

        // Hiển thị info thiết bị
        val deviceName = DeviceUtils.getDeviceName()
        tvDeviceName.text = "Device name: $deviceName"

        // Android 13+ cần xin quyền thông báo để ForegroundService show notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        // Start Foreground Service giữ WS chạy nền
        val svc = Intent(this, WsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }

        // Mặc định hiển thị status lúc mở
        tvStatus.text = if (WsService.isConnected) "Status: Connected" else "Status: Connecting..."

        // Nút mở cài đặt Trợ năng
//        btnAccessibility.setOnClickListener {
//            try {
//                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
//            } catch (e: Exception) {
//                Toast.makeText(this, "Không mở được cài đặt Trợ năng", Toast.LENGTH_SHORT).show()
//            }
//        }
        btnAccessibility.setOnClickListener {
            startActivity(Intent(this, ScreenCaptureActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(wsStatusReceiver, IntentFilter("WS_STATUS_CHANGED"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(wsStatusReceiver)
    }
}