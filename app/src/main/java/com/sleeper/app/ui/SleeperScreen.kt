package com.sleeper.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sleeper.app.model.InputPhase
import com.sleeper.app.model.SleeperUiState
import com.sleeper.app.ui.card.CardFace
import kotlin.math.abs

/**
 * Full-screen composable that shows either a pure black screen (input modes)
 * or the selected card (display mode) with multi-touch gesture support.
 */
@Composable
fun SleeperScreen(
    uiState: SleeperUiState,
    onTransformUpdate: (panDelta: Offset, zoomDelta: Float, rotationDelta: Float) -> Unit,
    onDismiss: () -> Unit,
    onEmergencyReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (uiState.phase != InputPhase.DISPLAY) {
                    Modifier.pointerInput(Unit) {
                        detectTwoFingerLongPress(onEmergencyReset)
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.phase == InputPhase.DISPLAY && uiState.selectedCard != null) {
            CardDisplayLayer(
                uiState = uiState,
                onTransformUpdate = onTransformUpdate,
                onDismiss = onDismiss,
                onEmergencyReset = onEmergencyReset
            )
        }
    }
}

@Composable
private fun CardDisplayLayer(
    uiState: SleeperUiState,
    onTransformUpdate: (Offset, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    onEmergencyReset: () -> Unit
) {
    val card = uiState.selectedCard ?: return
    val transform = uiState.cardTransform
    val density = LocalDensity.current

    // Track cumulative horizontal drag for swipe detection
    var swipeDragX by remember { mutableFloatStateOf(0f) }
    var isTransforming by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Card width: ~60% of screen width
        val cardWidth = maxWidth * 0.6f

        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTwoFingerLongPress(onEmergencyReset)
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        isTransforming = true
                        onTransformUpdate(pan, zoom, rotation)
                    }
                }
                .pointerInput(Unit) {
                    // Swipe detection for dismiss
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        swipeDragX = 0f
                        isTransforming = false

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: break

                            if (change.changedToUp()) {
                                // Check if this was a swipe (not a multi-touch transform)
                                if (!isTransforming && abs(swipeDragX) > with(density) { 100.dp.toPx() }) {
                                    onDismiss()
                                }
                                break
                            }

                            // Only track single-finger drags for swipe detection
                            if (event.changes.size == 1) {
                                val dragDelta = change.position - change.previousPosition
                                swipeDragX += dragDelta.x
                            } else {
                                isTransforming = true
                            }
                        }
                    }
                }
                .graphicsLayer {
                    translationX = transform.offset.x
                    translationY = transform.offset.y
                    scaleX = transform.scale
                    scaleY = transform.scale
                    rotationZ = transform.rotation
                }
                .width(cardWidth),
            contentAlignment = Alignment.Center
        ) {
            CardFace(card = card)
        }
    }
}

/**
 * Detects a two-finger long press (hold for 1 second with 2 pointers).
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTwoFingerLongPress(
    onLongPress: () -> Unit
) {
    awaitEachGesture {
        // Wait for first finger
        awaitFirstDown()
        // Wait for potential second finger
        val startTime = System.currentTimeMillis()
        while (true) {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size >= 2) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= 1000L) {
                    onLongPress()
                    // Consume all changes
                    event.changes.forEach { it.consume() }
                    break
                }
            }

            // All fingers released
            if (activePointers.isEmpty()) {
                break
            }
        }
    }
}
