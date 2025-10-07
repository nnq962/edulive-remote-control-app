package com.example.remotecontrolapp.streaming

import android.media.projection.MediaProjection

object ProjectionHolder {
    var projection: MediaProjection? = null
        private set

    fun set(p: MediaProjection?) {
        projection?.stop()
        projection = p
    }

    fun clear() {
        projection?.stop()
        projection = null
    }
}