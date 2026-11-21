package com.streamdock.pro.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    const val BASE64_IMAGE_PREFIX = "data:image/"

    private val bitmapCache = LruCache<Int, ImageBitmap>(64)

    private val iconMap: Map<String, ImageVector?> = mapOf(
        "none" to null,
        "default" to Icons.Default.RadioButtonChecked,
        "home" to Icons.Default.Home,
        // Multimedia
        "play" to Icons.Default.PlayArrow,
        "pause" to Icons.Default.Pause,
        "stop" to Icons.Default.Stop,
        "next" to Icons.Default.SkipNext,
        "prev" to Icons.Default.SkipPrevious,
        "vol_up" to Icons.AutoMirrored.Filled.VolumeUp,
        "vol_down" to Icons.AutoMirrored.Filled.VolumeDown,
        "mute" to Icons.AutoMirrored.Filled.VolumeOff,
        "mic" to Icons.Default.Mic,
        "mic_off" to Icons.Default.MicOff,
        "record" to Icons.Default.FiberManualRecord,

        // Sistema y Teclado
        "computer" to Icons.Default.Computer,
        "keyboard" to Icons.Default.Keyboard,
        "settings" to Icons.Default.Settings,
        "power" to Icons.Default.PowerSettingsNew,
        "lock" to Icons.Default.Lock,
        "apps" to Icons.Default.Apps,
        "folder" to Icons.Default.Folder,
        "launch" to Icons.AutoMirrored.Filled.Launch,

        // Web
        "web" to Icons.Default.Language,
        "public" to Icons.Default.Public,

        // Otros
        "game" to Icons.Default.VideogameAsset,
        "tv" to Icons.Default.Tv,
        "stream" to Icons.Default.Stream,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "info" to Icons.Default.Info,
        "warning" to Icons.Default.Warning,
        "error" to Icons.Default.Error,
    )

    fun isBase64Icon(iconName: String?): Boolean {
        if (iconName.isNullOrBlank()) return false
        return iconName.startsWith(BASE64_IMAGE_PREFIX, ignoreCase = true)
    }

    /**
     * Decodifica un icono PNG/WebP en Base64 enviado por el servidor o el editor de PC
     * (p.ej. iconos extraídos de ejecutables `.exe`) y lo almacena en caché LRU.
     */
    fun getBitmap(iconName: String?): ImageBitmap? {
        if (!isBase64Icon(iconName)) return null
        val raw = iconName!!
        val cacheKey = raw.hashCode()
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val base64Part = raw.substringAfter("base64,", missingDelimiterValue = raw)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            val trimmed = trimTransparentBorder(decoded)
            val bmp = trimmed.asImageBitmap()
            bitmapCache.put(cacheKey, bmp)
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Si el icono PNG enviado desde Windows trae márgenes transparentes alrededor
     * (típico de iconos Win32 dentro de un lienzo mayor), recorta el área útil para que
     * el icono se vea grande y aproveche todo el espacio del botón en la app.
     */
    private fun trimTransparentBorder(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 16 || height <= 16) return source

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var top = height
        var left = width
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
                if (alpha > 12) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        if (right < left || bottom < top) return source
        val croppedW = right - left + 1
        val croppedH = bottom - top + 1
        if (croppedW >= width * 0.92f && croppedH >= height * 0.92f) return source

        return Bitmap.createBitmap(source, left, top, croppedW, croppedH)
    }

    fun getIcon(name: String?): ImageVector? {
        if (name.isNullOrBlank() || isBase64Icon(name)) return null
        return iconMap[name.trim().lowercase()]
    }

    /**
     * Resuelve el icono vectorial de un botón: usa `iconName` si está definido y, cuando
     * el servidor o el editor envía `"default"` / `"none"` con una acción configurada,
     * infiere automáticamente el icono adecuado según `actionType` y `actionValue`.
     */
    fun getIconForButton(
        iconName: String?,
        actionType: String? = null,
        actionValue: String? = null,
    ): ImageVector? {
        if (isBase64Icon(iconName)) return null
        val normalized = iconName?.trim()?.lowercase()
        if (!normalized.isNullOrEmpty() && normalized != "none" && normalized != "default") {
            iconMap[normalized]?.let { return it }
        }
        val inferredName = inferIconNameForAction(actionType, actionValue)
        if (inferredName != null) {
            return iconMap[inferredName]
        }
        return if (normalized == "default") iconMap["default"] else null
    }

    fun inferIconNameForAction(actionType: String?, actionValue: String?): String? {
        val type = actionType?.trim()?.uppercase() ?: return null
        val value = actionValue?.trim()?.uppercase().orEmpty()
        return when (type) {
            "MEDIA_CONTROL" -> when (value) {
                "PLAY_PAUSE", "SPOTIFY_PLAY_PAUSE", "SPOTIFY_OPEN" -> "play"
                "NEXT_TRACK", "SPOTIFY_NEXT" -> "next"
                "PREV_TRACK", "SPOTIFY_PREV" -> "prev"
                "VOLUME_UP", "SPOTIFY_VOL_UP" -> "vol_up"
                "VOLUME_DOWN", "SPOTIFY_VOL_DOWN" -> "vol_down"
                "VOLUME_MUTE", "SPOTIFY_MUTE" -> "mute"
                "SPOTIFY_STOP" -> "stop"
                else -> "play"
            }
            "SYSTEM_COMMAND" -> when (value) {
                "LOCK_WORKSTATION" -> "lock"
                "SHOW_DESKTOP" -> "computer"
                "SLEEP", "SHUTDOWN", "REBOOT" -> "power"
                else -> "settings"
            }
            "KEYBOARD_HOTKEY" -> when (value) {
                "CTRL+SHIFT+M" -> "mic_off"
                "WIN+SHIFT+S" -> "computer"
                else -> if (value.isNotEmpty()) "keyboard" else null
            }
            "OBS_SCENE_SWITCH" -> when (value) {
                "START_RECORD" -> "record"
                "START_STREAM" -> "stream"
                "TOGGLE_MUTE" -> "mic_off"
                else -> "tv"
            }
            "LAUNCH_APP", "LAUNCH_SHORTCUT" -> when {
                value.startsWith("WIN_MINIMIZE") -> "computer"
                value.startsWith("WIN_MAXIMIZE") -> "apps"
                value.startsWith("WIN_CLOSE") -> "error"
                value.isNotEmpty() -> "launch"
                else -> null
            }
            "STREAMER_BOT_ACTION" -> "stream"
            "NAVIGATE" -> if (value.isNotEmpty()) "folder" else null
            else -> null
        }
    }

    fun getAllIconNames(): List<String> {
        return iconMap.keys.filter { it != "none" && it != "default" }
    }
}
