package com.example.remotecontrolapp.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class WsClient(
    private val appContext: Context,
    private val url: String,
    private val onOpen: () -> Unit = {},
    private val onClosed: (code: Int, reason: String) -> Unit = { _, _ -> },
    private val onFailure: (Throwable) -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)      // stream lâu dài
        .pingInterval(15, TimeUnit.SECONDS)         // giữ kết nối sống
        .retryOnConnectionFailure(true)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val reconnectIntervalMs = 10_000L
    private var reconnectScheduled = false
    private var shouldReconnect = true
    private var connecting = false

    @Volatile private var ws: WebSocket? = null

    fun connect() {
        if (connecting) return
        connecting = true
        cancelPendingReconnect()

        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                connecting = false
                toast("WS connected")
                onOpen()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connecting = false
                onClosed(code, reason)
                if (shouldReconnect) {
                    toast("WS closed ($code) – retry in 10s")
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connecting = false
                onFailure(t)
                if (shouldReconnect) {
                    toast("WS failed: ${t.message ?: "unknown"} – retry in 10s")
                    scheduleReconnect()
                }
            }
        })
    }

    fun sendText(text: String): Boolean = ws?.send(text) == true

    fun sendBinary(bytes: ByteArray): Boolean {
        val bs = bytes.toByteString()
        return ws?.send(bs) == true
    }

    fun isConnected(): Boolean = ws != null

    fun stop() {
        shouldReconnect = false
        cancelPendingReconnect()
        ws?.close(1000, "bye")
        ws = null
    }

    private fun scheduleReconnect() {
        if (reconnectScheduled) return
        reconnectScheduled = true
        mainHandler.postDelayed({
            reconnectScheduled = false
            if (shouldReconnect) connect()
        }, reconnectIntervalMs)
    }

    private fun cancelPendingReconnect() {
        reconnectScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show() }
    }
}