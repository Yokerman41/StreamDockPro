package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.network.websocket.WebSocketClient

private val defaultObsQuickActions = listOf(
    "Escena Principal" to "Escena Principal",
    "Iniciar Grabación" to "START_RECORD",
    "Iniciar Stream" to "START_STREAM",
    "Mute Micrófono" to "TOGGLE_MUTE",
)

@Composable
fun OBSScreen(
    connectionState: WebSocketClient.ConnectionState,
    scenes: List<String>,
    onFetchScenes: () -> Unit,
    onSwitchToScene: (String) -> Unit,
) {
    var port by remember { mutableStateOf("4455") }
    var password by remember { mutableStateOf("") }
    var showScenes by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Main Config Card
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D313E)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SectionHeader(text = "CONFIGURACIÓN DEL WEBSOCKET DE OBS")

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StyledInput(
                        value = port,
                        onValueChange = { if (it.all(Char::isDigit)) port = it },
                        label = "Puerto",
                        modifier = Modifier.weight(0.3f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StyledInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Contraseña",
                        modifier = Modifier.weight(0.7f),
                        trailingIcon = {
                            IconButton(onClick = { onFetchScenes() }) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = Color(0xFF64B5F6),
                                )
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryActionButton(
                    text = if (connectionState == WebSocketClient.ConnectionState.CONNECTED) "CONECTADO" else "COMPUTADORA DESCONECTADA",
                    onClick = onFetchScenes,
                    containerColor = Color(0xFF2D313E),
                    contentColor = Color(0xFF9499B0),
                    icon = {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ACCIONES RÁPIDAS Section
        SectionHeader(text = "ACCIONES RÁPIDAS")
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D313E)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        showScenes = !showScenes
                        if (showScenes) onFetchScenes()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showScenes) Color(0xFF4F46E5) else Color(0xFF2D313E),
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "ESCENAS Y COMANDOS OBS", fontWeight = FontWeight.Bold)
                }

                if (showScenes) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val displayItems: List<Pair<String, String>> = if (scenes.isNotEmpty()) {
                        scenes.map { cmd ->
                            val label = when {
                                cmd.startsWith("SOURCE_TOGGLE:") -> {
                                    val parts = cmd.removePrefix("SOURCE_TOGGLE:").split("|", limit = 2)
                                    if (parts.size == 2) "👁️ ${parts[1]} (${parts[0]})" else cmd
                                }
                                cmd.startsWith("MUTE_INPUT:") -> "🎙️ ${cmd.removePrefix("MUTE_INPUT:")}"
                                cmd == "START_RECORD" -> "🔴 Grabación"
                                cmd == "START_STREAM" -> "📡 Stream"
                                cmd == "TOGGLE_MUTE" -> "🎙️ Mute Audio"
                                else -> "🎬 $cmd"
                            }
                            label to cmd
                        }
                    } else {
                        defaultObsQuickActions
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp),
                    ) {
                        items(displayItems, key = { it.second }) { (title, command) ->
                            Surface(
                                color = Color(0xFF0F0F14),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitchToScene(command) },
                            ) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scan Status Card
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(100.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Monitor,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF2D313E),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Información adicional de OBS",
                        color = Color(0xFF4B4F5E),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
