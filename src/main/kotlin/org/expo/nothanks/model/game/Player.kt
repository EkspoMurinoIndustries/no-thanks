package org.expo.nothanks.model.game

import java.util.*

data class Player(
    val name: String = "Player",
    val cards: List<Int> = emptyList(),
    val coins: Int,
    val id: UUID,
    val score: Int = 0,
    val status: String = "LOBBY"
)