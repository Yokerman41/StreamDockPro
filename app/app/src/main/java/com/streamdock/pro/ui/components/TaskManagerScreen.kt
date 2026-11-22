package com.streamdock.pro.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.network.dto.ProcessItem
import com.streamdock.pro.network.websocket.WebSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskManagerScreen(
    connectionState: WebSocketClient.ConnectionState,
    processes: List<ProcessItem>,
    onFetchProcesses: () -> Unit,
    onKillProcess: (Long) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyApps by remember { mutableStateOf(true) }
    var processToKill by remember { mutableStateOf<ProcessItem?>(null) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isConnected = connectionState == WebSocketClient.ConnectionState.CONNECTED

    // Cargar procesos al entrar a la pantalla
    LaunchedEffect(isConnected) {
        if (isConnected) {
            onFetchProcesses()
        }
    }

    // Auto-refresco suave cada 5 segundos si está conectado
    LaunchedEffect(isConnected) {
        while (isConnected) {
            delay(5000L)
            onFetchProcesses()
        }
    }

    val appsList = remember(processes) { processes.filter { it.isApp } }
    val totalCount = processes.size
    val appsCount = appsList.size

    val filteredProcesses = remember(processes, searchQuery, filterOnlyApps) {
        val baseList = if (filterOnlyApps) appsList else processes
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.name.lowercase().contains(q) ||
                    (it.windowTitle?.lowercase()?.contains(q) == true) ||
                    it.pid.toString().contains(q)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10)),
    ) {
        if (!isConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(54.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No hay conexión con la PC",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Conecta la app por Wi-Fi o cable USB en la pestaña 'Conexión' para ver y administrar los procesos de Windows en tiempo real.",
                        color = Color(0xFF7C829B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                    )
                }
            }
        } else {
            // LazyColumn unificada: el encabezado, la barra de búsqueda y los filtros
            // se deslizan hacia arriba y abajo naturalmente junto con los procesos.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = if (isLandscape) 16.dp else 14.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 1. ENCABEZADO SCROLLABLE
                item(key = "header_section") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADMINISTRADOR DE TAREAS",
                                    color = Color.White,
                                    fontSize = if (isLandscape) 14.sp else 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                            Text(
                                text = "Windows PC • $totalCount procesos ($appsCount apps activas)",
                                color = Color(0xFF7C829B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 17.dp),
                            )
                        }

                        // Botón de refrescar
                        Surface(
                            color = Color(0xFF161A28),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF283048)),
                            modifier = Modifier.clickable {
                                if (!isManualRefreshing) {
                                    isManualRefreshing = true
                                    onFetchProcesses()
                                    coroutineScope.launch {
                                        delay(600L)
                                        isManualRefreshing = false
                                    }
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isManualRefreshing) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(13.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Actualizar procesos",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Refrescar",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // 2. BARRA DE BÚSQUEDA Y OPCIONES "APPS CON VENTANA" / "TODOS LOS PROCESOS"
                // En horizontal se colocan en una sola fila compacta para ganar altura.
                // En vertical se colocan una debajo de otra.
                // Ambos se deslizan fluidamente al scrollear la lista.
                item(key = "search_and_filters_section") {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Barra de Búsqueda Compacta en Horizontal
                            Box(modifier = Modifier.weight(1.1f)) {
                                SearchBarField(
                                    searchQuery = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    height = 44.dp,
                                )
                            }

                            // Filtros de Botón en Horizontal
                            Box(modifier = Modifier.weight(0.9f)) {
                                FilterChipsRow(
                                    filterOnlyApps = filterOnlyApps,
                                    appsCount = appsCount,
                                    totalCount = totalCount,
                                    onSelectOnlyApps = { filterOnlyApps = it },
                                    height = 44.dp,
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                        ) {
                            SearchBarField(
                                searchQuery = searchQuery,
                                onQueryChange = { searchQuery = it },
                                height = 48.dp,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChipsRow(
                                filterOnlyApps = filterOnlyApps,
                                appsCount = appsCount,
                                totalCount = totalCount,
                                onSelectOnlyApps = { filterOnlyApps = it },
                                height = 40.dp,
                            )
                        }
                    }
                }

                // 3. ESTADO VACÍO CUANDO NO HAY COINCIDENCIAS
                if (filteredProcesses.isEmpty()) {
                    item(key = "empty_search_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    "No se encontraron procesos que coincidan con \"$searchQuery\""
                                } else {
                                    "Cargando lista de procesos de Windows..."
                                },
                                color = Color(0xFF7C829B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else if (isLandscape) {
                    // 4. LISTA EN MODO HORIZONTAL: 2 COLUMNAS PARALELAS
                    // Aprovecha el ancho completo de la pantalla para mostrar más tareas
                    items(
                        items = filteredProcesses.chunked(2),
                        key = { pair -> pair.joinToString("-") { "${it.pid}-${it.name}" } },
                    ) { rowPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (process in rowPair) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ProcessCard(
                                        process = process,
                                        onKillClick = { processToKill = process },
                                    )
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    // 4. LISTA EN MODO VERTICAL: 1 COLUMNA
                    items(
                        items = filteredProcesses,
                        key = { "${it.pid}-${it.name}" },
                    ) { process ->
                        ProcessCard(
                            process = process,
                            onKillClick = { processToKill = process },
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGO DE CONFIRMACIÓN PARA FINALIZAR PROCESO
    processToKill?.let { proc ->
        AlertDialog(
            onDismissRequest = { processToKill = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(36.dp),
                )
            },
            title = {
                Text(
                    text = "Finalizar Proceso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Deseas finalizar el proceso \"${proc.name}\"?",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!proc.windowTitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ventana: \"${proc.windowTitle}\"",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PID: ${proc.pid} • Memoria: ${proc.memoryMb} MB",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Si este programa tiene archivos sin guardar, los cambios podrían perderse.",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.5.sp,
                    )
                }
            },
            containerColor = Color(0xFF161A28),
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                Button(
                    onClick = {
                        val pid = proc.pid
                        processToKill = null
                        onKillProcess(pid)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "Finalizar Proceso",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { processToKill = null }) {
                    Text(
                        text = "Cancelar",
                        color = Color(0xFF9499B0),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            },
        )
    }
}

@Composable
private fun SearchBarField(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Buscar proceso o app (ej. chrome, obs, discord)...",
                color = Color(0xFF565D75),
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF7C829B),
                modifier = Modifier.size(17.dp),
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar",
                        tint = Color(0xFF7C829B),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF111420),
            unfocusedContainerColor = Color(0xFF111420),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF22283A),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    )
}

@Composable
private fun FilterChipsRow(
    filterOnlyApps: Boolean,
    appsCount: Int,
    totalCount: Int,
    onSelectOnlyApps: (Boolean) -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = if (filterOnlyApps) Color(0xFF1E293B) else Color(0xFF111420),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                if (filterOnlyApps) Color(0xFF38BDF8) else Color(0xFF22283A),
            ),
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clickable { onSelectOnlyApps(true) },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = if (filterOnlyApps) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Apps ($appsCount)",
                    color = if (filterOnlyApps) Color.White else Color(0xFF9499B0),
                    fontSize = 11.5.sp,
                    fontWeight = if (filterOnlyApps) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }

        Surface(
            color = if (!filterOnlyApps) Color(0xFF1E293B) else Color(0xFF111420),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
                1.dp,
                if (!filterOnlyApps) Color(0xFF38BDF8) else Color(0xFF22283A),
            ),
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clickable { onSelectOnlyApps(false) },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (!filterOnlyApps) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Todos ($totalCount)",
                    color = if (!filterOnlyApps) Color.White else Color(0xFF9499B0),
                    fontSize = 11.5.sp,
                    fontWeight = if (!filterOnlyApps) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ProcessCard(
    process: ProcessItem,
    onKillClick: () -> Unit,
) {
    Surface(
        color = Color(0xFF131622),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (process.isApp) Color(0xFF283248) else Color(0xFF1C202E),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Icono del proceso
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (process.isApp) Color(0xFF1E2842) else Color(0xFF171A26),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (process.isApp) Icons.Default.Computer else Icons.Default.Memory,
                    contentDescription = null,
                    tint = if (process.isApp) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Información del proceso
            Column(modifier = Modifier.weight(1f)) {
                val hasTitle = !process.windowTitle.isNullOrBlank()
                Text(
                    text = if (hasTitle) process.windowTitle else process.name,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = process.name,
                        color = if (hasTitle) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = "• PID: ${process.pid}",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Consumo de Memoria RAM
            if (process.memoryMb > 0) {
                val memColor = when {
                    process.memoryMb > 1000 -> Color(0xFFF43F5E)
                    process.memoryMb > 400 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Surface(
                    color = memColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, memColor.copy(alpha = 0.35f)),
                    modifier = Modifier.padding(end = 6.dp),
                ) {
                    Text(
                        text = "${process.memoryMb} MB",
                        color = memColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                    )
                }
            }

            // Botón para Finalizar Proceso
            Surface(
                color = Color(0xFF261622),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.4f)),
                modifier = Modifier.clickable(onClick = onKillClick),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Finalizar",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Finalizar",
                        color = Color(0xFFF43F5E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
