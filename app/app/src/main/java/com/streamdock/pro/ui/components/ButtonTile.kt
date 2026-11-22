package com.streamdock.pro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

private const val CONFIG_HOLD_MS = 600L
private const val MOVE_HOLD_MS = 6_000L

@Composable
fun ButtonTile(
    position: Int,
    label: String,
    iconName: String? = null,
    iconTint: String? = null,
    backgroundColor: String? = null,
    fontSize: Int? = null,
    actionType: String? = null,
    actionValue: String? = null,
    isActive: Boolean = false,
    isEditMode: Boolean = false,
    isMovingSource: Boolean = false,
    isMoveDestinationCandidate: Boolean = false,
    tileHeight: Dp? = null,
    sliderVolumePercent: Int? = null,
    onSliderVolumeChange: ((Int, Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStartMove: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnStartMove by rememberUpdatedState(onStartMove)
    val currentOnSliderChange by rememberUpdatedState(onSliderVolumeChange)

    val hasAction = !actionType.isNullOrBlank() && actionType != "NONE"
    val hasIcon = !iconName.isNullOrBlank() && iconName != "none"
    val isConfigured = label.isNotBlank() || hasAction || hasIcon
    val isVolumeSliderTile = sliderVolumePercent != null && onSliderVolumeChange != null

    val scale by animateFloatAsState(
        targetValue = when {
            isMovingSource -> 0.93f
            isPressed -> 0.91f
            else -> 1f
        },
        label = "scale",
    )

    val defaultColor = MaterialTheme.colorScheme.surface
    val baseColor = remember(backgroundColor, defaultColor, isConfigured) {
        if (!backgroundColor.isNullOrBlank()) {
            try {
                Color(backgroundColor.toColorInt())
            } catch (_: Exception) {
                if (isConfigured) Color(0xFF1E293B) else Color(0xFF13151E)
            }
        } else {
            if (isConfigured) Color(0xFF1E293B) else Color(0xFF13151E)
        }
    }

    val resolvedIconTint = remember(iconTint) {
        if (!iconTint.isNullOrBlank()) {
            try {
                Color(iconTint.toColorInt())
            } catch (_: Exception) {
                Color.White
            }
        } else {
            Color.White
        }
    }

    val bitmapIcon = remember(iconName) { IconMapper.getBitmap(iconName) }
    val vectorIcon = remember(iconName, actionType, actionValue) {
        IconMapper.getIconForButton(iconName, actionType, actionValue)
    }

    val isCompactTile = tileHeight != null && tileHeight < 88.dp
    val cornerRadius = if (isCompactTile) 14.dp else 18.dp
    val shape = RoundedCornerShape(cornerRadius)

    val gradient = if (isConfigured) {
        Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.95f),
                baseColor.copy(alpha = 0.78f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF13151F),
                Color(0xFF0E1017),
            ),
        )
    }

    val borderColor = when {
        isMovingSource -> Color(0xFF00E676)
        isMoveDestinationCandidate -> Color(0xFF38BDF8).copy(alpha = 0.75f)
        isPressed -> Color(0xFF38BDF8)
        isActive -> Color(0xFF00E676)
        isEditMode && isConfigured -> Color(0xFFF59E0B).copy(alpha = 0.8f)
        isEditMode && !isConfigured -> Color(0xFF38BDF8).copy(alpha = 0.35f)
        isConfigured -> Color.White.copy(alpha = 0.16f)
        else -> Color(0xFF1F2332)
    }

    val borderWidth = when {
        isMovingSource -> 2.5.dp
        isPressed || isActive -> 2.dp
        isMoveDestinationCandidate || isEditMode -> 1.5.dp
        else -> 1.dp
    }

    val sizeModifier = if (tileHeight != null) {
        Modifier
            .fillMaxWidth()
            .height(tileHeight)
    } else {
        Modifier.aspectRatio(1f)
    }

    val dragSliderModifier = if (isVolumeSliderTile && !isEditMode) {
        Modifier.pointerInput(position, isVolumeSliderTile, isEditMode) {
            var lastDragPct = sliderVolumePercent ?: 80
            detectVerticalDragGestures(
                onDragStart = { offset ->
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val ratio = 1f - (offset.y / h).coerceIn(0f, 1f)
                    lastDragPct = (ratio * 100f).roundToInt().coerceIn(0, 100)
                    currentOnSliderChange?.invoke(lastDragPct, false)
                },
                onVerticalDrag = { change, _ ->
                    change.consume()
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val ratio = 1f - (change.position.y / h).coerceIn(0f, 1f)
                    val pct = (ratio * 100f).roundToInt().coerceIn(0, 100)
                    lastDragPct = pct
                    currentOnSliderChange?.invoke(pct, false)
                },
                onDragEnd = {
                    currentOnSliderChange?.invoke(lastDragPct, true)
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = sizeModifier
            .scale(scale)
            .clip(shape)
            .background( if (isVolumeSliderTile) Brush.verticalGradient(listOf(Color(0xFF141724), Color(0xFF0E1019))) else gradient )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape,
            )
            .then(dragSliderModifier)
            .pointerInput(position) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)
                    scope.launch { interactionSource.emit(press) }

                    // 0 = tap (<600ms), 1 = config (600ms..6000ms), 2 = move (>=6000ms)
                    val stage = AtomicInteger(0)

                    val timerJob = scope.launch {
                        delay(CONFIG_HOLD_MS)
                        stage.set(1)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        delay(MOVE_HOLD_MS - CONFIG_HOLD_MS)
                        stage.set(2)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnStartMove()
                    }

                    val up = waitForUpOrCancellation()
                    timerJob.cancel()

                    scope.launch {
                        interactionSource.emit(
                            if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                        )
                    }

                    if (up != null) {
                        when (stage.get()) {
                            0 -> {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnClick()
                            }
                            1 -> currentOnLongClick()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // If this tile is a Volume Slider Tile, show the vertical fill bar from bottom to top!
        if (isVolumeSliderTile && sliderVolumePercent != null) {
            val fillFraction = (sliderVolumePercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.85f),
                                baseColor.copy(alpha = 0.35f),
                            ),
                        ),
                    ),
            )
        }

        // 3D Glass Keycap top highlight bevel for configured buttons
        if (isConfigured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(if (isCompactTile) 16.dp else 22.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isPressed) 0.25f else 0.14f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        // Active / Pressed inner glow overlay
        if (isActive || isPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isPressed) Color(0xFF38BDF8).copy(alpha = 0.12f)
                        else Color(0xFF00E676).copy(alpha = 0.12f),
                    ),
            )
        }

        // Edit badge in top-right corner when in Edit Mode on a configured button
        if (isEditMode && isConfigured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF11111B),
                    modifier = Modifier.size(10.dp),
                )
            }
        }

        if (!isConfigured) {
            // Empty Recessed Slot Visual
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ranura Vacía",
                    tint = if (isEditMode) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color(0xFF282D3E),
                    modifier = Modifier.size(if (isCompactTile) 18.dp else 24.dp),
                )
                if (isEditMode && !isCompactTile) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AÑADIR",
                        color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isCompactTile) 4.dp else 6.dp,
                        vertical = if (isCompactTile) 4.dp else 7.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (isVolumeSliderTile && sliderVolumePercent != null) {
                    Text(
                        text = "🎚️ ${sliderVolumePercent}%",
                        color = Color.White,
                        fontSize = if (isCompactTile) 13.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                } else if (bitmapIcon != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = bitmapIcon,
                            contentDescription = label.ifEmpty { "App Icon" },
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier
                                .fillMaxSize(if (label.isEmpty()) 0.86f else 0.78f)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    }
                    if (label.isNotEmpty()) Spacer(modifier = Modifier.height(if (isCompactTile) 2.dp else 4.dp))
                } else if (vectorIcon != null) {
                    val vectorSize = when {
                        isCompactTile && label.isNotEmpty() -> 24.dp
                        isCompactTile -> 32.dp
                        label.isEmpty() -> 48.dp
                        else -> 38.dp
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = vectorIcon,
                            contentDescription = label.ifEmpty { iconName },
                            tint = resolvedIconTint,
                            modifier = Modifier.size(vectorSize),
                        )
                    }
                    if (label.isNotEmpty()) Spacer(modifier = Modifier.height(if (isCompactTile) 2.dp else 4.dp))
                }

                if (label.isNotEmpty()) {
                    val defaultTextSp = if (isCompactTile) 9 else 11
                    val textSize = (fontSize?.coerceIn(8, 22) ?: defaultTextSp).sp
                    Text(
                        text = label.uppercase(),
                        color = Color.White,
                        fontSize = textSize,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = (textSize.value + 1.5f).sp,
                        maxLines = if (isCompactTile) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
