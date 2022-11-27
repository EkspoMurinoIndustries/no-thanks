package org.expo.nothanks.model.game

import java.util.*

data class Player(
    val name: String = "Player",
    val cards: MutableSet<Int> = mutableSetOf(),
    var coins: Int,
    val id: UUID,
    val score: MutableList<Int> = mutableListOf(),
    val status: String = "LOBBY"
)