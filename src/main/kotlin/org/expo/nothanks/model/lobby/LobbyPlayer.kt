package org.expo.nothanks.model.lobby

import java.util.*

data class LobbyPlayer(
    val id: UUID,
    val number: Int,
    val name: String,
    val score: MutableList<Int> = mutableListOf()
)
