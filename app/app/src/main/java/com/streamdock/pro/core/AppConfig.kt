package com.streamdock.pro.core

object AppConfig {
    /**
     * Dirección IP del servidor Rust.
     * Use "127.0.0.1" si usa 'adb reverse tcp:8081 tcp:8081'.
     * Use la IP local de su PC (ej. "192.168.1.15") para conexión vía Wi-Fi.
     */
    const val SERVER_IP = "127.0.0.1"
    const val SERVER_PORT = 8081
    
    val SERVER_URL: String
        get() = "ws://$SERVER_IP:$SERVER_PORT/ws"

    const val RECONNECT_DELAY_MS = 3000L
}
