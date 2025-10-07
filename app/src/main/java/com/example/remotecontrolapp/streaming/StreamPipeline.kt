package com.example.remotecontrolapp.streaming

import com.example.remotecontrolapp.data.WsClient

class StreamPipeline(private val ws: WsClient) {

    fun sendInit(bytes: ByteArray) {
        val out = ByteArray(1 + bytes.size)
        out[0] = StreamingConfig.KIND_INIT
        System.arraycopy(bytes, 0, out, 1, bytes.size)
        ws.sendBinary(out)
    }

    fun sendMedia(bytes: ByteArray) {
        val out = ByteArray(1 + bytes.size)
        out[0] = StreamingConfig.KIND_MEDIA
        System.arraycopy(bytes, 0, out, 1, bytes.size)
        ws.sendBinary(out)
    }
}