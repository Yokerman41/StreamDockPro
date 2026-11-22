package com.streamdock.pro.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.data.models.Workspace
import com.streamdock.pro.network.discovery.DiscoveryManager
import com.streamdock.pro.network.websocket.WebSocketClient

@Composable
fun MainScreen(
    workspace: Workspace?,
    connectionState: WebSocketClient.ConnectionState,
    onButtonPress: (position: Int) -> Unit,
    onCreatePage: (String, Int, Int) -> Unit,
    onSelectPage: (String) -> Unit,
    onUpdateButton: (profileId: String, position: Int, label: String, icon: String, color: String, type: String, value: String) -> Unit,
    onDeleteButton: (profileId: String, position: Int) -> Unit,
    onMoveButton: (profileId: String, fromPosition: Int, toPosition: Int) -> Unit,
    currentConnectionMode: String,
    onModeSelected: (String) -> Unit,
    isScanning: Boolean,
    onRefreshScan: () -> Unit,
    discoveredServers: List<DiscoveryManager.DiscoveredServer>,
    onConnectToServer: (String, Int) -> Unit,
    onUsbSelected: () -> Unit,
    onDeleteProfile: () -> Unit,
    obsScenes: List<String>,
    onFetchObsScenes: () -> Unit,
    onSwitchToScene: (String) -> Unit,
    onQuickAction: (actionType: String, actionValue: String) -> Unit = { _, _ -> },
    processes: List<com.streamdock.pro.network.dto.ProcessItem> = emptyList(),
    onFetchProcesses: () -> Unit = {},
    onKillProcess: (Long) -> Unit = {},
) {
    var selectedRoute by rememberSaveable { mutableStateOf(NavItem.Panel.route) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var isConsoleMode by rememberSaveable { mutableStateOf(false) }
    val isPanelRoute = selectedRoute == NavItem.Panel.route
    val canDeleteProfile = isPanelRoute && (workspace?.profiles?.size ?: 0) > 1
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navItem = when (selectedRoute) {
        NavItem.Panel.route -> NavItem.Panel
        NavItem.Aplicaciones.route -> NavItem.Aplicaciones
        NavItem.OBS.route -> NavItem.OBS
        NavItem.Tareas.route -> NavItem.Tareas
        NavItem.Config.route -> NavItem.Config
        else -> NavItem.Panel
    }

    val onStatusPillClick: () -> Unit = {
        if (selectedRoute != NavItem.Config.route) {
            isConsoleMode = false
            selectedRoute = NavItem.Config.route
        } else {
            onRefreshScan()
        }
    }

    val screenContent: @Composable () -> Unit = {
        when (selectedRoute) {
            NavItem.Panel.route -> {
                GridScreen(
                    workspace = workspace,
                    isLandscape = isLandscape,
                    isEditMode = isEditMode,
                    isConsoleMode = isConsoleMode,
                    onButtonPress = onButtonPress,
                    onCreatePage = onCreatePage,
                    onSelectPage = onSelectPage,
                    onUpdateButton = onUpdateButton,
                    onDeleteButton = onDeleteButton,
                    onMoveButton = onMoveButton,
                    onQuickAction = onQuickAction,
                )
            }
            NavItem.Aplicaciones.route -> {
                ApplicationsScreen(
                    onQuickAction = onQuickAction,
                )
            }
            NavItem.OBS.route -> {
                OBSScreen(
                    connectionState = connectionState,
                    scenes = obsScenes,
                    onFetchScenes = onFetchObsScenes,
                    onSwitchToScene = onSwitchToScene,
                )
            }
            NavItem.Tareas.route -> {
                TaskManagerScreen(
                    connectionState = connectionState,
                    processes = processes,
                    onFetchProcesses = onFetchProcesses,
                    onKillProcess = onKillProcess,
                )
            }
            NavItem.Config.route -> {
                ConfigurationScreen(
                    currentMode = currentConnectionMode,
                    connectionState = connectionState,
                    onModeSelected = onModeSelected,
                    isScanning = isScanning,
                    onRefreshScan = onRefreshScan,
                    discoveredServers = discoveredServers,
                    onServerSelected = onConnectToServer,
                    onUsbSelected = onUsbSelected,
                )
            }
        }
    }

    if (isConsoleMode && isPanelRoute) {
        // Modo Consola Pantalla Completa (100% Stream Deck Hardware View)
        val activeProfileName = workspace?.profiles
            ?.find { it.id == workspace.activeProfileId }
            ?.name ?: "Stream Dock"
        val isConnected = connectionState == WebSocketClient.ConnectionState.CONNECTED

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra superior ultra-compacta de Consola (permite ver página activa y salir en 1 toque)
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF9800)),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$activeProfileName  •  Desliza ◀ ▶ para cambiar página",
                        color = Color(0xFF9499B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        onClick = { isConsoleMode = false },
                        color = Color(0xFF161925),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF283046)),
                        modifier = Modifier.height(26.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Salir Modo Consola",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MENÚ",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    screenContent()
                }
            }
        }
    } else if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14))
                .navigationBarsPadding(),
        ) {
            SideNavRail(
                selectedRoute = selectedRoute,
                onRouteSelected = { selectedRoute = it },
            )
            Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                CommandHeader(
                    title = navItem.label,
                    icon = navItem.icon,
                    connectionState = connectionState,
                    showDeleteButton = canDeleteProfile,
                    showEditButton = isPanelRoute,
                    isEditMode = isEditMode,
                    onToggleEditMode = { isEditMode = !isEditMode },
                    showConsoleButton = isPanelRoute,
                    onEnterConsoleMode = { isConsoleMode = true },
                    onStatusClick = onStatusPillClick,
                    compact = true,
                    onDeleteClick = onDeleteProfile,
                )
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    screenContent()
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                CommandHeader(
                    title = navItem.label,
                    icon = navItem.icon,
                    connectionState = connectionState,
                    showDeleteButton = canDeleteProfile,
                    showEditButton = isPanelRoute,
                    isEditMode = isEditMode,
                    onToggleEditMode = { isEditMode = !isEditMode },
                    showConsoleButton = isPanelRoute,
                    onEnterConsoleMode = { isConsoleMode = true },
                    onStatusClick = onStatusPillClick,
                    compact = false,
                    onDeleteClick = onDeleteProfile,
                )
            },
            bottomBar = {
                BottomNavBar(
                    selectedRoute = selectedRoute,
                    onRouteSelected = { selectedRoute = it },
                )
            },
            containerColor = Color(0xFF0F0F14),
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                screenContent()
            }
        }
    }
}
