package com.streamdock.pro.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Panel : NavItem("panel", "Panel Deck", Icons.Default.GridView)
    object Aplicaciones : NavItem("apps", "Quick Deck", Icons.Default.Bolt)
    object OBS : NavItem("obs", "OBS Studio", Icons.Default.Videocam)
    object Config : NavItem("config", "Conexión", Icons.Default.SettingsInputAntenna)
}

private val navItems = listOf(
    NavItem.Panel,
    NavItem.Aplicaciones,
    NavItem.OBS,
    NavItem.Config,
)

@Composable
fun BottomNavBar(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Color(0xFF0B0C10),
        contentColor = Color(0xFF9499B0),
        tonalElevation = 8.dp,
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
                        tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1E293B),
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
        containerColor = Color(0xFF0B0C10),
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
                        tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF7C829B),
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = Color(0xFF1E293B),
                ),
            )
        }
    }
}
