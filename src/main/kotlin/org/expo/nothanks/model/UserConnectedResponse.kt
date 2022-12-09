package org.expo.nothanks.model

import java.util.*

data class UserConnectedResponse(
    val isStarted: Boolean,
    val gameId: UUID,
    val allPlayers: List<SafeLobbyPlayer>,
    val isCreator: Boolean = false,
    val playerNumber: Int,
    val gameStatus: GameStatus? = null
)

data class SafeLobbyPlayer(
    val number: Int,
    val name: String,
    val score: MutableList<Int> = mutableListOf()
)

data class GameStatus (
    val coins: Int,
    val cards: Set<Int>,
    val currentCard: Int,
    val currentCardCoin: Int,
    val isCurrentPlayer: Boolean,
    val cardsByPlayer: Map<Int, Set<Int>>
)