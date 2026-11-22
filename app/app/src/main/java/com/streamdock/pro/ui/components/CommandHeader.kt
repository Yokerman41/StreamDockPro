package com.streamdock.pro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun CommandHeader(
    title: String,
    icon: ImageVector,
    connectionState: WebSocketClient.ConnectionState,
    showDeleteButton: Boolean = true,
    showEditButton: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    showConsoleButton: Boolean = false,
    onEnterConsoleMode: () -> Unit = {},
    onStatusClick: () -> Unit = {},
    compact: Boolean = false,
    onDeleteClick: () -> Unit,
) {
    val isConnected = connectionState == WebSocketClient.ConnectionState.CONNECTED
    val statusColor = when (connectionState) {
        WebSocketClient.ConnectionState.CONNECTED -> Color(0xFF00E676)
        WebSocketClient.ConnectionState.ERROR -> Color(0xFFFF5252)
        else -> Color(0xFFFF9800)
    }

    val statusText = when (connectionState) {
        WebSocketClient.ConnectionState.CONNECTED -> "ONLINE"
        WebSocketClient.ConnectionState.CONNECTING,
        WebSocketClient.ConnectionState.REGISTERING,
        -> "CONECTANDO"
        WebSocketClient.ConnectionState.ERROR -> "ERROR"
        WebSocketClient.ConnectionState.DISCONNECTED -> "CONECTAR"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0C10))
            .statusBarsPadding()
            .padding(
                horizontal = 10.dp,
                vertical = if (compact) 4.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left side: Icon + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(if (compact) 18.dp else 22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = if (compact) 14.sp else 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Right side: Fullscreen Console Mode + Edit Mode + Interactive Status Pill + Trash
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showConsoleButton) {
                Surface(
                    onClick = onEnterConsoleMode,
                    color = Color(0xFF161925),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF283046)),
                    modifier = Modifier.height(if (compact) 26.dp else 30.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Modo Consola Pantalla Completa",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp),
                        )
                        if (!compact) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "DECK",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (showEditButton) {
                Surface(
                    onClick = onToggleEditMode,
                    color = if (isEditMode) Color(0xFFF59E0B) else Color(0xFF161925),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isEditMode) Color(0xFFF59E0B) else Color(0xFF283046),
                    ),
                    modifier = Modifier.height(if (compact) 26.dp else 30.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Modo Edición",
                            tint = if (isEditMode) Color(0xFF11111B) else Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "LISTO" else "EDITAR",
                            color = if (isEditMode) Color(0xFF11111B) else Color(0xFFF59E0B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Interactive Connection Pill
            Surface(
                onClick = onStatusClick,
                color = if (isConnected) Color(0xFF0D2118) else Color(0xFF1F161A),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier.height(if (compact) 26.dp else 30.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                statusColor.copy(
                                    alpha = if (!isConnected) alpha else 1f,
                                ),
                            ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            if (showDeleteButton) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(if (compact) 28.dp else 34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar página activa",
                        tint = Color(0xFF9499B0),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
