package org.expo.nothanks.model.event.output

import org.expo.nothanks.model.lobby.Params
import java.util.*

data class UserConnectedMessage(
    val isStarted: Boolean,
    val gameId: UUID,
    val players: List<SafeLobbyPlayer>,
    val isCreator: Boolean = false,
    val playerNumber: Int,
    val gameStatus: GameStatus? = null,
    val params: Params,
    val inviteCode: String
) : OutputMessage

data class SafeLobbyPlayer(
    val number: Int,
    val name: String,
    val score: MutableList<Int> = mutableListOf()
)

data class GameStatus(
    val coins: Int,
    val cards: Set<Int>,
    val currentCard: Int,
    val currentCardCoin: Int,
    val isCurrentPlayer: Boolean,
    val remainingNumberCards: Int,
    val players: List<SafeGamePlayer>,
    val currentPlayerNumber: Int
)