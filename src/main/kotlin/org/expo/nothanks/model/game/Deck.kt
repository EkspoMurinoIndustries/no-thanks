package org.expo.nothanks.model.game

data class Deck(
    val cards: List<Int>,
    var skip: Int = 0
)