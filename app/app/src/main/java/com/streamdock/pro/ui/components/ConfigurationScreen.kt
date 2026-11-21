package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.core.AppConfig
import com.streamdock.pro.data.local.ConnectionSettingsManager
import com.streamdock.pro.network.discovery.DiscoveryManager

@Composable
fun ConfigurationScreen(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    isScanning: Boolean,
    onRefreshScan: () -> Unit,
    discoveredServers: List<DiscoveryManager.DiscoveredServer>,
    onServerSelected: (String, Int) -> Unit,
    onUsbSelected: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // USB/WiFi Selector
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D313E)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ConnectionTypeItem(
                    title = "USB",
                    icon = Icons.Default.Usb,
                    isSelected = currentMode == ConnectionSettingsManager.MODE_USB,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeSelected(ConnectionSettingsManager.MODE_USB) },
                )
                ConnectionTypeItem(
                    title = "WiFi",
                    icon = Icons.Default.Wifi,
                    isSelected = currentMode == ConnectionSettingsManager.MODE_WIFI,
                    modifier = Modifier.weight(1f),
                    onClick = { onModeSelected(ConnectionSettingsManager.MODE_WIFI) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Computers Card
        Surface(
            color = Color(0xFF161921),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF2D313E)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SERVIDORES DISPONIBLES",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2196F3),
                        )
                    } else {
                        IconButton(onClick = onRefreshScan, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "Escanear", tint = Color(0xFF2196F3))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (currentMode == ConnectionSettingsManager.MODE_USB) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = "CONEXIÓN USB (ADB)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Para que la conexión USB funcione, debes ejecutar el siguiente comando en tu computadora:",
                                    color = Color(0xFF9499B0),
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = Color(0xFF0F0F14),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "adb reverse tcp:8081 tcp:8081",
                                        color = Color(0xFF00E676),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                PrimaryActionButton(
                                    text = "CONECTAR POR USB",
                                    onClick = onUsbSelected,
                                    containerColor = Color(0xFF2D313E),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Asegúrate de que el servidor StreamDock esté abierto en tu PC.",
                                    color = Color(0xFF4B4F5E),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    } else {
                        if (discoveredServers.isEmpty() && !isScanning) {
                            item {
                                Text(
                                    text = "No se encontraron servidores. Pulsa el icono de búsqueda para reintentar.",
                                    color = Color(0xFF4B4F5E),
                                    fontSize = 13.sp,
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

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2D313E), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader(text = "CONEXIÓN MANUAL")

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StyledInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre",
                        placeholder = "Nombre",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StyledInput(
                        value = ip,
                        onValueChange = { ip = it },
                        label = "IP (:Puerto)",
                        placeholder = "192.168.1.15:8081",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryActionButton(
                    text = "CONECTAR MANUALMENTE",
                    onClick = {
                        val raw = ip.trim()
                        if (raw.isNotBlank()) {
                            val parts = raw.split(":", limit = 2)
                            val host = parts[0].trim()
                            val port = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: AppConfig.SERVER_PORT
                            if (host.isNotBlank()) {
                                onServerSelected(host, port)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun ServerItem(name: String, host: String, port: Int, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1A1B26),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF64B5F6))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "$host:$port", color = Color(0xFF9499B0), fontSize = 12.sp)
            }
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
            .height(80.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2196F3) else Color(0xFF9499B0),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color(0xFF2196F3) else Color(0xFF9499B0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
