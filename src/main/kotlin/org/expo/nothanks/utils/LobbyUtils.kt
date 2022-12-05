package org.expo.nothanks.utils

import org.expo.nothanks.model.event.output.Score
import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.Player
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.model.lobby.LobbyPlayer
import java.util.*

fun Lobby.removePlayer(playerId: UUID) {
    this.players.remove(playerId)
}

fun Lobby.connectPlayer(playerId: UUID) {
    if (disconnectedPLayers.contains(playerId)) {
        disconnectedPLayers.remove(playerId)
        if (disconnectedPLayers.isEmpty() && !isGameStarted()) {
            game!!.continueGame()
        }
    } else if (!players.values.any { it.id == playerId }) {
        throw IllegalStateException("Impossible to connect player to this game")
    }
}

fun Lobby.disconnectPlayer(playerId: UUID) {
    if (!players.values.any { it.id == playerId }) {
        throw IllegalStateException("Impossible to disconnect player from this game")
    }

    if (isGameStarted()) {
        pause()
        disconnectedPLayers.add(playerId)
    } else {
        removePlayer(playerId)
    }
}

fun Lobby.isGameStarted(): Boolean = game != null

fun Lobby.pause() {
    getGame().pause()
}

fun Lobby.startGame() {
    game = createGame()
}

fun Lobby.getGame(): Game {
    if (game == null) {
        throw IllegalStateException("Game has not been started")
    }
    return game!!
}

fun Lobby.finishRound() {
    if (game == null) {
        throw IllegalStateException("Game has not been started")
    }
    val result = game!!.calculateResult()
    result.forEach { (playerId, playerResult) ->
        players[playerId]!!.score.add(playerResult.score)
    }
    game = null
}

fun Lobby.addPlayer(playerId: UUID, name: String) {
    if (!this.players.containsKey(playerId)) {
        this.players[playerId] = LobbyPlayer(
            id = playerId,
            number = this.players.values.maxOfOrNull{ it.number } ?: 0,
            name = name,
        )
    }
}

fun Lobby.playerNames(): List<String> {
    return this.players.values.map { it.name }
}

fun Lobby.createGame(): Game {
    val players = this.players.values.map {
        Player(
            id = it.id,
            coins = it.number,
            number = coins(params)
        )
    }.shuffled().toList()
    for (i: Int in 1 until players.size) {
        players[i-1].nextPlayer = players[i]
    }
    players.last().nextPlayer = players[0]
    return Game(
        currentPlayer = players[0],
        id = this.gameId,
        inviteCode = inviteCode,
        deck = createDeck(this.params),
        playerCount = players.size
    )
}

fun Lobby.getResult(): Map<Int, Score> {
    return players.values.associate {
        it.number to Score(
            playerName = it.name,
            rounds = it.score,
            totalScore = it.score.sum(),
            lastRoundScore = it.score.last()
        )
    }
}