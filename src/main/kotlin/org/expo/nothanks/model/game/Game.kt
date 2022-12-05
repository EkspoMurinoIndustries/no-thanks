package org.expo.nothanks.model.game

import java.util.*

data class Game(
    val playerCount: Int,
    var currentPlayer: Player,
    var currentCardCoins: Int = 0,
    val deck: Deck,
    val id: UUID,
    val inviteCode: String,
    var status: GameStatus = GameStatus.STARTED,
)