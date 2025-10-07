package com.example.remotecontrolapp.streaming

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity này chỉ có nhiệm vụ:
 *  - Gọi MediaProjectionManager để xin quyền ghi màn hình
 *  - Khi user bấm "Allow", nó gửi token (resultCode + data) sang ScreenShareService
 *  - Sau đó tự đóng ngay (finish())
 *
 * Được start bằng: startActivity(Intent(context, ScreenCaptureActivity::class.java).apply {
 *      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 * })
 */
class ScreenCaptureActivity : AppCompatActivity() {

    // Launcher xin quyền MediaProjection
    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val svc = Intent(this, ScreenShareService::class.java).apply {
                putExtra("resultCode", res.resultCode)
                putExtra("dataIntent", res.data)
            }
            // Gọi Foreground Service stream màn hình
            startForegroundService(svc)
        }
        // Dù thành công hay không đều tự đóng
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lấy MediaProjectionManager và mở intent xin quyền
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        val captureIntent = projectionManager.createScreenCaptureIntent()
        projectionLauncher.launch(captureIntent)
    }
}