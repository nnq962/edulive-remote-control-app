//package com.example.remotecontrolapp.streaming
//
//import android.media.MediaCodec
//import android.media.MediaCodecInfo
//import android.media.MediaFormat
//import android.view.Surface
//import java.nio.ByteBuffer
//
//class VideoEncoder(
//    width: Int,
//    height: Int,
//    fps: Int = 30,
//    bitrate: Int = (width * height * 5.0).toInt() // ~5 bpp
//) {
//    data class Encoded(
//        val data: ByteArray,
//        val isConfig: Boolean,
//        val isKey: Boolean,
//        val ptsUs: Long
//    )
//
//    private val codec: MediaCodec =
//        MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
//
//    val inputSurface: Surface
//
//    init {
//        val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
//            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
//            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
//            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
//            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2) // keyframe mỗi 2 giây
//            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
//            setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel31)
//        }
//        codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//        inputSurface = codec.createInputSurface()
//        codec.start()
//    }
//
//    fun drain(onFrame: (Encoded) -> Unit) {
//        val info = MediaCodec.BufferInfo()
//        while (true) {
//            val idx = codec.dequeueOutputBuffer(info, 0)
//            when {
//                idx >= 0 -> {
//                    val out: ByteBuffer = codec.getOutputBuffer(idx)!!
//                    val arr = ByteArray(info.size)
//                    out.position(info.offset)
//                    out.limit(info.offset + info.size)
//                    out.get(arr)
//
//                    val flags = info.flags
//                    onFrame(
//                        Encoded(
//                            data = arr,
//                            isConfig = (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0,
//                            isKey = (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0,
//                            ptsUs = info.presentationTimeUs
//                        )
//                    )
//                    codec.releaseOutputBuffer(idx, false)
//                }
//                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
//                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
//            }
//        }
//    }
//
//    fun release() {
//        try { codec.stop() } catch (_: Exception) {}
//        try { codec.release() } catch (_: Exception) {}
//    }
//}