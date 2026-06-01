package com.sleeper.app.model

import androidx.compose.ui.geometry.Offset

/** The three possible phases of the input state machine. */
enum class InputPhase {
    VALUE_ENTRY,
    SUIT_ENTRY,
    DISPLAY
}

/** Holds the full UI state for the Sleeper app. */
data class SleeperUiState(
    val phase: InputPhase = InputPhase.VALUE_ENTRY,
    val valueCount: Int = 0,
    val suitCount: Int = 0,
    val selectedCard: PlayingCard? = null,
    val cardTransform: CardTransform = CardTransform()
)

/** Transform state for the displayed card (position, scale, rotation). */
data class CardTransform(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f
)
