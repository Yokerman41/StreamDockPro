package com.streamdock.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile

private data class ActionPreset(
    val value: String,
    val defaultLabel: String,
    val defaultIcon: String,
    val defaultColor: String,
)

/**
 * Preajustes alineados exactamente con el servidor Rust (`win32_input.rs`, `action_engine.rs`)
 * y con el editor de escritorio WinUI (`MainViewModel.cs`).
 */
private val presetsByActionType: Map<String, List<ActionPreset>> = mapOf(
    "MEDIA_CONTROL" to listOf(
        ActionPreset("PLAY_PAUSE", "Play / Pausa", "play", "#9B59B6"),
        ActionPreset("NEXT_TRACK", "Siguiente", "next", "#9B59B6"),
        ActionPreset("PREV_TRACK", "Anterior", "prev", "#9B59B6"),
        ActionPreset("VOLUME_UP", "Subir Vol.", "vol_up", "#2ECC71"),
        ActionPreset("VOLUME_DOWN", "Bajar Vol.", "vol_down", "#E67E22"),
        ActionPreset("VOLUME_MUTE", "Silenciar", "mute", "#E74C3C"),
        ActionPreset("SPOTIFY_PLAY_PAUSE", "Spotify Play", "play", "#1DB954"),
        ActionPreset("SPOTIFY_NEXT", "Spotify Sig.", "next", "#1DB954"),
        ActionPreset("SPOTIFY_PREV", "Spotify Ant.", "prev", "#1DB954"),
        ActionPreset("SPOTIFY_VOL_UP", "Spotify Vol+", "vol_up", "#1DB954"),
        ActionPreset("SPOTIFY_VOL_DOWN", "Spotify Vol-", "vol_down", "#1DB954"),
        ActionPreset("SPOTIFY_MUTE", "Spotify Mute", "mute", "#E74C3C"),
        ActionPreset("SPOTIFY_OPEN", "Abrir Spotify", "play", "#1DB954"),
    ),
    "SYSTEM_COMMAND" to listOf(
        ActionPreset("LOCK_WORKSTATION", "Bloquear PC", "lock", "#E74C3C"),
        ActionPreset("SLEEP", "Suspender PC", "power", "#E74C3C"),
        ActionPreset("SHUTDOWN", "Apagar PC", "power", "#E74C3C"),
        ActionPreset("REBOOT", "Reiniciar PC", "power", "#E74C3C"),
        ActionPreset("SHOW_DESKTOP", "Escritorio", "computer", "#E74C3C"),
    ),
    "KEYBOARD_HOTKEY" to listOf(
        ActionPreset("CTRL+SHIFT+M", "Mute Mic", "mic_off", "#34495E"),
        ActionPreset("WIN+SHIFT+S", "Captura", "computer", "#34495E"),
        ActionPreset("ALT+F4", "Cerrar", "error", "#34495E"),
    ),
    "OBS_SCENE_SWITCH" to listOf(
        ActionPreset("Escena Principal", "OBS", "tv", "#3498DB"),
        ActionPreset("START_RECORD", "Grabar", "record", "#F38BA8"),
        ActionPreset("START_STREAM", "Stream", "stream", "#A6E3A1"),
        ActionPreset("TOGGLE_MUTE", "Mute Mic", "mic_off", "#FAB387"),
    ),
    "LAUNCH_APP" to listOf(
        ActionPreset("WIN_MINIMIZE", "Minimizar", "computer", "#3498DB"),
        ActionPreset("WIN_MAXIMIZE", "Maximizar", "apps", "#2ECC71"),
        ActionPreset("WIN_CLOSE", "Cerrar Ventana", "error", "#E74C3C"),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
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

    val colors = listOf("#1A1B26", "#2196F3", "#F44336", "#4CAF50", "#FFEB3B", "#9C27B0", "#FF9800", "#795548")
    val actionTypes = listOf(
        "NONE",
        "KEYBOARD_HOTKEY",
        "MEDIA_CONTROL",
        "LAUNCH_APP",
        "LAUNCH_SHORTCUT",
        "OBS_SCENE_SWITCH",
        "STREAMER_BOT_ACTION",
        "SYSTEM_COMMAND",
        "NAVIGATE",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F0F14),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .fillMaxHeight(),
            ) {
                // Encabezado fijo con botón rápido de papelera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CONFIGURAR BOTÓN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar botón",
                                tint = Color(0xFFFF5252),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido desplazable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SectionHeader(text = "APARIENCIA")

                    StyledInput(
                        value = label,
                        onValueChange = { label = it },
                        label = "Etiqueta",
                        placeholder = "Nombre del botón",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Color de Fondo", color = Color(0xFF9499B0), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        colors.forEach { hex ->
                            val color = Color(hex.toColorInt())
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Icono", color = Color(0xFF9499B0), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.height(120.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
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

                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(text = "ACCIÓN")

                    var expanded by remember { mutableStateOf(value = false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = actionTypeDisplayName(selectedType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Acción") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedBorderColor = Color(0xFF2D313E),
                            ),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            actionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(actionTypeDisplayName(type)) },
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                        val firstPreset = presetsByActionType[type]?.firstOrNull()
                                        if (firstPreset != null) {
                                            actionValue = firstPreset.value
                                            if (label.isBlank()) label = firstPreset.defaultLabel
                                            if (selectedIcon == "none") selectedIcon = firstPreset.defaultIcon
                                        }
                                    },
                                )
                            }
                        }
                    }

                    // Preajustes rápidos compatibles con el servidor Rust
                    val presets = presetsByActionType[selectedType]
                    if (!presets.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Preajustes del Servidor", color = Color(0xFF9499B0), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            presets.forEach { preset ->
                                val isSelected = actionValue == preset.value
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        actionValue = preset.value
                                        label = preset.defaultLabel
                                        selectedIcon = preset.defaultIcon
                                        selectedColor = preset.defaultColor
                                    },
                                    label = { Text(preset.defaultLabel, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF4F46E5),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1A1B26),
                                        labelColor = Color(0xFF9499B0),
                                    ),
                                )
                            }
                        }
                    }

                    // Selector de páginas cuando la acción es NAVIGATE
                    if (selectedType == "NAVIGATE" && availableProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Página de destino", color = Color(0xFF9499B0), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            availableProfiles.forEach { profile ->
                                val isSelected = actionValue == profile.id || actionValue.equals(profile.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        actionValue = profile.id
                                        if (label.isBlank()) label = profile.name
                                        if (selectedIcon == "none") selectedIcon = "folder"
                                    },
                                    label = { Text(profile.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF4F46E5),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF1A1B26),
                                        labelColor = Color(0xFF9499B0),
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedType != "NONE") {
                        StyledInput(
                            value = actionValue,
                            onValueChange = { actionValue = it },
                            label = when (selectedType) {
                                "KEYBOARD_HOTKEY" -> "Combinación (Ej: CTRL+SHIFT+M)"
                                "MEDIA_CONTROL" -> "Comando (PLAY_PAUSE, VOLUME_UP, VOLUME_MUTE...)"
                                "LAUNCH_APP", "LAUNCH_SHORTCUT" -> "Ruta o ejecutable (Ej: notepad.exe)"
                                "OBS_SCENE_SWITCH" -> "Nombre de la Escena / Comando OBS"
                                "STREAMER_BOT_ACTION" -> "Acción de Streamer.bot"
                                "SYSTEM_COMMAND" -> "Comando (LOCK_WORKSTATION, SLEEP, SHUTDOWN...)"
                                "NAVIGATE" -> "Nombre o ID de Página"
                                else -> "Valor"
                            },
                            placeholder = "Escribe el valor aquí...",
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Barra inferior fija: SIEMPRE visible sin necesidad de hacer scroll
                HorizontalDivider(color = Color(0xFF2D313E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onDelete != null) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5252),
                                contentColor = Color.White,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ELIMINAR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    Button(
                        onClick = { onSave(label, selectedIcon, selectedColor, selectedType, actionValue) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "GUARDAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF9499B0)),
                ) {
                    Text("CANCELAR")
                }
            }
        }
    }
}

private fun actionTypeDisplayName(type: String): String = when (type) {
    "OBS_SCENE_SWITCH" -> "OBS"
    else -> type
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
        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1A1B26),
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
