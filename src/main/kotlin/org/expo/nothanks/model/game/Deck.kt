package org.expo.nothanks.model.game

data class Deck(
    val cards: List<Int>,
    val removedCards: List<Int> = emptyList(),
    var skip: Int = 0
)
