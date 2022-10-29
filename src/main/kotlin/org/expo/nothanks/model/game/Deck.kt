package org.expo.nothanks.model.game

data class Deck(
    val cards: List<Int>,
    val skip: Int = 0
)