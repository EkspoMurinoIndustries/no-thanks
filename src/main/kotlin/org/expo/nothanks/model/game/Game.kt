package org.expo.nothanks.model.game

import java.time.Instant
import java.util.*

data class Game(
    val active: Boolean = true,
    val creator: UUID,
    var currentPlayer: UUID?,
    var currentCardCoins: Int = 0,
    val deck: Deck,
    val id: UUID,
    val inviteCode: String,
    val params: Params = Params(),
    val players: MutableList<Player>,
    var round: Int = 0,
    var status: GameStatus = GameStatus.CREATED,
    val lastUpdate: Instant = Instant.now()
)