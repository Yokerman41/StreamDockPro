package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile

private data class ActionCategoryTab(
    val id: String,
    val title: String,
    val protocolActionType: String,
    val accentColor: Color,
)

private data class ActionPreset(
    val protocolActionType: String,
    val value: String,
    val title: String,
    val subtitle: String,
    val defaultIcon: String,
    val defaultColor: String,
)

private val categoryTabs = listOf(
    ActionCategoryTab("AUDIO_PC", "🎵 Audio PC", "MEDIA_CONTROL", Color(0xFFCBA6F7)),
    ActionCategoryTab("SPOTIFY", "🟢 Spotify", "MEDIA_CONTROL", Color(0xFF1DB954)),
    ActionCategoryTab("VOICEMEETER", "🎛️ Voicemeeter", "VOICEMEETER", Color(0xFFFFB86C)),
    ActionCategoryTab("LAUNCH_APP", "🚀 Abrir App", "LAUNCH_APP", Color(0xFF89B4FA)),
    ActionCategoryTab("WINDOWS", "🪟 Ventanas", "LAUNCH_APP", Color(0xFF38BDF8)),
    ActionCategoryTab("OBS_SCENE_SWITCH", "🎥 OBS Studio", "OBS_SCENE_SWITCH", Color(0xFF89DCEB)),
    ActionCategoryTab("KEYBOARD_HOTKEY", "⌨️ Atajo Teclado", "KEYBOARD_HOTKEY", Color(0xFFF9E2AF)),
    ActionCategoryTab("NAVIGATE", "📄 Ir a Página", "NAVIGATE", Color(0xFF818CF8)),
    ActionCategoryTab("SYSTEM_COMMAND", "⚙️ Sistema PC", "SYSTEM_COMMAND", Color(0xFFF38BA8)),
)

private val presetsByCategory: Map<String, List<ActionPreset>> = mapOf(
    "AUDIO_PC" to listOf(
        ActionPreset("MEDIA_CONTROL", "VOLUME_SLIDER_PC", "🎚️ Barra Vol. PC", "Deslizar arriba/abajo en perfil", "vol_up", "#7C3AED"),
        ActionPreset("MEDIA_CONTROL", "PLAY_PAUSE", "Play / Pausa", "Audio global PC", "play", "#9B59B6"),
        ActionPreset("MEDIA_CONTROL", "VOLUME_MUTE", "Silenciar PC", "Mutea todo Windows", "mute", "#E74C3C"),
        ActionPreset("MEDIA_CONTROL", "VOLUME_UP", "Subir Vol.", "Volumen maestro +", "vol_up", "#2ECC71"),
        ActionPreset("MEDIA_CONTROL", "VOLUME_DOWN", "Bajar Vol.", "Volumen maestro -", "vol_down", "#E67E22"),
        ActionPreset("MEDIA_CONTROL", "PREV_TRACK", "Anterior", "Canción previa", "prev", "#9B59B6"),
        ActionPreset("MEDIA_CONTROL", "NEXT_TRACK", "Siguiente", "Siguiente pista", "next", "#9B59B6"),
    ),
    "SPOTIFY" to listOf(
        ActionPreset("MEDIA_CONTROL", "VOLUME_SLIDER_SPOTIFY", "🎚️ Barra Spotify", "Deslizar arriba/abajo en perfil", "vol_up", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_PLAY_PAUSE", "Spotify Play", "Solo Spotify 2do plano", "play", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_OPEN", "Abrir Spotify", "Mostrar ventana", "play", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_PREV", "Spotify Ant.", "Canción previa", "prev", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_NEXT", "Spotify Sig.", "Siguiente pista", "next", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_VOL_UP", "Spotify Vol+", "No sube juegos/PC", "vol_up", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_VOL_DOWN", "Spotify Vol-", "No baja juegos/PC", "vol_down", "#1DB954"),
        ActionPreset("MEDIA_CONTROL", "SPOTIFY_MUTE", "Spotify Mute", "Silencia solo Spotify", "mute", "#E74C3C"),
    ),
    "VOICEMEETER" to listOf(
        // Salidas Master (Outputs A1..B2)
        ActionPreset("VOICEMEETER", "VOICEMEETER_OUTPUT_A1", "Salida Master A1", "Altavoces Realtek (ON/MUTE)", "vol_up", "#2ECC71"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_OUTPUT_A2", "Salida Master A2", "GPM27Q NVIDIA (ON/MUTE)", "vol_up", "#3498DB"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_OUTPUT_A3", "Salida Master A3", "Altavoces F998 (ON/MUTE)", "vol_up", "#9B59B6"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_OUTPUT_B1", "Salida Master B1", "Canal Virtual OBS/Stream", "stream", "#E67E22"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_OUTPUT_B2", "Salida Master B2", "Canal Virtual AUX", "stream", "#E67E22"),

        // Entradas (Inputs) - Micrófono (Hardware 1)
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].Mute", "Activar/Mute Mic", "Micrófono Realtek", "mic_off", "#E74C3C"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].A1", "Mic -> Salida A1", "Altavoces Realtek", "mic", "#9B59B6"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].A2", "Mic -> Salida A2", "GPM27Q NVIDIA", "mic", "#3498DB"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].A3", "Mic -> Salida A3", "Altavoces F998", "mic", "#CBA6F7"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].B1", "Mic -> Salida B1", "OBS Studio / Discord", "stream", "#2ECC71"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].B2", "Mic -> Salida B2", "Virtual AUX Out", "stream", "#A6E3A1"),
        ActionPreset("VOICEMEETER", "VOLUME_SLIDER_VM_STRIP0", "🎚️ Fader Micrófono", "Ganancia Mic en perfil", "vol_up", "#FFB86C"),

        // Entradas (Inputs) - PC / Juegos (Voicemeeter VAIO - Strip 3)
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].Mute", "Activar/Mute VAIO", "Audio PC / Juegos", "mute", "#E74C3C"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].A1", "VAIO -> Salida A1", "Altavoces Realtek", "vol_up", "#9B59B6"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].A2", "VAIO -> Salida A2", "Auriculares NVIDIA", "vol_up", "#3498DB"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].A3", "VAIO -> Salida A3", "Altavoces F998", "vol_up", "#CBA6F7"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].B1", "VAIO -> Salida B1", "Audio PC a OBS", "stream", "#2ECC71"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].B2", "VAIO -> Salida B2", "Audio PC a AUX B2", "stream", "#A6E3A1"),
        ActionPreset("VOICEMEETER", "VOLUME_SLIDER_VM_STRIP3", "🎚️ Fader VAIO PC", "Volumen Voicemeeter Input", "vol_up", "#FFB86C"),

        // Entradas (Inputs) - Música / Chat (Voicemeeter AUX - Strip 4)
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].Mute", "Activar/Mute AUX", "Discord / Spotify", "mute", "#E74C3C"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].A1", "AUX -> Salida A1", "Altavoces Realtek", "vol_up", "#9B59B6"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].A2", "AUX -> Salida A2", "Auriculares NVIDIA", "vol_up", "#3498DB"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].A3", "AUX -> Salida A3", "Altavoces F998", "vol_up", "#CBA6F7"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].B1", "AUX -> Salida B1", "AUX a OBS Studio", "stream", "#2ECC71"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].B2", "AUX -> Salida B2", "AUX a B2", "stream", "#A6E3A1"),
        ActionPreset("VOICEMEETER", "VOLUME_SLIDER_VM_STRIP4", "🎚️ Fader AUX", "Volumen Voicemeeter AUX", "vol_up", "#FFB86C"),

        // Utilidades
        ActionPreset("VOICEMEETER", "VOICEMEETER_RESTART", "Reiniciar Audio", "Reiniciar motor Voicemeeter", "power", "#E67E22"),
        ActionPreset("VOICEMEETER", "VOICEMEETER_SHOW", "Abrir Interfaz", "Mostrar ventana Banana", "apps", "#3498DB"),
    ),
    "WINDOWS" to listOf(
        ActionPreset("LAUNCH_APP", "WIN_MINIMIZE", "Minimizar", "Oculta ventana activa", "computer", "#3498DB"),
        ActionPreset("LAUNCH_APP", "WIN_MAXIMIZE", "Maximizar", "Pantalla completa", "apps", "#2ECC71"),
        ActionPreset("LAUNCH_APP", "WIN_CLOSE", "Cerrar Ventana", "Cierra ventana activa", "error", "#E74C3C"),
        ActionPreset("SYSTEM_COMMAND", "SHOW_DESKTOP", "Escritorio", "Minimiza todo (Win+D)", "computer", "#3498DB"),
    ),
    "OBS_SCENE_SWITCH" to listOf(
        ActionPreset("OBS_SCENE_SWITCH", "START_STREAM", "Stream OBS", "Iniciar / Detener directo", "stream", "#2ECC71"),
        ActionPreset("OBS_SCENE_SWITCH", "START_RECORD", "Grabar OBS", "Iniciar / Detener grabación", "record", "#E74C3C"),
        ActionPreset("OBS_SCENE_SWITCH", "TOGGLE_MUTE", "Mute Mic OBS", "Silenciar micro en OBS", "mic_off", "#E67E22"),
        ActionPreset("OBS_SCENE_SWITCH", "Escena Principal", "Escena OBS", "Cambiar a escena", "tv", "#3498DB"),
    ),
    "KEYBOARD_HOTKEY" to listOf(
        ActionPreset("KEYBOARD_HOTKEY", "CTRL+SHIFT+M", "Mute Discord", "Ctrl + Shift + M", "mic_off", "#34495E"),
        ActionPreset("KEYBOARD_HOTKEY", "WIN+SHIFT+S", "Captura", "Win + Shift + S", "computer", "#34495E"),
        ActionPreset("KEYBOARD_HOTKEY", "CTRL+C", "Copiar", "Ctrl + C", "keyboard", "#34495E"),
        ActionPreset("KEYBOARD_HOTKEY", "CTRL+V", "Pegar", "Ctrl + V", "keyboard", "#34495E"),
        ActionPreset("KEYBOARD_HOTKEY", "ALT+TAB", "Cambiar App", "Alt + Tab", "apps", "#34495E"),
        ActionPreset("KEYBOARD_HOTKEY", "ALT+F4", "Cerrar App", "Alt + F4", "error", "#E74C3C"),
    ),
    "SYSTEM_COMMAND" to listOf(
        ActionPreset("SYSTEM_COMMAND", "LOCK_WORKSTATION", "Bloquear PC", "Pantalla de bloqueo", "lock", "#E74C3C"),
        ActionPreset("SYSTEM_COMMAND", "SLEEP", "Suspender PC", "Modo reposo", "power", "#9B59B6"),
        ActionPreset("SYSTEM_COMMAND", "REBOOT", "Reiniciar PC", "Reinicio del sistema", "power", "#E67E22"),
        ActionPreset("SYSTEM_COMMAND", "SHUTDOWN", "Apagar PC", "Apagado seguro", "power", "#E74C3C"),
    ),
    "LAUNCH_APP" to listOf(
        ActionPreset("LAUNCH_APP", "notepad.exe", "Bloc de Notas", "Abrir Notepad", "launch", "#2A2A3C"),
        ActionPreset("LAUNCH_APP", "calc.exe", "Calculadora", "Abrir Calculadora", "launch", "#2A2A3C"),
        ActionPreset("LAUNCH_APP", "explorer.exe", "Explorador", "Abrir Archivos", "folder", "#2A2A3C"),
        ActionPreset("LAUNCH_APP", "cmd.exe", "Terminal", "Símbolo del sistema", "terminal", "#2A2A3C"),
    ),
)

private fun inferInitialCategory(actionType: String, actionValue: String): String = when {
    actionType == "VOICEMEETER" ||
        actionValue.startsWith("VOICEMEETER_", ignoreCase = true) ||
        actionValue.startsWith("VM_", ignoreCase = true) ||
        actionValue.startsWith("VOLUME_SLIDER_VM_", ignoreCase = true) -> "VOICEMEETER"
    actionType == "MEDIA_CONTROL" && (
        actionValue.startsWith("SPOTIFY_", ignoreCase = true) ||
            actionValue.startsWith("VOLUME_SLIDER_SPOTIFY", ignoreCase = true)
        ) -> "SPOTIFY"
    actionType == "MEDIA_CONTROL" -> "AUDIO_PC"
    actionType == "LAUNCH_APP" && (
        actionValue.startsWith("WIN_MINIMIZE", ignoreCase = true) ||
            actionValue.startsWith("WIN_MAXIMIZE", ignoreCase = true) ||
            actionValue.startsWith("WIN_CLOSE", ignoreCase = true)
        ) -> "WINDOWS"
    actionType == "SYSTEM_COMMAND" && actionValue.equals("SHOW_DESKTOP", ignoreCase = true) -> "WINDOWS"
    actionType == "LAUNCH_APP" || actionType == "LAUNCH_SHORTCUT" -> "LAUNCH_APP"
    actionType == "OBS_SCENE_SWITCH" -> "OBS_SCENE_SWITCH"
    actionType == "KEYBOARD_HOTKEY" -> "KEYBOARD_HOTKEY"
    actionType == "NAVIGATE" -> "NAVIGATE"
    actionType == "SYSTEM_COMMAND" -> "SYSTEM_COMMAND"
    else -> "AUDIO_PC"
}

@Composable
fun ButtonConfigDialog(
    button: ButtonModel,
    availableProfiles: List<Profile> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (label: String, icon: String, color: String, type: String, value: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var label by remember(button) { mutableStateOf(button.label) }
    var selectedIcon by remember(button) { mutableStateOf(button.iconName) }
    var selectedColor by remember(button) { mutableStateOf(button.backgroundColor ?: "#1A1B26") }
    var selectedType by remember(button) { mutableStateOf(button.actionType) }
    var actionValue by remember(button) { mutableStateOf(button.actionValue) }
    var selectedCategory by remember(button) {
        mutableStateOf(inferInitialCategory(button.actionType, button.actionValue))
    }

    val colors = listOf("#1A1B26", "#2196F3", "#1DB954", "#9B59B6", "#E74C3C", "#E67E22", "#4F46E5", "#34495E")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F0F14),
            border = BorderStroke(1.dp, Color(0xFF262B3D)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
        ) {
            @Suppress("UnusedBoxWithConstraintsScope")
            BoxWithConstraints {
                val isWideDialog = this.maxWidth >= 520.dp
                val gridColumnsCount = if (isWideDialog) 4 else 2

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxHeight(),
                ) {
                    // Encabezado fijo con botón cerrar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CONFIGURAR TECLA #${button.position + 1}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF9499B0),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Contenido desplazable adaptable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SectionHeader(text = "1. CATEGORÍA DE ACCIÓN")

                        // Cuadrícula adaptable: 4 columnas en Horizontal/Tablet, 2 en Vertical
                        val chunkedCategories = remember(gridColumnsCount) {
                            categoryTabs.chunked(gridColumnsCount)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunkedCategories.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    rowItems.forEach { cat ->
                                        val isSelected = selectedCategory == cat.id
                                        Surface(
                                            color = if (isSelected) cat.accentColor.copy(alpha = 0.18f) else Color(0xFF161922),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) cat.accentColor else Color(0xFF282D3E),
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedCategory = cat.id
                                                    if (selectedType == "NONE" || selectedType.isBlank()) {
                                                        selectedType = cat.protocolActionType
                                                    }
                                                },
                                        ) {
                                            Text(
                                                text = cat.title,
                                                color = if (isSelected) Color.White else Color(0xFFCDD6F4),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Catálogo de Tarjetas de la Categoría Seleccionada (Adaptable 2 o 3 columnas)
                        val presetCols = if (isWideDialog) 3 else 2
                        val categoryPresets = presetsByCategory[selectedCategory]
                        if (!categoryPresets.isNullOrEmpty()) {
                            Text(
                                text = "Selecciona una Función:",
                                color = Color(0xFF9499B0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val chunkedPresets = remember(selectedCategory, presetCols) {
                                categoryPresets.chunked(presetCols)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunkedPresets.forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        rowPresets.forEach { preset ->
                                            val isActionSelected = actionValue.equals(preset.value, ignoreCase = true)
                                            Surface(
                                                color = if (isActionSelected) Color(0xFF202A44) else Color(0xFF151822),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(
                                                    width = if (isActionSelected) 1.5.dp else 1.dp,
                                                    color = if (isActionSelected) Color(0xFF00E676) else Color(0xFF282D3E),
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        selectedType = preset.protocolActionType
                                                        actionValue = preset.value
                                                        label = preset.title
                                                        selectedIcon = preset.defaultIcon
                                                        selectedColor = preset.defaultColor
                                                    },
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                    Text(
                                                        text = preset.title,
                                                        color = if (isActionSelected) Color(0xFF00E676) else Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = preset.subtitle,
                                                        color = Color(0xFF9499B0),
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                        repeat(presetCols - rowPresets.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Selector de páginas cuando la categoría es NAVIGATE
                        if (selectedCategory == "NAVIGATE" && availableProfiles.isNotEmpty()) {
                            Text(
                                text = "Selecciona la Página de destino:",
                                color = Color(0xFF9499B0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val chunkedProfiles = remember(availableProfiles, presetCols) {
                                availableProfiles.chunked(presetCols)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunkedProfiles.forEach { rowProfiles ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        rowProfiles.forEach { profile ->
                                            val isSelected = actionValue == profile.id || actionValue.equals(profile.name, ignoreCase = true)
                                            Surface(
                                                color = if (isSelected) Color(0xFF28255A) else Color(0xFF151822),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF818CF8) else Color(0xFF282D3E),
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        selectedType = "NAVIGATE"
                                                        actionValue = profile.id
                                                        label = profile.name
                                                        selectedIcon = "folder"
                                                        selectedColor = "#4F46E5"
                                                    },
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "📄 ${profile.name}",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = "Ir a esta página",
                                                        color = Color(0xFF9499B0),
                                                        fontSize = 10.sp,
                                                    )
                                                }
                                            }
                                        }
                                        repeat(presetCols - rowProfiles.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        StyledInput(
                            value = actionValue,
                            onValueChange = {
                                actionValue = it
                                if (selectedType == "NONE" || selectedType.isBlank()) {
                                    selectedType = categoryTabs.find { c -> c.id == selectedCategory }?.protocolActionType ?: "LAUNCH_APP"
                                }
                            },
                            label = when (selectedCategory) {
                                "KEYBOARD_HOTKEY" -> "Combinación (Ej: CTRL+SHIFT+M)"
                                "LAUNCH_APP" -> "Ruta o ejecutable (Ej: notepad.exe)"
                                "OBS_SCENE_SWITCH" -> "Escena o comando OBS"
                                "NAVIGATE" -> "Nombre o ID de Página"
                                else -> "Comando asignado"
                            },
                            placeholder = "Escribe o elige una tarjeta arriba...",
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        SectionHeader(text = "2. APARIENCIA DE LA TECLA")

                        StyledInput(
                            value = label,
                            onValueChange = { label = it },
                            label = "Etiqueta Visible",
                            placeholder = "Nombre del botón",
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Color del Botón", color = Color(0xFF9499B0), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            colors.forEach { hex ->
                                val color = Color(hex.toColorInt())
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = 2.dp,
                                            color = if (selectedColor.equals(hex, ignoreCase = true)) Color.White else Color.Transparent,
                                            shape = CircleShape,
                                        )
                                        .clickable { selectedColor = hex },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Icono", color = Color(0xFF9499B0), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.height(100.dp)) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(if (isWideDialog) 8 else 5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    IconItem(
                                        name = "none",
                                        isSelected = selectedIcon == "none",
                                        onClick = { selectedIcon = "none" },
                                    )
                                }
                                items(IconMapper.getAllIconNames()) { name ->
                                    IconItem(
                                        name = name,
                                        isSelected = selectedIcon == name,
                                        onClick = { selectedIcon = name },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Barra inferior fija compacta en una sola fila (ideal para Vertical y Horizontal)
                    HorizontalDivider(color = Color(0xFF262B3D), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2231),
                                contentColor = Color(0xFFCDD6F4),
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "CANCELAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }

                        if (onDelete != null) {
                            Button(
                                onClick = onDelete,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(42.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BORRAR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        Button(
                            onClick = { onSave(label, selectedIcon, selectedColor, selectedType, actionValue) },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "GUARDAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1A1B26),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (name == "none") {
                Text("X", color = Color.White, fontSize = 12.sp)
            } else {
                IconMapper.getIcon(name)?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = name,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
