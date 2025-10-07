package com.example.remotecontrolapp.streaming

import android.media.projection.MediaProjection

/**
 * Stub tối thiểu cho compile & test flow.
 * Sau khi luồng ổn, ta thay bằng encoder MediaCodec thật.
 */
class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrate: Int,
    private val onInit: (ByteArray) -> Unit,
    private val onChunk: (ByteArray) -> Unit
) {
    fun startWith(projection: MediaProjection) {
        // TODO: implement real MediaCodec + VirtualDisplay.
        // Tạm thời emit init giả để test pipeline.
        onInit("fmp4-init-mock".toByteArray())
        // Có thể emit vài chunk mock nếu muốn:
        // onChunk("fmp4-media-chunk".toByteArray())
    }

    fun stop() {
        // TODO: release codec & virtual display khi có encoder thật
    }
}