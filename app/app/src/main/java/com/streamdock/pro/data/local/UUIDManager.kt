package com.streamdock.pro.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.UUID

class UUIDManager(context: Context) {
    // Nota: el nombre incluye ".xml" por error histórico (el archivo real es "stream_dock_prefs.xml.xml").
    // No se cambia para no regenerar el UUID de los dispositivos ya registrados en el servidor.
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stream_dock_prefs.xml", Context.MODE_PRIVATE)

    fun getOrGenerateUUID(): String {
        val stored = prefs.getString(KEY_CLIENT_UUID, null)
        if (!stored.isNullOrBlank()) return stored

        val uuid = UUID.randomUUID().toString()
        prefs.edit { putString(KEY_CLIENT_UUID, uuid) }
        return uuid
    }

    companion object {
        private const val KEY_CLIENT_UUID = "key_client_uuid_v4"
    }
}
