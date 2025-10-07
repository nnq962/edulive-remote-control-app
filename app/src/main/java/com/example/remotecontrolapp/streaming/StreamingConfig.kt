package com.example.remotecontrolapp.streaming

object StreamingConfig {
    const val KIND_INIT: Byte = 1
    const val KIND_MEDIA: Byte = 2

    const val DEFAULT_WIDTH = 1280
    const val DEFAULT_HEIGHT = 720
    const val DEFAULT_FPS = 30
    const val DEFAULT_BITRATE = 4_000_000 // 4 Mbps
}