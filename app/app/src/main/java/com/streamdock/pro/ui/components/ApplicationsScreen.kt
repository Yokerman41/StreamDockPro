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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ApplicationsScreen(
    onQuickAction: (actionType: String, actionValue: String) -> Unit,
) {
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
            if (isWide) {
                // Adaptive 2-Column Layout for Landscape & Tablets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        VoicemeeterQuickCard(onQuickAction = onQuickAction)
                        SpotifyQuickCard(onQuickAction = onQuickAction)
                    }
                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ActiveWindowQuickCard(onQuickAction = onQuickAction)
                        DesktopAndAudioQuickCard(onQuickAction = onQuickAction)
                    }
                }
            } else {
                // Portrait Single Column Layout
                VoicemeeterQuickCard(onQuickAction = onQuickAction)
                SpotifyQuickCard(onQuickAction = onQuickAction)
                ActiveWindowQuickCard(onQuickAction = onQuickAction)
                DesktopAndAudioQuickCard(onQuickAction = onQuickAction)
            }
        }
    }
}

@Composable
private fun SpotifyQuickCard(
    onQuickAction: (actionType: String, actionValue: String) -> Unit,
) {
    Surface(
        color = Color(0xFF112019),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPOTIFY EXCLUSIVO (2DO PLANO)",
                        color = Color(0xFF1DB954),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    color = Color(0xFF1DB954),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { onQuickAction("MEDIA_CONTROL", "SPOTIFY_OPEN") },
                ) {
                    Text(
                        text = "Abrir App",
                        color = Color(0xFF09140E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    label = "Anterior",
                    icon = Icons.Default.SkipPrevious,
                    accentColor = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_PREV") },
                )
                QuickActionTile(
                    label = "Play/Pausa",
                    icon = Icons.Default.PlayArrow,
                    accentColor = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_PLAY_PAUSE") },
                )
                QuickActionTile(
                    label = "Siguiente",
                    icon = Icons.Default.SkipNext,
                    accentColor = Color(0xFF1DB954),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_NEXT") },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    label = "Vol -",
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_VOL_DOWN") },
                )
                QuickActionTile(
                    label = "Mute Spotify",
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_MUTE") },
                )
                QuickActionTile(
                    label = "Vol +",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    accentColor = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_VOL_UP") },
                )
            }
        }
    }
}

@Composable
private fun ActiveWindowQuickCard(
    onQuickAction: (actionType: String, actionValue: String) -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "VENTANA ACTIVA EN PC",
                color = Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    label = "Minimizar",
                    icon = Icons.Default.Minimize,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("LAUNCH_APP", "WIN_MINIMIZE") },
                )
                QuickActionTile(
                    label = "Maximizar",
                    icon = Icons.Default.Fullscreen,
                    accentColor = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("LAUNCH_APP", "WIN_MAXIMIZE") },
                )
                QuickActionTile(
                    label = "Cerrar",
                    icon = Icons.Default.Close,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("LAUNCH_APP", "WIN_CLOSE") },
                )
            }
        }
    }
}

@Composable
private fun DesktopAndAudioQuickCard(
    onQuickAction: (actionType: String, actionValue: String) -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "AUDIO MAESTRO Y ESCRITORIO WINDOWS",
                color = Color(0xFFA855F7),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    label = "Vol PC -",
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "VOLUME_DOWN") },
                )
                QuickActionTile(
                    label = "Mute PC",
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    accentColor = Color(0xFFF43F5E),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "VOLUME_MUTE") },
                )
                QuickActionTile(
                    label = "Vol PC +",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    accentColor = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("MEDIA_CONTROL", "VOLUME_UP") },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    label = "Escritorio",
                    icon = Icons.Default.Computer,
                    accentColor = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("SYSTEM_COMMAND", "SHOW_DESKTOP") },
                )
                QuickActionTile(
                    label = "Bloquear PC",
                    icon = Icons.Default.Lock,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = { onQuickAction("SYSTEM_COMMAND", "LOCK_WORKSTATION") },
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = Color(0xFF1E2230),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VoicemeeterQuickCard(
    onQuickAction: (actionType: String, actionValue: String) -> Unit,
) {
    val routeStates = remember {
        mutableStateMapOf(
            "out_A1" to true,
            "out_A2" to true,
            "out_A3" to true,
            "out_B1" to true,
            "out_B2" to true,
            "mic_mute" to false,
            "mic_A1" to false,
            "mic_A2" to false,
            "mic_A3" to false,
            "mic_B1" to true,
            "mic_B2" to false,
            "vaio_mute" to false,
            "vaio_A1" to false,
            "vaio_A2" to true,
            "vaio_A3" to false,
            "vaio_B1" to false,
            "vaio_B2" to false,
            "aux_mute" to false,
            "aux_A1" to true,
            "aux_A2" to false,
            "aux_A3" to false,
            "aux_B1" to false,
            "aux_B2" to false,
        )
    }

    Surface(
        color = Color(0xFF1E1A16),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFB86C).copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFFFB86C),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VOICEMEETER BANANA",
                            color = Color(0xFFFFB86C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Matriz de Entradas y Salidas A1..B2",
                            color = Color(0xFFA6ADC8),
                            fontSize = 10.sp,
                        )
                    }
                }

                Surface(
                    color = Color(0xFF332516),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB86C).copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        onQuickAction("VOICEMEETER", "VOICEMEETER_RESTART")
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar",
                            tint = Color(0xFFFFB86C),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reiniciar",
                            color = Color(0xFFFFB86C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ================= SALIDAS MASTER (OUTPUTS A1..B2) =================
            Text(
                text = "SALIDAS MASTER (ACTIVAR / DESACTIVAR)",
                color = Color(0xFFF9E2AF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val outputs = listOf(
                    Triple("out_A1", "A1", "VOICEMEETER_OUTPUT_A1"),
                    Triple("out_A2", "A2", "VOICEMEETER_OUTPUT_A2"),
                    Triple("out_A3", "A3", "VOICEMEETER_OUTPUT_A3"),
                    Triple("out_B1", "B1", "VOICEMEETER_OUTPUT_B1"),
                    Triple("out_B2", "B2", "VOICEMEETER_OUTPUT_B2"),
                )
                outputs.forEach { (key, label, cmd) ->
                    val isActive = routeStates[key] ?: true
                    Surface(
                        color = if (isActive) Color(0xFF263321) else Color(0xFF2B181C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isActive) Color(0xFFA6E3A1) else Color(0xFFF38BA8)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                routeStates[key] = !isActive
                                onQuickAction("VOICEMEETER", cmd)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = label,
                                color = if (isActive) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = if (isActive) "ON" else "MUTE",
                                color = if (isActive) Color(0xFFA6E3A1) else Color(0xFFF38BA8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= ENTRADAS Y ENRUTAMIENTO =================
            Text(
                text = "ENRUTAMIENTO DE ENTRADAS (ROUTING A1..B2)",
                color = Color(0xFF89B4FA),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Micrófono
            VoicemeeterStripRow(
                title = "🎙️ Micrófono (Input 1)",
                subtitle = "Realtek Audio",
                isMuted = routeStates["mic_mute"] ?: false,
                onToggleMute = {
                    val m = !(routeStates["mic_mute"] ?: false)
                    routeStates["mic_mute"] = m
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].Mute")
                },
                busA1 = routeStates["mic_A1"] ?: false,
                busA2 = routeStates["mic_A2"] ?: false,
                busA3 = routeStates["mic_A3"] ?: false,
                busB1 = routeStates["mic_B1"] ?: true,
                busB2 = routeStates["mic_B2"] ?: false,
                onToggleBus = { bus ->
                    val k = "mic_$bus"
                    val cur = routeStates[k] ?: false
                    routeStates[k] = !cur
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[0].$bus")
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. VAIO PC
            VoicemeeterStripRow(
                title = "🔊 PC / Juegos (VAIO)",
                subtitle = "Voicemeeter Input",
                isMuted = routeStates["vaio_mute"] ?: false,
                onToggleMute = {
                    val m = !(routeStates["vaio_mute"] ?: false)
                    routeStates["vaio_mute"] = m
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].Mute")
                },
                busA1 = routeStates["vaio_A1"] ?: false,
                busA2 = routeStates["vaio_A2"] ?: true,
                busA3 = routeStates["vaio_A3"] ?: false,
                busB1 = routeStates["vaio_B1"] ?: false,
                busB2 = routeStates["vaio_B2"] ?: false,
                onToggleBus = { bus ->
                    val k = "vaio_$bus"
                    val cur = routeStates[k] ?: false
                    routeStates[k] = !cur
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[3].$bus")
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. AUX Música / Chat
            VoicemeeterStripRow(
                title = "🎵 Música / Chat (AUX)",
                subtitle = "Voicemeeter AUX Input",
                isMuted = routeStates["aux_mute"] ?: false,
                onToggleMute = {
                    val m = !(routeStates["aux_mute"] ?: false)
                    routeStates["aux_mute"] = m
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].Mute")
                },
                busA1 = routeStates["aux_A1"] ?: true,
                busA2 = routeStates["aux_A2"] ?: false,
                busA3 = routeStates["aux_A3"] ?: false,
                busB1 = routeStates["aux_B1"] ?: false,
                busB2 = routeStates["aux_B2"] ?: false,
                onToggleBus = { bus ->
                    val k = "aux_$bus"
                    val cur = routeStates[k] ?: false
                    routeStates[k] = !cur
                    onQuickAction("VOICEMEETER", "VOICEMEETER_TOGGLE:Strip[4].$bus")
                },
            )
        }
    }
}

@Composable
private fun VoicemeeterStripRow(
    title: String,
    subtitle: String,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    busA1: Boolean,
    busA2: Boolean,
    busA3: Boolean,
    busB1: Boolean,
    busB2: Boolean,
    onToggleBus: (String) -> Unit,
) {
    Surface(
        color = Color(0xFF14141E),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF313244)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFA6ADC8),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                color = if (isMuted) Color(0xFF4A1E24) else Color(0xFF262738),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isMuted) Color(0xFFF38BA8) else Color(0xFF45475A)),
                modifier = Modifier
                    .clickable { onToggleMute() }
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = if (isMuted) "MUTED" else "ACTIVO",
                    color = if (isMuted) Color(0xFFF38BA8) else Color(0xFFA6ADC8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                )
            }

            Row(
                modifier = Modifier.weight(1.8f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf(
                    Pair("A1", busA1),
                    Pair("A2", busA2),
                    Pair("A3", busA3),
                    Pair("B1", busB1),
                    Pair("B2", busB2),
                ).forEach { (bus, isEnabled) ->
                    val isBusB = bus.startsWith("B")
                    val activeColor = if (isBusB) Color(0xFFA6E3A1) else Color(0xFF89B4FA)
                    val activeBg = if (isBusB) Color(0xFF1E3827) else Color(0xFF1E2D48)

                    Surface(
                        color = if (isEnabled) activeBg else Color(0xFF181825),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, if (isEnabled) activeColor else Color(0xFF313244)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleBus(bus) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 5.dp),
                        ) {
                            Text(
                                text = bus,
                                color = if (isEnabled) activeColor else Color(0xFF6C7086),
                                fontSize = 10.sp,
                                fontWeight = if (isEnabled) FontWeight.ExtraBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
