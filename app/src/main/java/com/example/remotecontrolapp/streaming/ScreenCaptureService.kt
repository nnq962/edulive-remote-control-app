//package com.example.remotecontrolapp.streaming
//
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.hardware.display.DisplayManager
//import android.hardware.display.VirtualDisplay
//import android.media.projection.MediaProjection
//import android.media.projection.MediaProjectionManager
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//import com.example.remotecontrolapp.utils.DeviceUtils
//
//
//class ScreenCaptureService : Service() {
//
//    companion object {
//        private const val TAG = "ScreenCaptureService"
//        private const val NOTI_CHANNEL_ID = "screen_stream"
//        private const val NOTI_ID = 777
//
//        // Extras khi startService
//        const val EXTRA_RESULT_CODE = "result_code"
//        const val EXTRA_RESULT_DATA = "result_data" // Intent từ MediaProjection permission
//        const val EXTRA_WS_URL = "ws_url"           // ws://host:port/path
//        const val EXTRA_FPS = "fps"                 // optional, default 30
//        const val EXTRA_BITRATE = "bitrate"         // optional, bps; default auto theo res
//    }
//
//    private var mediaProjection: MediaProjection? = null
//    private var virtualDisplay: VirtualDisplay? = null
//    private var encoder: VideoEncoder? = null
//    private var ws: WsClient? = null
//
//    @Volatile private var running = false
//    private var drainThread: Thread? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        startForegroundWithNotification()
//
//        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
//        val resultData = if (Build.VERSION.SDK_INT >= 33) {
//            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
//        } else {
//            @Suppress("DEPRECATION")
//            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
//        }
//        val wsUrl = intent?.getStringExtra(EXTRA_WS_URL)
//        val fps = intent?.getIntExtra(EXTRA_FPS, 30) ?: 30
//        val explicitBitrate = intent?.getIntExtra(EXTRA_BITRATE, -1) ?: -1
//
//        if (resultCode != Activity.RESULT_OK || resultData == null || wsUrl.isNullOrBlank()) {
//            Log.e(TAG, "Thiếu dữ liệu start stream (permission/URL). Dừng service.")
//            stopSelf()
//            return START_NOT_STICKY
//        }
//
//        startStream(resultCode, resultData, wsUrl, fps, explicitBitrate)
//        return START_NOT_STICKY
//    }
//
//    private fun startForegroundWithNotification() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val channel = NotificationChannel(
//                NOTI_CHANNEL_ID, "Screen Streaming",
//                NotificationManager.IMPORTANCE_LOW
//            )
//            mgr.createNotificationChannel(channel)
//        }
//        val noti = Notification.Builder(this, NOTI_CHANNEL_ID)
//            .setContentTitle("Đang chia sẻ màn hình")
//            .setContentText("Đang truyền hình ảnh qua WebSocket")
//            .setSmallIcon(android.R.drawable.presence_video_online)
//            .build()
//        startForeground(NOTI_ID, noti)
//    }
//
//    private fun startStream(
//        resultCode: Int,
//        resultData: Intent,
//        wsUrl: String,
//        fps: Int,
//        bitrate: Int
//    ) {
//        if (running) return
//        running = true
//
//        // 1) Lấy MediaProjection
//        val mpMgr = getSystemService(MediaProjectionManager::class.java)
//        mediaProjection = mpMgr.getMediaProjection(resultCode, resultData)
//
//        // 2) Lấy độ phân giải tối đa (vật lý) của màn hình
//        val (width, height) = DeviceUtils.getScreenResolution(this)
//        val density = resources.displayMetrics.densityDpi
//
//        // 3) Tạo encoder H.264 (surface input)
//        val targetBitrate = if (bitrate > 0) bitrate else ((width * height) * 5.0).toInt() // ~5 bpp
//        val enc = VideoEncoder(width, height, fps = fps, bitrate = targetBitrate)
//        encoder = enc
//
//        // 4) Kết nối WebSocket
//        ws = WsClient(
//            url = wsUrl,
//            onOpen = { Log.i(TAG, "WS open") },
//            onClosed = { code, reason -> Log.i(TAG, "WS closed $code/$reason"); stopSelf() },
//            onFailure = { t -> Log.e(TAG, "WS failure", t); stopSelf() }
//        ).also { it.connect() }
//
//        // 5) Tạo VirtualDisplay, render vào inputSurface của encoder
//        virtualDisplay = mediaProjection?.createVirtualDisplay(
//            "RC-VD",
//            width, height, density,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            enc.inputSurface, null, null
//        )
//
//        // 6) Drain loop: đọc NAL từ encoder và gửi qua WS
//        drainThread = Thread {
//            try {
//                while (running) {
//                    enc.drain { frame ->
//                        // frame.data: NAL Annex-B (SPS/PPS/IDR/P/B) → gửi binary
//                        ws?.sendBinary(frame.data)
//                    }
//                    // ngủ rất ngắn để nhường CPU; encoder có event-driven qua dequeueOutputBuffer
//                    Thread.sleep(3)
//                }
//            } catch (t: Throwable) {
//                Log.e(TAG, "Drain loop error", t)
//            } finally {
//                stopSelf()
//            }
//        }.apply { start() }
//
//        Log.i(TAG, "Streaming start: ${width}x${height} @$fps fps, bitrate=$targetBitrate, url=$wsUrl")
//    }
//
//    private fun stopStream() {
//        running = false
//
//        try { drainThread?.join(300) } catch (_: Exception) {}
//        drainThread = null
//
//        try { virtualDisplay?.release() } catch (_: Exception) {}
//        virtualDisplay = null
//
//        try { mediaProjection?.stop() } catch (_: Exception) {}
//        mediaProjection = null
//
//        try { encoder?.release() } catch (_: Exception) {}
//        encoder = null
//
//        try { ws?.close() } catch (_: Exception) {}
//        ws = null
//
//        Log.i(TAG, "Streaming stopped")
//    }
//
//    override fun onDestroy() {
//        stopStream()
//        super.onDestroy()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}