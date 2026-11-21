package com.streamdock.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun StreamerBotScreen(
    connectionState: WebSocketClient.ConnectionState,
) {
    var port by remember { mutableStateOf("8080") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D313E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SectionHeader(text = "CONFIGURACIÓN STREAMER.BOT")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    StyledInput(
                        value = port,
                        onValueChange = { port = it },
                        label = "Puerto",
                        modifier = Modifier.weight(0.3f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StyledInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Contraseña",
                        modifier = Modifier.weight(0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PrimaryActionButton(
                    text = if (connectionState == WebSocketClient.ConnectionState.CONNECTED) "CONECTADO" else "COMPUTADORA DESCONECTADA",
                    onClick = { /* TODO */ },
                    containerColor = Color(0xFF2D313E),
                    contentColor = Color(0xFF9499B0),
                    icon = {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }
    }
}
