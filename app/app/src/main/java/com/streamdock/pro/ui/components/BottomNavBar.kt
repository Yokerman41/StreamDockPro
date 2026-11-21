package com.streamdock.pro.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Panel : NavItem("panel", "Panel", Icons.Default.GridView)
    object Aplicaciones : NavItem("apps", "Aplicaciones", Icons.Default.Widgets)
    object OBS : NavItem("obs", "OBS", Icons.Default.Monitor)
    object StreamerBot : NavItem("streamer_bot", "Streamer.bot", Icons.Default.SmartToy)
    object Config : NavItem("config", "Configuración", Icons.Default.Settings)
}

private val navItems = listOf(
    NavItem.Panel,
    NavItem.Aplicaciones,
    NavItem.OBS,
    NavItem.StreamerBot,
    NavItem.Config,
)

@Composable
fun BottomNavBar(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Color(0xFF0F0F14),
        contentColor = Color(0xFF9499B0),
    ) {
        navItems.forEach { item ->
            val isSelected = selectedRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onRouteSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF64B5F6) else Color(0xFF9499B0),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) Color(0xFF64B5F6) else Color(0xFF9499B0),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun SideNavRail(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
) {
    NavigationRail(
        containerColor = Color(0xFF0F0F14),
        contentColor = Color(0xFF9499B0),
        modifier = Modifier
            .fillMaxHeight()
            .navigationBarsPadding(),
    ) {
        navItems.forEach { item ->
            val isSelected = selectedRoute == item.route
            NavigationRailItem(
                selected = isSelected,
                onClick = { onRouteSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF64B5F6) else Color(0xFF9499B0),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) Color(0xFF64B5F6) else Color(0xFF9499B0),
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
