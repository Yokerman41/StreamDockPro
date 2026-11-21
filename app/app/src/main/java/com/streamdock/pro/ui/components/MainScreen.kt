package com.streamdock.pro.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
) {
    var selectedRoute by rememberSaveable { mutableStateOf(NavItem.Panel.route) }
    val canDeleteProfile = selectedRoute == NavItem.Panel.route && (workspace?.profiles?.size ?: 0) > 1
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navItem = when (selectedRoute) {
        NavItem.Panel.route -> NavItem.Panel
        NavItem.Aplicaciones.route -> NavItem.Aplicaciones
        NavItem.OBS.route -> NavItem.OBS
        NavItem.StreamerBot.route -> NavItem.StreamerBot
        NavItem.Config.route -> NavItem.Config
        else -> NavItem.Panel
    }

    val screenContent: @Composable () -> Unit = {
        when (selectedRoute) {
            NavItem.Panel.route -> {
                GridScreen(
                    workspace = workspace,
                    isLandscape = isLandscape,
                    onButtonPress = onButtonPress,
                    onCreatePage = onCreatePage,
                    onSelectPage = onSelectPage,
                    onUpdateButton = onUpdateButton,
                    onDeleteButton = onDeleteButton,
                    onMoveButton = onMoveButton,
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
            NavItem.StreamerBot.route -> {
                StreamerBotScreen(connectionState = connectionState)
            }
            NavItem.Config.route -> {
                ConfigurationScreen(
                    currentMode = currentConnectionMode,
                    onModeSelected = onModeSelected,
                    isScanning = isScanning,
                    onRefreshScan = onRefreshScan,
                    discoveredServers = discoveredServers,
                    onServerSelected = onConnectToServer,
                    onUsbSelected = onUsbSelected,
                )
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Pantalla de ${selectedRoute.uppercase()}",
                        color = Color.White,
                    )
                }
            }
        }
    }

    if (isLandscape) {
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
