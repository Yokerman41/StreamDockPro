package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun OBSScreen(
    connectionState: WebSocketClient.ConnectionState,
    scenes: List<String>,
    onFetchScenes: () -> Unit,
    onSwitchToScene: (String) -> Unit,
) {
    var customSceneInput by remember { mutableStateOf("") }
    val customScenes = remember {
        mutableStateListOf("Escena Principal", "Gameplay", "Charlando / Just Chatting", "BRB / Pausa")
    }

    LaunchedEffect(connectionState) {
        if (connectionState == WebSocketClient.ConnectionState.CONNECTED) {
            onFetchScenes()
        }
    }

    val isConnected = connectionState == WebSocketClient.ConnectionState.CONNECTED

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14)),
    ) {
        val isWide = this.maxWidth >= 560.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Status & Sync Bar
            Surface(
                color = Color(0xFF151822),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isConnected) Color(0xFF1E3A2F) else Color(0xFF2D313E)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF9800)),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CENTRO DE CONTROL OBS STUDIO",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = if (isConnected) "Sincronizado con el servidor de tu PC" else "Conecta tu PC para controlar OBS en vivo",
                                color = Color(0xFF9499B0),
                                fontSize = 11.sp,
                            )
                        }
                    }

                    Button(
                        onClick = onFetchScenes,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar OBS",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sincronizar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (isWide) {
                // Adaptive 2-Column Layout for Landscape & Tablets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ObsMasterControlsCard(onSwitchToScene = onSwitchToScene)
                    }
                    Box(modifier = Modifier.weight(1.2f)) {
                        ObsScenesDeckCard(
                            scenes = scenes,
                            customScenes = customScenes,
                            customSceneInput = customSceneInput,
                            onCustomSceneInputChange = { customSceneInput = it },
                            onAddCustomScene = {
                                val trimmed = customSceneInput.trim()
                                if (trimmed.isNotBlank() && !customScenes.contains(trimmed)) {
                                    customScenes.add(0, trimmed)
                                    customSceneInput = ""
                                }
                            },
                            onSwitchToScene = onSwitchToScene,
                        )
                    }
                }
            } else {
                // Portrait Single Column Layout
                ObsMasterControlsCard(onSwitchToScene = onSwitchToScene)
                ObsScenesDeckCard(
                    scenes = scenes,
                    customScenes = customScenes,
                    customSceneInput = customSceneInput,
                    onCustomSceneInputChange = { customSceneInput = it },
                    onAddCustomScene = {
                        val trimmed = customSceneInput.trim()
                        if (trimmed.isNotBlank() && !customScenes.contains(trimmed)) {
                            customScenes.add(0, trimmed)
                            customSceneInput = ""
                        }
                    },
                    onSwitchToScene = onSwitchToScene,
                )
            }
        }
    }
}

@Composable
private fun ObsMasterControlsCard(
    onSwitchToScene: (String) -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TRANSMISIÓN Y GRABACIÓN (1 TOQUE)",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ObsDeckActionTile(
                    title = "Stream ON/OFF",
                    subtitle = "Transmitir",
                    icon = Icons.Default.Sensors,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { onSwitchToScene("START_STREAM") },
                )
                ObsDeckActionTile(
                    title = "Grabar ON/OFF",
                    subtitle = "Grabación",
                    icon = Icons.Default.FiberManualRecord,
                    accentColor = Color(0xFFF97316),
                    modifier = Modifier.weight(1f),
                    onClick = { onSwitchToScene("START_RECORD") },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ObsDeckActionTile(
                    title = "Pausar Rec",
                    subtitle = "Pausa/Reanudar",
                    icon = Icons.Default.Pause,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onSwitchToScene("PAUSE_RECORD") },
                )
                ObsDeckActionTile(
                    title = "Replay Buffer",
                    subtitle = "Guardar Clip",
                    icon = Icons.Default.Replay,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onSwitchToScene("SAVE_REPLAY") },
                )
                ObsDeckActionTile(
                    title = "Mute Audio",
                    subtitle = "Mic / Desktop",
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    accentColor = Color(0xFFA855F7),
                    modifier = Modifier.weight(1f),
                    onClick = { onSwitchToScene("TOGGLE_MUTE") },
                )
            }
        }
    }
}

@Composable
private fun ObsScenesDeckCard(
    scenes: List<String>,
    customScenes: List<String>,
    customSceneInput: String,
    onCustomSceneInputChange: (String) -> Unit,
    onAddCustomScene: () -> Unit,
    onSwitchToScene: (String) -> Unit,
) {
    val allSceneItems: List<Pair<String, String>> = remember(scenes, customScenes.toList()) {
        val list = mutableListOf<Pair<String, String>>()
        val ignoreCommands = setOf("START_RECORD", "START_STREAM", "TOGGLE_MUTE", "PAUSE_RECORD", "SAVE_REPLAY")
        scenes.filter { it !in ignoreCommands }.forEach { cmd ->
            val label = when {
                cmd.startsWith("SOURCE_TOGGLE:") -> {
                    val parts = cmd.removePrefix("SOURCE_TOGGLE:").split("|", limit = 2)
                    if (parts.size == 2) "👁️ ${parts[1]}" else cmd
                }
                cmd.startsWith("MUTE_INPUT:") -> "🎙️ ${cmd.removePrefix("MUTE_INPUT:")}"
                else -> "🎬 $cmd"
            }
            list.add(label to cmd)
        }
        customScenes.forEach { sceneName ->
            if (list.none { it.second.equals(sceneName, ignoreCase = true) }) {
                list.add("🎬 $sceneName" to sceneName)
            }
        }
        list
    }

    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CAMBIADOR RÁPIDO DE ESCENAS Y FUENTES",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Add Scene Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyledInput(
                    value = customSceneInput,
                    onValueChange = onCustomSceneInputChange,
                    label = "Añadir Escena de OBS",
                    placeholder = "Ej: Escena Gameplay",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onAddCustomScene,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir Escena",
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Column Chunked Grid (scroll-friendly inside parent verticalScroll)
            val rows = allSceneItems.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { (title, command) ->
                            Surface(
                                color = Color(0xFF1E2231),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .clickable { onSwitchToScene(command) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObsDeckActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = Color(0xFF1D202D),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = Color(0xFF9499B0),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
