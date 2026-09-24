package org.expo.nothanks.model.lobby

import java.util.*

data class LobbyPlayer(
    val id: UUID,
    val number: Int,
    var name: String,
    val score: MutableList<Int> = mutableListOf(),
    var avatar: String? = null
)
