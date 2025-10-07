package com.example.remotecontrolapp.streaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.remotecontrolapp.R
import com.example.remotecontrolapp.data.WsClient
import com.example.remotecontrolapp.data.WsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.os.Build

/**
 * Foreground Service giữ MediaProjection và encoder gửi dữ liệu qua WS.
 *
 * - Được start bởi ScreenCaptureActivity khi user cho phép ghi màn hình.
 * - Nhận resultCode + dataIntent từ activity.
 * - Tạo MediaProjection -> VirtualDisplay -> VideoEncoder -> gửi WS.
 * - Giữ foreground notification để Android không kill.
 */
class ScreenShareService : Service() {

    private val channelId = "screen_share_channel"
    private val notificationId = 2001

    private var projection: MediaProjection? = null
    private var encoder: VideoEncoder? = null
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(notificationId, buildNotification("Đang chia sẻ màn hình..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("dataIntent", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("dataIntent")
        }

        if (resultCode == 0 || dataIntent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val mpm = getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, dataIntent)

        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Gắn MediaProjection vào ProjectionHolder (để chỗ khác reuse)
        ProjectionHolder.set(projection)

        // Tạo pipeline gửi qua WS
        val wsClient = WsService.getWsClient()
        if (wsClient == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val pipeline = StreamPipeline(wsClient)

        // Dựng encoder + VirtualDisplay trong coroutine riêng
        job = CoroutineScope(Dispatchers.Default).launch {
            val cfg = StreamingConfig
            encoder = VideoEncoder(
                cfg.DEFAULT_WIDTH,
                cfg.DEFAULT_HEIGHT,
                cfg.DEFAULT_FPS,
                cfg.DEFAULT_BITRATE,
                onInit = { bytes -> pipeline.sendInit(bytes) },
                onChunk = { bytes -> pipeline.sendMedia(bytes) }
            )

            // Bắt đầu encode màn hình
            encoder?.startWith(projection!!)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        encoder?.stop()
        projection?.stop()
        ProjectionHolder.clear()
        updateNotification("Dừng chia sẻ màn hình")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ============================================================
    // 🔔 Notification setup
    // ============================================================
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Screen Share",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Thông báo khi chia sẻ màn hình đang chạy"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Remote Control - Streaming")
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }
}