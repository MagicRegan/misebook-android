package com.sleeper.app.ui.card

import com.sleeper.app.model.Rank

/**
 * Defines the standard pip positions for each numeric rank on a poker card.
 * Positions are normalized to [0,1] where (0,0) is top-left of the pip area.
 * `inverted` means the pip should be drawn upside-down (bottom half of card).
 */
data class PipPosition(val x: Float, val y: Float, val inverted: Boolean = false)

fun getPipPositions(rank: Rank): List<PipPosition> = when (rank) {
    Rank.ACE -> listOf(
        PipPosition(0.5f, 0.5f)
    )
    Rank.TWO -> listOf(
        PipPosition(0.5f, 0.15f),
        PipPosition(0.5f, 0.85f, inverted = true)
    )
    Rank.THREE -> listOf(
        PipPosition(0.5f, 0.15f),
        PipPosition(0.5f, 0.5f),
        PipPosition(0.5f, 0.85f, inverted = true)
    )
    Rank.FOUR -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.FIVE -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.5f, 0.5f),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.SIX -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.3f, 0.5f),
        PipPosition(0.7f, 0.5f),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.SEVEN -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.5f, 0.325f),
        PipPosition(0.3f, 0.5f),
        PipPosition(0.7f, 0.5f),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.EIGHT -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.5f, 0.325f),
        PipPosition(0.3f, 0.5f),
        PipPosition(0.7f, 0.5f),
        PipPosition(0.5f, 0.675f, inverted = true),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.NINE -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.3f, 0.38f),
        PipPosition(0.7f, 0.38f),
        PipPosition(0.5f, 0.5f),
        PipPosition(0.3f, 0.62f, inverted = true),
        PipPosition(0.7f, 0.62f, inverted = true),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    Rank.TEN -> listOf(
        PipPosition(0.3f, 0.15f),
        PipPosition(0.7f, 0.15f),
        PipPosition(0.5f, 0.27f),
        PipPosition(0.3f, 0.38f),
        PipPosition(0.7f, 0.38f),
        PipPosition(0.3f, 0.62f, inverted = true),
        PipPosition(0.7f, 0.62f, inverted = true),
        PipPosition(0.5f, 0.73f, inverted = true),
        PipPosition(0.3f, 0.85f, inverted = true),
        PipPosition(0.7f, 0.85f, inverted = true)
    )
    // Face cards use a single centered large symbol
    Rank.JACK, Rank.QUEEN, Rank.KING -> emptyList()
}
