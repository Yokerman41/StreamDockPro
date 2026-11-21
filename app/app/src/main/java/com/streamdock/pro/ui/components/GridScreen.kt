package com.streamdock.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile
import com.streamdock.pro.data.models.Workspace

@Composable
fun GridScreen(
    workspace: Workspace?,
    isLandscape: Boolean = false,
    onButtonPress: (position: Int) -> Unit,
    onCreatePage: (String, Int, Int) -> Unit,
    onSelectPage: (String) -> Unit,
    onUpdateButton: (profileId: String, position: Int, label: String, icon: String, color: String, type: String, value: String) -> Unit,
    onDeleteButton: (profileId: String, position: Int) -> Unit,
    onMoveButton: (profileId: String, fromPosition: Int, toPosition: Int) -> Unit,
) {
    var showNamingDialog by remember { mutableStateOf(value = false) }
    var selectedButtonForConfig by remember { mutableStateOf<ButtonModel?>(null) }
    var movingFromPosition by remember(workspace?.activeProfileId) { mutableStateOf<Int?>(null) }

    if (showNamingDialog) {
        PageNamingDialog(
            onDismiss = { showNamingDialog = false },
            onConfirm = { name, rows, cols ->
                onCreatePage(name, rows, cols)
                showNamingDialog = false
            },
        )
    }

    selectedButtonForConfig?.let { button ->
        ButtonConfigDialog(
            button = button,
            availableProfiles = workspace?.profiles.orEmpty(),
            onDismiss = { selectedButtonForConfig = null },
            onSave = { label, icon, color, type, value ->
                onUpdateButton(button.profileId, button.position, label, icon, color, type, value)
                selectedButtonForConfig = null
            },
            onDelete = {
                onDeleteButton(button.profileId, button.position)
                selectedButtonForConfig = null
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(
                horizontal = if (isLandscape) 12.dp else 8.dp,
                vertical = if (isLandscape) 4.dp else 8.dp,
            ),
    ) {
        if (workspace != null && workspace.profiles.isNotEmpty()) {
            val activeProfile = workspace.profiles.find { it.id == workspace.activeProfileId } ?: workspace.profiles.first()
            val activeButtonsByPos = remember(workspace.buttons, activeProfile.id) {
                workspace.buttons
                    .filter { it.profileId == activeProfile.id }
                    .associateBy { it.position }
            }

            PageSelector(
                profiles = workspace.profiles,
                activeProfileId = activeProfile.id,
                compact = isLandscape,
                onSelectPage = { profileId ->
                    movingFromPosition = null
                    onSelectPage(profileId)
                },
                onAddClick = { showNamingDialog = true },
            )

            // Banner de Modo Mover cuando se mantuvo presionado un botón por más de 6 segundos
            movingFromPosition?.let { fromPos ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF1A2E26),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Moviendo botón #${fromPos + 1}: toca la nueva posición",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(
                            onClick = { movingFromPosition = null },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar mover",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))

            val rawRows = activeProfile.gridRows.coerceAtLeast(1)
            val rawCols = activeProfile.gridCols.coerceAtLeast(1)

            // En horizontal, si el perfil fue diseñado vertical (más filas que columnas, p.ej. 4x3 o 5x4),
            // rotamos la cuadrícula (3x4 o 4x5) igual que un Stream Deck físico en modo horizontal.
            val displayCols = if (isLandscape && rawRows > rawCols) rawRows else rawCols
            val displayRows = if (isLandscape && rawRows > rawCols) rawCols else rawRows
            val totalCells = rawRows * rawCols
            val spacing = 6.dp

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellWidth = (maxWidth - spacing * (displayCols - 1)) / displayCols
                val fitHeight = (maxHeight - spacing * (displayRows - 1)) / displayRows

                // En horizontal ajustamos la altura de cada botón para que todas las filas quepan
                // en pantalla sin cortarse (con un mínimo táctil cómodo de 68.dp).
                val tileHeight = if (isLandscape) {
                    fitHeight.coerceIn(68.dp, cellWidth)
                } else {
                    cellWidth
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(displayCols),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = totalCells,
                        key = { index -> "${activeProfile.id}:$index" },
                    ) { index ->
                        val button = activeButtonsByPos[index]
                        val isBeingMoved = movingFromPosition == index
                        val isMoveTarget = movingFromPosition != null && movingFromPosition != index
                        ButtonTile(
                            position = index,
                            label = button?.label.orEmpty(),
                            iconName = button?.iconName,
                            iconTint = button?.iconTint,
                            backgroundColor = button?.backgroundColor,
                            fontSize = button?.fontSize,
                            actionType = button?.actionType,
                            actionValue = button?.actionValue,
                            isActive = button?.isActiveState ?: false,
                            isMovingSource = isBeingMoved,
                            isMoveDestinationCandidate = isMoveTarget,
                            tileHeight = tileHeight,
                            onClick = {
                                val fromPos = movingFromPosition
                                if (fromPos != null) {
                                    if (fromPos != index) {
                                        onMoveButton(activeProfile.id, fromPos, index)
                                    }
                                    movingFromPosition = null
                                } else {
                                    onButtonPress(index)
                                }
                            },
                            onLongClick = {
                                if (movingFromPosition != null) {
                                    movingFromPosition = null
                                } else {
                                    selectedButtonForConfig = button ?: ButtonModel(
                                        id = "btn-${activeProfile.id}-$index",
                                        profileId = activeProfile.id,
                                        position = index,
                                        label = "",
                                        iconName = "none",
                                        actionType = "NONE",
                                        actionValue = "",
                                    )
                                }
                            },
                            onStartMove = {
                                selectedButtonForConfig = null
                                movingFromPosition = index
                            },
                        )
                    }
                }
            }
        } else {
            EmptyStateView(onCreateClick = { showNamingDialog = true })
        }
    }
}

@Composable
fun PageSelector(
    profiles: List<Profile>,
    activeProfileId: String,
    compact: Boolean = false,
    onSelectPage: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        profiles.forEach { profile ->
            val isSelected = profile.id == activeProfileId
            Surface(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onSelectPage(profile.id) },
                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1A1B26),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = profile.name,
                    color = Color.White,
                    modifier = Modifier.padding(
                        horizontal = if (compact) 12.dp else 16.dp,
                        vertical = if (compact) 5.dp else 8.dp,
                    ),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(if (compact) 30.dp else 36.dp)
                .background(Color(0xFF1A1B26), RoundedCornerShape(8.dp)),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Añadir Página",
                tint = Color.White,
                modifier = Modifier.size(if (compact) 18.dp else 24.dp),
            )
        }
    }
}

@Composable
fun EmptyStateView(onCreateClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 64.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                contentDescription = null,
                tint = Color(0xFF1E212A),
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No hay páginas creadas",
                color = Color(0xFF9499B0),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5),
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crear Primera Página",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
