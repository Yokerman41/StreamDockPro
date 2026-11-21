package com.streamdock.pro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun CommandHeader(
    title: String,
    icon: ImageVector,
    connectionState: WebSocketClient.ConnectionState,
    showDeleteButton: Boolean = true,
    compact: Boolean = false,
    onDeleteClick: () -> Unit,
) {
    val statusColor = when (connectionState) {
        WebSocketClient.ConnectionState.CONNECTED -> Color(0xFF00E676)
        WebSocketClient.ConnectionState.ERROR -> Color(0xFFFF5252)
        else -> Color(0xFFFF5722)
    }

    val statusText = when (connectionState) {
        WebSocketClient.ConnectionState.CONNECTED -> "CONNECTED"
        WebSocketClient.ConnectionState.CONNECTING,
        WebSocketClient.ConnectionState.REGISTERING,
        -> "CONNECTING"
        WebSocketClient.ConnectionState.ERROR -> "ERROR"
        WebSocketClient.ConnectionState.DISCONNECTED -> "DISCONNECTED"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
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
            .statusBarsPadding()
            .padding(
                horizontal = 8.dp,
                vertical = if (compact) 2.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left side: Icon + Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(if (compact) 20.dp else 24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                color = Color(0xFF64B5F6),
                fontSize = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }

        // Right side: Status + Trash
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF151921),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(if (compact) 26.dp else 32.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                statusColor.copy(
                                    alpha = if (connectionState != WebSocketClient.ConnectionState.CONNECTED) alpha else 1f,
                                ),
                            ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            if (showDeleteButton) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(if (compact) 32.dp else 40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar página activa",
                        tint = Color(0xFF9499B0),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
