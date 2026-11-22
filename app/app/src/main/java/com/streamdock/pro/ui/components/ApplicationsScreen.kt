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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                    Box(modifier = Modifier.weight(1f)) {
                        SpotifyQuickCard(onQuickAction = onQuickAction)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ActiveWindowQuickCard(onQuickAction = onQuickAction)
                        DesktopAndAudioQuickCard(onQuickAction = onQuickAction)
                    }
                }
            } else {
                // Portrait Single Column Layout
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
