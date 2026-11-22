package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.core.AppConfig
import com.streamdock.pro.data.local.ConnectionSettingsManager
import com.streamdock.pro.network.discovery.DiscoveryManager
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun ConfigurationScreen(
    currentMode: String,
    connectionState: WebSocketClient.ConnectionState = WebSocketClient.ConnectionState.DISCONNECTED,
    onModeSelected: (String) -> Unit,
    isScanning: Boolean,
    onRefreshScan: () -> Unit,
    discoveredServers: List<DiscoveryManager.DiscoveredServer>,
    onServerSelected: (String, Int) -> Unit,
    onUsbSelected: () -> Unit,
) {
    var ip by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
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
            // Live Connection Status Banner
            Surface(
                color = if (isConnected) Color(0xFF0E231A) else Color(0xFF151822),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isConnected) Color(0xFF00E676).copy(alpha = 0.6f) else Color(0xFF262B3D),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF9800)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isConnected) "CONECTADO A TU PC" else "SIN CONEXIÓN ACTIVA",
                            color = if (isConnected) Color(0xFF00E676) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = if (isConnected) {
                                "Modo activo: $currentMode • Los cambios se sincronizan en tiempo real"
                            } else {
                                "Elige WiFi (misma red) o Cable USB (Depuración USB) para vincular tu PC"
                            },
                            color = Color(0xFF9499B0),
                            fontSize = 11.sp,
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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ConnectionModeSelectorCard(
                            currentMode = currentMode,
                            onModeSelected = onModeSelected,
                        )
                        AutoDiscoveryCard(
                            currentMode = currentMode,
                            isScanning = isScanning,
                            onRefreshScan = onRefreshScan,
                            discoveredServers = discoveredServers,
                            onServerSelected = onServerSelected,
                            onUsbSelected = onUsbSelected,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ManualIpConnectionCard(
                            ip = ip,
                            onIpChange = { ip = it },
                            onPasteIp = {
                                val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (clip.isNotBlank()) {
                                    ip = clip
                                }
                            },
                            onConnectIp = { rawIp ->
                                val parts = rawIp.trim().split(":", limit = 2)
                                val host = parts[0].trim()
                                val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: AppConfig.SERVER_PORT
                                if (host.isNotBlank()) {
                                    onServerSelected(host, port)
                                }
                            },
                        )
                    }
                }
            } else {
                // Portrait Single Column Layout
                ConnectionModeSelectorCard(
                    currentMode = currentMode,
                    onModeSelected = onModeSelected,
                )
                AutoDiscoveryCard(
                    currentMode = currentMode,
                    isScanning = isScanning,
                    onRefreshScan = onRefreshScan,
                    discoveredServers = discoveredServers,
                    onServerSelected = onServerSelected,
                    onUsbSelected = onUsbSelected,
                )
                ManualIpConnectionCard(
                    ip = ip,
                    onIpChange = { ip = it },
                    onPasteIp = {
                        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (clip.isNotBlank()) {
                            ip = clip
                        }
                    },
                    onConnectIp = { rawIp ->
                        val parts = rawIp.trim().split(":", limit = 2)
                        val host = parts[0].trim()
                        val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: AppConfig.SERVER_PORT
                        if (host.isNotBlank()) {
                            onServerSelected(host, port)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ConnectionModeSelectorCard(
    currentMode: String,
    onModeSelected: (String) -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ConnectionTypeItem(
                title = "WiFi Local",
                icon = Icons.Default.Wifi,
                isSelected = currentMode == ConnectionSettingsManager.MODE_WIFI,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ConnectionSettingsManager.MODE_WIFI) },
            )
            ConnectionTypeItem(
                title = "Cable USB",
                icon = Icons.Default.Usb,
                isSelected = currentMode == ConnectionSettingsManager.MODE_USB,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(ConnectionSettingsManager.MODE_USB) },
            )
        }
    }
}

@Composable
private fun AutoDiscoveryCard(
    currentMode: String,
    isScanning: Boolean,
    onRefreshScan: () -> Unit,
    discoveredServers: List<DiscoveryManager.DiscoveredServer>,
    onServerSelected: (String, Int) -> Unit,
    onUsbSelected: () -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (currentMode == ConnectionSettingsManager.MODE_USB) "CONEXIÓN POR CABLE USB" else "PC ENCONTRADAS EN TU RED WIFI",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                if (currentMode == ConnectionSettingsManager.MODE_WIFI) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF38BDF8),
                        )
                    } else {
                        IconButton(onClick = onRefreshScan, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "Escanear", tint = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (currentMode == ConnectionSettingsManager.MODE_USB) {
                Text(
                    text = "1. Conecta tu teléfono a la PC por USB con la Depuración USB activada.\n2. Abre Stream Dock Pro en tu PC (el puente ADB se activa solo).\n3. Toca el botón de abajo:",
                    color = Color(0xFF9499B0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryActionButton(
                    text = "CONECTAR POR CABLE USB",
                    onClick = onUsbSelected,
                    containerColor = Color(0xFF2563EB),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (discoveredServers.isEmpty() && !isScanning) {
                        item {
                            Text(
                                text = "No se detectó ninguna PC automáticamente. Toca la lupa para buscar o usa la Conexión Rápida por IP.",
                                color = Color(0xFF7C829B),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    items(
                        items = discoveredServers,
                        key = { "${it.host}:${it.port}" },
                    ) { server ->
                        ServerItem(
                            name = server.name,
                            host = server.host,
                            port = server.port,
                            onClick = { onServerSelected(server.host, server.port) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualIpConnectionCard(
    ip: String,
    onIpChange: (String) -> Unit,
    onPasteIp: () -> Unit,
    onConnectIp: (String) -> Unit,
) {
    Surface(
        color = Color(0xFF151822),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF262B3D)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "CONEXIÓN DIRECTA POR IP (1 PASO)",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Button(
                    onClick = onPasteIp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Pegar IP",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pegar IP",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Escribe la IP que aparece arriba en el Editor de PC (ej. 192.168.1.15:8081):",
                color = Color(0xFF9499B0),
                fontSize = 11.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            StyledInput(
                value = ip,
                onValueChange = onIpChange,
                label = "Dirección IP de tu PC (:Puerto)",
                placeholder = "192.168.1.15:8081",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryActionButton(
                text = "CONECTAR A ESTA IP",
                onClick = { onConnectIp(ip) },
                containerColor = Color(0xFF4F46E5),
            )
        }
    }
}

@Composable
fun ServerItem(name: String, host: String, port: Int, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1E2231),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$host:$port", color = Color(0xFF9499B0), fontSize = 12.sp)
            }
            Text(
                text = "CONECTAR →",
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun ConnectionTypeItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF262B3D),
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.1f) else Color(0xFF1A1D29),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF9499B0),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFF9499B0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
