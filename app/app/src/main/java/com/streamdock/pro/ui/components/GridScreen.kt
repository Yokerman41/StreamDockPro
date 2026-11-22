package com.streamdock.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamdock.pro.data.models.Button as ButtonModel
import com.streamdock.pro.data.models.Profile
import com.streamdock.pro.data.models.Workspace
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GridScreen(
    workspace: Workspace?,
    isLandscape: Boolean = false,
    isEditMode: Boolean = false,
    isConsoleMode: Boolean = false,
    onButtonPress: (position: Int) -> Unit,
    onCreatePage: (String, Int, Int) -> Unit,
    onSelectPage: (String) -> Unit,
    onUpdateButton: (profileId: String, position: Int, label: String, icon: String, color: String, type: String, value: String) -> Unit,
    onDeleteButton: (profileId: String, position: Int) -> Unit,
    onMoveButton: (profileId: String, fromPosition: Int, toPosition: Int) -> Unit,
    onQuickAction: (actionType: String, actionValue: String) -> Unit = { _, _ -> },
) {
    var showNamingDialog by remember { mutableStateOf(value = false) }
    var selectedButtonForConfig by remember { mutableStateOf<ButtonModel?>(null) }
    var movingFromPosition by remember(workspace?.activeProfileId) { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var pcVolumePercent by rememberSaveable { mutableIntStateOf(80) }
    var spotifyVolumePercent by rememberSaveable { mutableIntStateOf(85) }
    var lastPcSendTime by remember { mutableLongStateOf(0L) }
    var lastSpotifySendTime by remember { mutableLongStateOf(0L) }
    val vmSliderPercents = remember { mutableStateMapOf<String, Int>() }
    var lastVmSendTime by remember { mutableLongStateOf(0L) }

    val updatePcVolume: (Int, Boolean) -> Unit = { newPct, forceSend ->
        val clamped = newPct.coerceIn(0, 100)
        pcVolumePercent = clamped
        val now = System.currentTimeMillis()
        if (forceSend || now - lastPcSendTime >= 45L) {
            lastPcSendTime = now
            onQuickAction("MEDIA_CONTROL", "VOLUME_SET:$clamped")
        }
    }

    val updateSpotifyVolume: (Int, Boolean) -> Unit = { newPct, forceSend ->
        val clamped = newPct.coerceIn(0, 100)
        spotifyVolumePercent = clamped
        val now = System.currentTimeMillis()
        if (forceSend || now - lastSpotifySendTime >= 45L) {
            lastSpotifySendTime = now
            onQuickAction("MEDIA_CONTROL", "SPOTIFY_SET_VOL:$clamped")
        }
    }

    val updateVmVolume: (String, Int, Boolean) -> Unit = { actionKey, newPct, forceSend ->
        val clamped = newPct.coerceIn(0, 100)
        vmSliderPercents[actionKey] = clamped
        val now = System.currentTimeMillis()
        if (forceSend || now - lastVmSendTime >= 45L) {
            lastVmSendTime = now
            onQuickAction("VOICEMEETER", "$actionKey:$clamped")
        }
    }

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
                horizontal = if (isLandscape) 12.dp else 10.dp,
                vertical = if (isConsoleMode) 6.dp else if (isLandscape) 4.dp else 8.dp,
            ),
    ) {
        if (workspace != null && workspace.profiles.isNotEmpty()) {
            val profiles = workspace.profiles
            val activeIndex = profiles.indexOfFirst { it.id == workspace.activeProfileId }.coerceAtLeast(0)
            val activeProfile = profiles[activeIndex]

            val pagerState = rememberPagerState(
                initialPage = activeIndex,
                pageCount = { profiles.size },
            )

            // Sync external activeProfileId changes (e.g., PC auto-switch or GOTO_PAGE) into pagerState
            LaunchedEffect(activeIndex, profiles.size) {
                if (pagerState.currentPage != activeIndex && activeIndex in profiles.indices) {
                    pagerState.animateScrollToPage(activeIndex)
                }
            }

            // Sync user swipe on HorizontalPager back to activeProfileId
            LaunchedEffect(pagerState, profiles) {
                snapshotFlow { pagerState.settledPage }
                    .distinctUntilChanged()
                    .collect { pageIdx ->
                        val targetProfile = profiles.getOrNull(pageIdx)
                        if (targetProfile != null && targetProfile.id != workspace.activeProfileId) {
                            movingFromPosition = null
                            onSelectPage(targetProfile.id)
                        }
                    }
            }

            if (!isConsoleMode) {
                val activePageIndex = pagerState.currentPage.coerceIn(0, (profiles.size - 1).coerceAtLeast(0))
                val currentProfile = profiles.getOrNull(activePageIndex) ?: activeProfile
                val currentProfileButtons = remember(workspace.buttons, currentProfile.id) {
                    workspace.buttons
                        .filter { it.profileId == currentProfile.id }
                        .associateBy { it.position }
                }
                val activePcSliderBtn = remember(currentProfileButtons) {
                    currentProfileButtons.values.firstOrNull {
                        it.actionType == "MEDIA_CONTROL" &&
                            it.actionValue.equals("VOLUME_SLIDER_PC", ignoreCase = true)
                    }
                }
                val activeSpotifySliderBtn = remember(currentProfileButtons) {
                    currentProfileButtons.values.firstOrNull {
                        it.actionType == "MEDIA_CONTROL" &&
                            it.actionValue.equals("VOLUME_SLIDER_SPOTIFY", ignoreCase = true)
                    }
                }

                val findEmptyOrLastSlot: () -> Int = {
                    val total = (currentProfile.gridRows * currentProfile.gridCols).coerceAtLeast(1)
                    (0 until total).firstOrNull { pos ->
                        val b = currentProfileButtons[pos]
                        b == null || b.actionType.isBlank() || b.actionType == "NONE"
                    } ?: (total - 1)
                }

                PageSelector(
                    profiles = profiles,
                    activeProfileId = currentProfile.id,
                    compact = isLandscape,
                    hasPcVolumeBar = activePcSliderBtn != null,
                    hasSpotifyVolumeBar = activeSpotifySliderBtn != null,
                    onSelectPage = { profileId ->
                        movingFromPosition = null
                        val idx = profiles.indexOfFirst { it.id == profileId }
                        if (idx >= 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                        }
                        onSelectPage(profileId)
                    },
                    onAddClick = { showNamingDialog = true },
                    onTogglePcVolumeBar = {
                        if (activePcSliderBtn != null) {
                            onDeleteButton(currentProfile.id, activePcSliderBtn.position)
                        } else {
                            val slot = findEmptyOrLastSlot()
                            onUpdateButton(
                                currentProfile.id,
                                slot,
                                "Barra Vol. PC",
                                "vol_up",
                                "#7C3AED",
                                "MEDIA_CONTROL",
                                "VOLUME_SLIDER_PC",
                            )
                        }
                    },
                    onToggleSpotifyVolumeBar = {
                        if (activeSpotifySliderBtn != null) {
                            onDeleteButton(currentProfile.id, activeSpotifySliderBtn.position)
                        } else {
                            val slot = findEmptyOrLastSlot()
                            onUpdateButton(
                                currentProfile.id,
                                slot,
                                "Barra Spotify",
                                "vol_up",
                                "#1DB954",
                                "MEDIA_CONTROL",
                                "VOLUME_SLIDER_SPOTIFY",
                            )
                        }
                    },
                )
            }

            // Banner de Modo Edición rápida
            if (isEditMode && movingFromPosition == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF281F10),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "✏️ Modo Edición: toca cualquier tecla o barra de volumen para configurarla",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // Banner de Modo Mover
            movingFromPosition?.let { fromPos ->
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF122921),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.6f)),
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
                                text = "Moviendo tecla #${fromPos + 1}: toca la casilla destino",
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

            Spacer(modifier = Modifier.height(if (isLandscape || isConsoleMode) 6.dp else 10.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                pageSpacing = 12.dp,
            ) { pageIndex ->
                val pageProfile = profiles.getOrNull(pageIndex) ?: activeProfile
                val pageButtonsByPos = remember(workspace.buttons, pageProfile.id) {
                    workspace.buttons
                        .filter { it.profileId == pageProfile.id }
                        .associateBy { it.position }
                }

                val pcVolumeSliderButton = remember(pageButtonsByPos) {
                    pageButtonsByPos.values.firstOrNull {
                        it.actionType == "MEDIA_CONTROL" &&
                            it.actionValue.equals("VOLUME_SLIDER_PC", ignoreCase = true)
                    }
                }
                val spotifyVolumeSliderButton = remember(pageButtonsByPos) {
                    pageButtonsByPos.values.firstOrNull {
                        it.actionType == "MEDIA_CONTROL" &&
                            it.actionValue.equals("VOLUME_SLIDER_SPOTIFY", ignoreCase = true)
                    }
                }
                val hasVolumeBarSpace = pcVolumeSliderButton != null || spotifyVolumeSliderButton != null

                val rawRows = pageProfile.gridRows.coerceAtLeast(1)
                val rawCols = pageProfile.gridCols.coerceAtLeast(1)

                // Adaptabilidad inteligente: en horizontal (Landscape), rotamos perfiles verticales
                val displayCols = if (isLandscape && rawRows > rawCols) rawRows else rawCols
                val displayRows = if (isLandscape && rawRows > rawCols) rawCols else rawRows
                val totalCells = rawRows * rawCols

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 8.dp),
                ) {
                    // Cuadrícula Principal de Botones del Perfil
                    @Suppress("UnusedBoxWithConstraintsScope")
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isWideScreen = this.maxWidth >= 600.dp
                        val spacing = if (isWideScreen) 10.dp else 6.dp
                        val cellWidth = ((this.maxWidth - spacing * (displayCols - 1)) / displayCols).coerceAtLeast(44.dp)
                        val fitHeight = ((this.maxHeight - spacing * (displayRows - 1)) / displayRows).coerceAtLeast(44.dp)

                        val tileHeight = minOf(cellWidth, fitHeight).coerceAtLeast(52.dp)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(displayCols),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                count = totalCells,
                                key = { index -> "${pageProfile.id}:$index" },
                            ) { index ->
                                val button = pageButtonsByPos[index]
                                val isBeingMoved = movingFromPosition == index
                                val isMoveTarget = movingFromPosition != null && movingFromPosition != index
                                val isPcSliderTile = button?.actionValue.equals("VOLUME_SLIDER_PC", ignoreCase = true)
                                val isSpotifySliderTile = button?.actionValue.equals("VOLUME_SLIDER_SPOTIFY", ignoreCase = true)
                                val isVmSliderTile = button?.actionValue?.startsWith("VOLUME_SLIDER_VM_", ignoreCase = true) == true
                                val vmActionKey = button?.actionValue.orEmpty()

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
                                    isEditMode = isEditMode,
                                    isMovingSource = isBeingMoved,
                                    isMoveDestinationCandidate = isMoveTarget,
                                    tileHeight = tileHeight,
                                    sliderVolumePercent = when {
                                        isPcSliderTile -> pcVolumePercent
                                        isSpotifySliderTile -> spotifyVolumePercent
                                        isVmSliderTile -> vmSliderPercents[vmActionKey] ?: 100
                                        else -> null
                                    },
                                    onSliderVolumeChange = when {
                                        isPcSliderTile -> { pct, final -> updatePcVolume(pct, final) }
                                        isSpotifySliderTile -> { pct, final -> updateSpotifyVolume(pct, final) }
                                        isVmSliderTile -> { pct, final -> updateVmVolume(vmActionKey, pct, final) }
                                        else -> null
                                    },
                                    onClick = {
                                        val fromPos = movingFromPosition
                                        if (fromPos != null) {
                                            if (fromPos != index) {
                                                onMoveButton(pageProfile.id, fromPos, index)
                                            }
                                            movingFromPosition = null
                                        } else if (isEditMode) {
                                            selectedButtonForConfig = button ?: ButtonModel(
                                                id = "btn-${pageProfile.id}-$index",
                                                profileId = pageProfile.id,
                                                position = index,
                                                label = "",
                                                iconName = "none",
                                                actionType = "NONE",
                                                actionValue = "",
                                            )
                                        } else {
                                            onButtonPress(index)
                                        }
                                    },
                                    onLongClick = {
                                        if (movingFromPosition != null) {
                                            movingFromPosition = null
                                        } else if (isEditMode) {
                                            movingFromPosition = index
                                        } else {
                                            selectedButtonForConfig = button ?: ButtonModel(
                                                id = "btn-${pageProfile.id}-$index",
                                                profileId = pageProfile.id,
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

                    // ESPACIO DEDICADO DE BARRAS DE VOLUMEN DENTRO DEL PERFIL
                    if (hasVolumeBarSpace) {
                        ProfileVolumeFaderDock(
                            pcSliderButton = pcVolumeSliderButton,
                            spotifySliderButton = spotifyVolumeSliderButton,
                            pcVolumePercent = pcVolumePercent,
                            spotifyVolumePercent = spotifyVolumePercent,
                            isCompact = !isLandscape,
                            isEditMode = isEditMode,
                            onPcVolumeChange = updatePcVolume,
                            onSpotifyVolumeChange = updateSpotifyVolume,
                            onMutePc = { onQuickAction("MEDIA_CONTROL", "VOLUME_MUTE") },
                            onMuteSpotify = { onQuickAction("MEDIA_CONTROL", "SPOTIFY_MUTE") },
                            onConfigureButton = { btn -> selectedButtonForConfig = btn },
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
private fun ProfileVolumeFaderDock(
    pcSliderButton: ButtonModel?,
    spotifySliderButton: ButtonModel?,
    pcVolumePercent: Int,
    spotifyVolumePercent: Int,
    isCompact: Boolean,
    isEditMode: Boolean,
    onPcVolumeChange: (Int, Boolean) -> Unit,
    onSpotifyVolumeChange: (Int, Boolean) -> Unit,
    onMutePc: () -> Unit,
    onMuteSpotify: () -> Unit,
    onConfigureButton: (ButtonModel) -> Unit,
) {
    Surface(
        color = Color(0xFF131520),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF283046)),
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = if (isCompact) 6.dp else 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎚️ VOLUMEN",
                color = Color(0xFF38BDF8),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
            ) {
                if (pcSliderButton != null) {
                    VerticalVolumeFaderBar(
                        title = "PC",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        accentColor = Color(0xFFA855F7),
                        volumePercent = pcVolumePercent,
                        barWidth = if (isCompact) 50.dp else 56.dp,
                        isEditMode = isEditMode,
                        onVolumeChange = onPcVolumeChange,
                        onMuteClick = onMutePc,
                        onEditClick = { onConfigureButton(pcSliderButton) },
                    )
                }

                if (spotifySliderButton != null) {
                    VerticalVolumeFaderBar(
                        title = "SPO",
                        icon = Icons.Default.MusicNote,
                        accentColor = Color(0xFF1DB954),
                        volumePercent = spotifyVolumePercent,
                        barWidth = if (isCompact) 50.dp else 56.dp,
                        isEditMode = isEditMode,
                        onVolumeChange = onSpotifyVolumeChange,
                        onMuteClick = onMuteSpotify,
                        onEditClick = { onConfigureButton(spotifySliderButton) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalVolumeFaderBar(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    volumePercent: Int,
    barWidth: androidx.compose.ui.unit.Dp,
    isEditMode: Boolean,
    onVolumeChange: (Int, Boolean) -> Unit,
    onMuteClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticStep by remember { mutableIntStateOf(volumePercent / 5) }

    Column(
        modifier = Modifier
            .width(barWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181B28))
            .border(
                width = 1.5.dp,
                color = if (isEditMode) Color(0xFFF59E0B) else accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: Icon/Title or Edit button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditClick),
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Edit else icon,
                contentDescription = title,
                tint = if (isEditMode) Color(0xFFF59E0B) else accentColor,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // Percentage readout
        Text(
            text = "$volumePercent%",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 3.dp),
        )

        // Vertical Touch Slider Track (Deslizar de arriba 100% a abajo 0%)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D0F17))
                .border(1.dp, Color(0xFF283046), RoundedCornerShape(10.dp))
                .pointerInput(isEditMode) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (isEditMode) {
                                onEditClick()
                            } else {
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val ratio = 1f - (offset.y / h).coerceIn(0f, 1f)
                                val pct = (ratio * 100f).roundToInt().coerceIn(0, 100)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onVolumeChange(pct, true)
                            }
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEditClick()
                        },
                    )
                }
                .pointerInput(Unit) {
                    var currentDragPct = volumePercent
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val ratio = 1f - (offset.y / h).coerceIn(0f, 1f)
                            currentDragPct = (ratio * 100f).roundToInt().coerceIn(0, 100)
                            onVolumeChange(currentDragPct, false)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val ratio = 1f - (change.position.y / h).coerceIn(0f, 1f)
                            val pct = (ratio * 100f).roundToInt().coerceIn(0, 100)
                            currentDragPct = pct
                            val step = pct / 5
                            if (step != lastHapticStep) {
                                lastHapticStep = step
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onVolumeChange(pct, false)
                        },
                        onDragEnd = {
                            onVolumeChange(currentDragPct, true)
                        },
                    )
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val fillFraction = (volumePercent / 100f).coerceIn(0f, 1f)

            // Filled Level Bar from bottom to current volumePercent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.45f),
                            ),
                        ),
                    ),
            )

            // Horizontal Fader Cap Indicator line at top of fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Bottom Quick Mute Button
        Surface(
            onClick = onMuteClick,
            color = Color(0xFF23283B),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Silenciar $title",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
fun PageSelector(
    profiles: List<Profile>,
    activeProfileId: String,
    compact: Boolean = false,
    hasPcVolumeBar: Boolean = false,
    hasSpotifyVolumeBar: Boolean = false,
    onSelectPage: (String) -> Unit,
    onAddClick: () -> Unit,
    onTogglePcVolumeBar: () -> Unit = {},
    onToggleSpotifyVolumeBar: () -> Unit = {},
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
                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF151822),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF262B3D),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 11.dp else 14.dp,
                        vertical = if (compact) 5.dp else 7.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8)),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = profile.name,
                        color = if (isSelected) Color.White else Color(0xFF9499B0),
                        fontSize = if (compact) 11.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.gridRows}×${profile.gridCols}",
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF565D75),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .padding(end = 6.dp)
                .clickable(onClick = onAddClick),
            color = Color(0xFF151822),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2E364F)),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 10.dp else 12.dp,
                    vertical = if (compact) 5.dp else 7.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Añadir Página",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(if (compact) 15.dp else 17.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Página",
                    color = Color(0xFF38BDF8),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Botón rápido para mostrar/ocultar Barra Vertical de Volumen PC en el Perfil
        Surface(
            modifier = Modifier
                .padding(end = 6.dp)
                .clickable(onClick = onTogglePcVolumeBar),
            color = if (hasPcVolumeBar) Color(0xFF2A1D4E) else Color(0xFF151822),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (hasPcVolumeBar) 1.5.dp else 1.dp,
                color = if (hasPcVolumeBar) Color(0xFFCBA6F7) else Color(0xFF2E364F),
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 9.dp else 11.dp,
                    vertical = if (compact) 5.dp else 7.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (hasPcVolumeBar) "🎚️ Vol PC ✓" else "🎚️ +Vol PC",
                    color = if (hasPcVolumeBar) Color(0xFFCBA6F7) else Color(0xFF9499B0),
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Botón rápido para mostrar/ocultar Barra Vertical de Volumen Spotify en el Perfil
        Surface(
            modifier = Modifier.clickable(onClick = onToggleSpotifyVolumeBar),
            color = if (hasSpotifyVolumeBar) Color(0xFF113022) else Color(0xFF151822),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (hasSpotifyVolumeBar) 1.5.dp else 1.dp,
                color = if (hasSpotifyVolumeBar) Color(0xFF1DB954) else Color(0xFF2E364F),
            ),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (compact) 9.dp else 11.dp,
                    vertical = if (compact) 5.dp else 7.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (hasSpotifyVolumeBar) "🟢 Vol Spotify ✓" else "🟢 +Spotify",
                    color = if (hasSpotifyVolumeBar) Color(0xFF1DB954) else Color(0xFF9499B0),
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
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
            modifier = Modifier.padding(bottom = 48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                contentDescription = null,
                tint = Color(0xFF232838),
                modifier = Modifier.size(100.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No hay páginas creadas",
                color = Color(0xFF9499B0),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
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
