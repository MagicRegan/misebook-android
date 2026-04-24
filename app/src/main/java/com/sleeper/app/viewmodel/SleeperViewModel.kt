package com.sleeper.app.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.sleeper.app.model.CardTransform
import com.sleeper.app.model.InputPhase
import com.sleeper.app.model.PlayingCard
import com.sleeper.app.model.Rank
import com.sleeper.app.model.SleeperUiState
import com.sleeper.app.model.Suit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the Sleeper state machine.
 *
 * State transitions:
 *  VALUE_ENTRY  --Vol Down--> SUIT_ENTRY
 *  SUIT_ENTRY   --Vol Up--->  DISPLAY (confirms card)
 *  DISPLAY      --Vol Up / swipe--> VALUE_ENTRY (resets)
 */
class SleeperViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SleeperUiState())
    val uiState: StateFlow<SleeperUiState> = _uiState.asStateFlow()

    /** Describes what happened so the Activity can trigger haptics. */
    enum class HapticEvent { NONE, SUIT_MODE, CONFIRM, RESET }

    private val _hapticEvent = MutableStateFlow(HapticEvent.NONE)
    val hapticEvent: StateFlow<HapticEvent> = _hapticEvent.asStateFlow()

    fun consumeHapticEvent() {
        _hapticEvent.value = HapticEvent.NONE
    }

    /** Called when Volume Up is pressed. */
    fun onVolumeUp() {
        _uiState.update { state ->
            when (state.phase) {
                InputPhase.VALUE_ENTRY -> {
                    val newCount = if (state.valueCount >= 13) 1 else state.valueCount + 1
                    state.copy(valueCount = newCount)
                }
                InputPhase.SUIT_ENTRY -> {
                    // Confirm: resolve the card and show it
                    val rank = Rank.fromCount(state.valueCount)
                    val suit = Suit.fromCount(state.suitCount)
                    if (rank != null && suit != null) {
                        _hapticEvent.value = HapticEvent.CONFIRM
                        state.copy(
                            phase = InputPhase.DISPLAY,
                            selectedCard = PlayingCard(rank, suit),
                            cardTransform = CardTransform()
                        )
                    } else {
                        // Invalid input — reset
                        _hapticEvent.value = HapticEvent.RESET
                        SleeperUiState()
                    }
                }
                InputPhase.DISPLAY -> {
                    // Dismiss and reset
                    _hapticEvent.value = HapticEvent.RESET
                    SleeperUiState()
                }
            }
        }
    }

    /** Called when Volume Down is pressed. */
    fun onVolumeDown() {
        _uiState.update { state ->
            when (state.phase) {
                InputPhase.VALUE_ENTRY -> {
                    if (state.valueCount == 0) {
                        // No value entered yet — ignore
                        state
                    } else {
                        // Transition to suit entry
                        _hapticEvent.value = HapticEvent.SUIT_MODE
                        state.copy(phase = InputPhase.SUIT_ENTRY, suitCount = 1)
                    }
                }
                InputPhase.SUIT_ENTRY -> {
                    val newCount = if (state.suitCount >= 4) 1 else state.suitCount + 1
                    state.copy(suitCount = newCount)
                }
                InputPhase.DISPLAY -> {
                    // Ignore volume down during display
                    state
                }
            }
        }
    }

    /** Called when the card is dismissed by swipe gesture. */
    fun onDismiss() {
        _hapticEvent.value = HapticEvent.RESET
        _uiState.value = SleeperUiState()
    }

    /** Emergency reset — two-finger long press. */
    fun onEmergencyReset() {
        _hapticEvent.value = HapticEvent.RESET
        _uiState.value = SleeperUiState()
    }

    /** Updates the card transform during gesture manipulation. */
    fun updateCardTransform(
        panDelta: Offset = Offset.Zero,
        zoomDelta: Float = 1f,
        rotationDelta: Float = 0f
    ) {
        _uiState.update { state ->
            val t = state.cardTransform
            state.copy(
                cardTransform = t.copy(
                    offset = t.offset + panDelta,
                    scale = (t.scale * zoomDelta).coerceIn(0.3f, 5f),
                    rotation = t.rotation + rotationDelta
                )
            )
        }
    }
}
