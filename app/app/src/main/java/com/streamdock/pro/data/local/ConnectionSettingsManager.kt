package com.streamdock.pro.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.streamdock.pro.core.AppConfig

class ConnectionSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("connection_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MODE = "connection_mode"
        private const val KEY_LAST_IP = "last_known_ip"
        private const val KEY_LAST_PORT = "last_known_port"

        const val MODE_USB = "USB"
        const val MODE_WIFI = "WIFI"
    }

    fun setConnectionMode(mode: String) {
        prefs.edit { putString(KEY_MODE, mode) }
    }

    fun getConnectionMode(): String {
        return prefs.getString(KEY_MODE, MODE_USB) ?: MODE_USB
    }

    fun setLastKnownServer(ip: String, port: Int) {
        prefs.edit {
            putString(KEY_LAST_IP, ip)
            putInt(KEY_LAST_PORT, port)
        }
    }

    fun getLastKnownIp(): String? {
        return prefs.getString(KEY_LAST_IP, null)
    }

    fun getLastKnownPort(): Int {
        return prefs.getInt(KEY_LAST_PORT, AppConfig.SERVER_PORT)
    }
}
