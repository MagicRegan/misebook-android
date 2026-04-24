package com.sleeper.app.model

/** Standard playing card ranks. */
enum class Rank(val display: String, val symbol: String, val value: Int) {
    ACE("A", "A", 1),
    TWO("2", "2", 2),
    THREE("3", "3", 3),
    FOUR("4", "4", 4),
    FIVE("5", "5", 5),
    SIX("6", "6", 6),
    SEVEN("7", "7", 7),
    EIGHT("8", "8", 8),
    NINE("9", "9", 9),
    TEN("10", "10", 10),
    JACK("J", "J", 11),
    QUEEN("Q", "Q", 12),
    KING("K", "K", 13);

    companion object {
        fun fromCount(count: Int): Rank? = entries.firstOrNull { it.value == count }
    }
}

/** Standard playing card suits. */
enum class Suit(val symbol: String, val suitValue: Int, val isRed: Boolean) {
    SPADES("\u2660", 1, false),
    HEARTS("\u2665", 2, true),
    CLUBS("\u2663", 3, false),
    DIAMONDS("\u2666", 4, true);

    companion object {
        fun fromCount(count: Int): Suit? = entries.firstOrNull { it.suitValue == count }
    }
}

/** A single playing card defined by its rank and suit. */
data class PlayingCard(val rank: Rank, val suit: Suit) {
    val displayName: String get() = "${rank.display}${suit.symbol}"
}
