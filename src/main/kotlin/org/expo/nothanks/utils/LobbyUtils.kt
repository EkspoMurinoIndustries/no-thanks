package org.expo.nothanks.utils

import org.expo.nothanks.model.GameStatus
import org.expo.nothanks.model.SafeLobbyPlayer
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.model.event.output.SafeGamePLayer
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

fun Lobby.shouldBeDeleted(): Boolean {
    return if (isGameStarted()) {
        getGame().playerCount == disconnectedPLayers.size
    } else {
        players.isEmpty()
    }
}

fun Lobby.gameStatusOrNull(playerId: UUID): GameStatus? {
    return if (isGameStarted()) {
        val game = getGame()
        val player = game.getPlayer(playerId)
        GameStatus(
            coins = player.coins,
            cards = player.cards,
            currentCard = game.currentCard(),
            currentCardCoin = game.currentCardCoins,
            isCurrentPlayer = game.isPlayerCurrent(playerId),
            cardsByPlayer = game.playerSequence().associate { it.number to it.cards }
        )
    } else {
        null
    }
}

fun Lobby.getSafeLobbyPlayers(): List<SafeLobbyPlayer> {
    return players.values.map {
        SafeLobbyPlayer(
            number = it.number,
            name = it.name,
            score = it.score
        )
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

fun Lobby.getPlayersInGame(): List<SafeGamePLayer> {
    return getGame().playerSequence().map {
        SafeGamePLayer(
            name = players[it.id]!!.name,
            number = it.number,
            cards = it.cards,
            coins = it.coins
        )
    }.toList()
}

fun Lobby.getPlayerInLobby(playerId: UUID): SafeLobbyPlayer {
    return players[playerId]?.let {
        SafeLobbyPlayer(
            name = it.name,
            number = it.number,
            score = it.score
        )
    } ?: throw IllegalStateException("Player has not been found")
}

fun Lobby.getPlayersInLobby(): List<SafeLobbyPlayer> {
    return players.values.map {
        SafeLobbyPlayer(
            name = it.name,
            number = it.number,
            score = it.score
        )
    }.toList()
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
            number = this.players.values.maxOfOrNull { it.number }?.plus(1) ?: 0,
            name = name,
        )
    }
}

fun Lobby.createGame(): Game {
    val players = this.players.values.map {
        Player(
            id = it.id,
            number = it.number,
            coins = params.defaultCoinsCount
        )
    }.shuffled().toList()
    for (i: Int in 1 until players.size) {
        players[i - 1].nextPlayer = players[i]
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

fun Lobby.updateParams(newParams: NewParams) {
    params.maxCard = newParams.maxCard ?: params.maxCard
    params.minCard = newParams.minCard ?: params.minCard
    params.defaultCoinsCount = newParams.defaultCoinsCount ?: params.defaultCoinsCount
    params.extraCards = newParams.extraCards ?: params.extraCards
}

fun Lobby.reset() {
    players.values.forEach {
        it.score.clear()
    }
}