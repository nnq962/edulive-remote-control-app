package com.example.remotecontrolapp.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.remotecontrolapp.MainActivity
import com.example.remotecontrolapp.utils.DeviceUtils
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class WsService : Service() {

    private lateinit var wsClient: WsClient
    private val wsUrl = "ws://192.168.0.100:8080/ws" // đổi IP LAN backend
    private val channelId = "remote_ws_channel"
    private val notificationId = 1001
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        @Volatile var isConnected: Boolean = false
        @Volatile private var wsClientRef: WsClient? = null

        fun getWsClient(): WsClient? = wsClientRef
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification("Đang kết nối server..."))
        startWebSocket()
    }

    private fun notifyStatus(status: String) {
        val intent = Intent("WS_STATUS_CHANGED")
        intent.putExtra("status", status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun startWebSocket() {
        wsClient = WsClient(
            applicationContext,
            wsUrl,
            onOpen = {
                isConnected = true
                updateNotification("Đã kết nối server")
                notifyStatus("connected")
                mainHandler.post {
                    Toast.makeText(this, "WebSocket connected", Toast.LENGTH_SHORT).show()
                }

                // 🔹 Lấy thông tin thiết bị
                val androidId = DeviceUtils.getAndroidId(this)
                val deviceName = DeviceUtils.getDeviceName()

                // 🔹 Gửi gói đăng ký pub.register
                val registerJson = """
                    {
                      "type": "pub.register",
                      "deviceId": "$androidId",
                      "deviceName": "$deviceName",
                      "codec": "avc",
                      "width": 1280,
                      "height": 720,
                      "fps": 30
                    }
                """.trimIndent()

                wsClient.sendText(registerJson)
            },
            onClosed = { _, reason ->
                isConnected = false
                updateNotification("Mất kết nối, đang thử lại...")
                notifyStatus("disconnected")
                mainHandler.post {
                    Toast.makeText(this, "WebSocket closed: $reason", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { t ->
                isConnected = false
                updateNotification("Lỗi: ${t.message}")
                notifyStatus("disconnected")
                mainHandler.post {
                    Toast.makeText(this, "WebSocket failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
        wsClientRef = wsClient
        wsClient.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::wsClient.isInitialized) startWebSocket()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wsClient.stop()
        isConnected = false
        Toast.makeText(this, "WsService destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ============================================================
    // 🔔 Notification setup
    // ============================================================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Remote Control Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Trạng thái kết nối WebSocket"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Remote Control App")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}