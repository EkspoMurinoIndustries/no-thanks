package org.expo.nothanks.model.game

import java.time.Instant
import java.util.*

data class Game(
    val active: Boolean = true,
    val creator: UUID,
    val currentPlayer: UUID?,
    val currentCard: CurrentCard?,
    val deck: Deck,
    val id: UUID,
    val inviteCode: String,
    val params: Params = Params(),
    val players: MutableList<Player>,
    val round: Int = 0,
    val status: String = "ACTIVE",
    val lastUpdate: Instant = Instant.now()
)