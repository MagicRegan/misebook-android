package com.misebook.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A simple long-press drag-to-reorder column.
 *
 * Items expose a [ReorderableItemScope.dragHandle] modifier that must be attached
 * to the element the user presses to start the drag (typically an icon). Once a
 * drag is active the full row is translated vertically; when the cumulative
 * offset crosses half of the row's height, [onMove] is called with the new
 * target index and the translation is rebased.
 */
@Composable
fun <T : Any> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 10.dp,
    itemContent: @Composable ReorderableItemScope.(item: T, index: Int) -> Unit
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { spacing.toPx() }
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { index, item ->
            val itemKey = key(item)
            val isDragging = draggedKey == itemKey
            val translation = if (isDragging) dragOffsetPx else 0f

            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords -> heights[itemKey] = coords.size.height }
                    .zIndex(if (isDragging) 1f else 0f)
                    .shadow(elevation = if (isDragging) 8.dp else 0.dp)
                    .graphicsLayer {
                        translationY = translation
                        alpha = if (isDragging) 0.97f else 1f
                    }
            ) {
                val scope = ReorderableItemScope(
                    isDragging = isDragging,
                    dragHandleFactory = {
                        Modifier.pointerInput(itemKey, items.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedKey = itemKey
                                    dragOffsetPx = 0f
                                },
                                onDragEnd = {
                                    draggedKey = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggedKey = null
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetPx += amount.y
                                    val currentIndex = items.indexOfFirst { key(it) == itemKey }
                                    if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                    val ownHeight = (heights[itemKey] ?: 0).toFloat() + spacingPx
                                    if (ownHeight <= 0f) return@detectDragGesturesAfterLongPress
                                    val threshold = ownHeight / 2f
                                    if (dragOffsetPx > threshold && currentIndex < items.lastIndex) {
                                        onMove(currentIndex, currentIndex + 1)
                                        dragOffsetPx -= ownHeight
                                    } else if (dragOffsetPx < -threshold && currentIndex > 0) {
                                        onMove(currentIndex, currentIndex - 1)
                                        dragOffsetPx += ownHeight
                                    }
                                }
                            )
                        }
                    }
                )
                scope.itemContent(item, index)
            }
        }
    }
}

class ReorderableItemScope internal constructor(
    val isDragging: Boolean,
    private val dragHandleFactory: () -> Modifier
) {
    /** Attach this modifier to the element the user should long-press to start dragging. */
    fun dragHandle(): Modifier = dragHandleFactory()
}
