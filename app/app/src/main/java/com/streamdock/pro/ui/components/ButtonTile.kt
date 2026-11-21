package com.streamdock.pro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
    isMovingSource: Boolean = false,
    isMoveDestinationCandidate: Boolean = false,
    tileHeight: Dp? = null,
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

    val scale by animateFloatAsState(
        targetValue = when {
            isMovingSource -> 0.94f
            isPressed -> 0.92f
            else -> 1f
        },
        label = "scale",
    )

    val defaultColor = MaterialTheme.colorScheme.surface
    val baseColor = remember(backgroundColor, defaultColor) {
        if (!backgroundColor.isNullOrBlank()) {
            try {
                Color(backgroundColor.toColorInt())
            } catch (_: Exception) {
                defaultColor
            }
        } else {
            defaultColor
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

    val gradient = Brush.verticalGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.9f),
            baseColor,
        ),
    )

    val borderColor = when {
        isMovingSource -> Color(0xFF00E676)
        isMoveDestinationCandidate -> Color(0xFF64B5F6).copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val isCompactTile = tileHeight != null && tileHeight < 95.dp
    val sizeModifier = if (tileHeight != null) {
        Modifier
            .fillMaxWidth()
            .height(tileHeight)
    } else {
        Modifier.aspectRatio(1f)
    }

    Box(
        modifier = sizeModifier
            .scale(scale)
            .clip(RoundedCornerShape(if (isCompactTile) 12.dp else 16.dp))
            .background(gradient)
            .border(
                width = if (isMovingSource) 3.dp else if (isMoveDestinationCandidate) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(if (isCompactTile) 12.dp else 16.dp),
            )
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
            }
            .padding(
                horizontal = if (isCompactTile) 4.dp else 6.dp,
                vertical = if (isCompactTile) 4.dp else 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (bitmapIcon != null) {
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
                            .fillMaxSize(if (label.isEmpty()) 0.88f else 0.82f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
                if (label.isNotEmpty()) Spacer(modifier = Modifier.height(if (isCompactTile) 2.dp else 4.dp))
            } else if (vectorIcon != null) {
                val vectorSize = when {
                    isCompactTile && label.isNotEmpty() -> 28.dp
                    isCompactTile -> 36.dp
                    label.isEmpty() -> 56.dp
                    else -> 44.dp
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
                val defaultTextSp = if (isCompactTile) 10 else 11
                val textSize = (fontSize?.coerceIn(8, 24) ?: defaultTextSp).sp
                Text(
                    text = label.uppercase(),
                    color = Color.White,
                    fontSize = textSize,
                    fontFamily = FontFamily.Monospace,
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
