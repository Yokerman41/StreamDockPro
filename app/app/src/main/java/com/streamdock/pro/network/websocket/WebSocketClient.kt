package com.streamdock.pro.network.websocket

import android.os.Build
import android.util.Log
import com.google.gson.JsonObject
import com.streamdock.pro.core.AppConfig
import com.streamdock.pro.core.GsonProvider
import com.streamdock.pro.network.dto.ActionTriggerPayload
import com.streamdock.pro.network.dto.MessageTypes
import com.streamdock.pro.network.dto.WsEnvelope
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Cliente WebSocket de una sola conexión. Para reconectar se crea una nueva instancia
 * (ver [com.streamdock.pro.data.repository.StreamDockRepository]).
 */
class WebSocketClient(
    private val clientUuid: String,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
    private val onMessageReceived: (WsEnvelope) -> Unit,
) {
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, REGISTERING, CONNECTED, ERROR
    }

    @Volatile
    private var webSocket: WebSocket? = null
    private val gson = GsonProvider.gson

    fun connect(serverUrl: String = AppConfig.SERVER_URL) {
        onConnectionStateChanged(ConnectionState.CONNECTING)
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = sharedHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onConnectionStateChanged(ConnectionState.REGISTERING)
                    registerClient(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val envelope = gson.fromJson(text, WsEnvelope::class.java) ?: return
                        if (envelope.type == MessageTypes.REGISTER_ACK) {
                            onConnectionStateChanged(ConnectionState.CONNECTED)
                        }
                        onMessageReceived(envelope)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: $text", e)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    // El servidor inició el cierre: hay que responder para que se invoque onClosed.
                    webSocket.close(NORMAL_CLOSURE, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onConnectionStateChanged(ConnectionState.DISCONNECTED)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}")
                    onConnectionStateChanged(ConnectionState.ERROR)
                }
            },
        )
    }

    private fun registerClient(socket: WebSocket) {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val payload = JsonObject().apply {
            addProperty("display_name", "$manufacturer ${Build.MODEL}")
        }
        val registerEnvelope = WsEnvelope(
            type = MessageTypes.REGISTER_CLIENT,
            uuid = clientUuid,
            timestamp = System.currentTimeMillis(),
            payload = payload,
        )
        socket.send(gson.toJson(registerEnvelope))
    }

    fun sendAction(payload: ActionTriggerPayload): Boolean {
        val envelope = WsEnvelope(
            type = MessageTypes.ACTION,
            uuid = clientUuid,
            timestamp = System.currentTimeMillis(),
            payload = gson.toJsonTree(payload).asJsonObject,
        )
        return sendEnvelope(envelope)
    }

    fun sendEnvelope(envelope: WsEnvelope): Boolean {
        return webSocket?.send(gson.toJson(envelope)) ?: false
    }

    fun disconnect() {
        webSocket?.close(NORMAL_CLOSURE, "Client disconnect")
    }

    companion object {
        private const val TAG = "WebSocketClient"
        private const val NORMAL_CLOSURE = 1000

        /** Un único OkHttpClient compartido (pools de hilos y conexiones) para todas las conexiones. */
        private val sharedHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(15, TimeUnit.SECONDS) // Detecta conexiones muertas (WiFi caído, PC suspendido)
                .build()
        }
    }
}
