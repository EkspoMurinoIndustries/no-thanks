package org.expo.nothanks.model.game

import java.util.*

data class Player(
    val number: Int,
    val cards: MutableSet<Int> = mutableSetOf(),
    var coins: Int,
    val id: UUID,
    var nextPlayer: Player? = null
)