package org.expo.nothanks.utils

import org.expo.nothanks.exception.GameException
import org.expo.nothanks.exception.PlayerException
import org.expo.nothanks.model.event.output.GameStatus
import org.expo.nothanks.model.event.output.SafeLobbyPlayer
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.model.event.output.SafeGamePlayer
import org.expo.nothanks.model.event.output.Score
import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.Player
import org.expo.nothanks.model.lobby.GameParams
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.model.lobby.LobbyPlayer
import java.util.*

fun Lobby.removePlayer(playerId: UUID) {
    this.players.remove(playerId)
}

fun Lobby.connectPlayer(playerId: UUID): Boolean {
    if (disconnectedPLayers.contains(playerId)) {
        disconnectedPLayers.remove(playerId)
        return true
    } else if (!players.values.any { it.id == playerId }) {
        throw PlayerException("Impossible to connect player to this game", gameId, playerId)
    }
    return false
}

fun Lobby.playerAlreadyInGame(playerId: UUID): Boolean {
    return this.players.containsKey(playerId)
}

fun Lobby.addPlayer(playerId: UUID, name: String): Boolean {
    if (!this.players.containsKey(playerId)) {
        if (players.size >= params.maxPlayerNumber) {
            throw PlayerException("Lobby is filled", gameId, playerId)
        }
        this.players[playerId] = LobbyPlayer(
            id = playerId,
            number = this.players.values.maxOfOrNull { it.number }?.plus(1) ?: 0,
            name = name,
        )
        return true
    }
    return false
}

fun Lobby.setNotActive() {
    active = false
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
            remainingNumberCards = game.remainingNumberCards(),
            players = getPlayersInGame(),
            currentPlayerNumber = game.currentPlayerNumber()
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
        throw PlayerException("Impossible to disconnect player from this game", gameId, playerId)
    }
    if (isGameStarted() || getPlayerInLobby(playerId).score.isNotEmpty()) {
        disconnectedPLayers.add(playerId)
    } else {
        removePlayer(playerId)
    }
}

fun Lobby.canBeReconnected(playerId: UUID): Boolean {
    return disconnectedPLayers.contains(playerId)
}

fun Lobby.isGameStarted(): Boolean = game != null

fun Lobby.startGame(gameParams: GameParams) {
    if (players.size < 2) {
        throw GameException("Sorry, you can't play alone", gameId)
    }
    game = createGame(gameParams)
}

fun Lobby.getGame(): Game {
    if (game == null) {
        throw GameException("Game has not been started", gameId)
    }
    return game!!
}

fun Lobby.getPlayersInGame(): List<SafeGamePlayer> {
    return getGame().playerSequence().map {
        it.toSafeGamePlayer(players[it.id]!!.name)
    }.toList()
}

fun Player.toSafeGamePlayer(name: String): SafeGamePlayer {
    return SafeGamePlayer(
        name = name,
        number = this.number,
        cards = this.cards,
        coins = this.coins
    )
}

fun Lobby.getPlayerInLobby(playerId: UUID): SafeLobbyPlayer {
    return players[playerId]?.let {
        SafeLobbyPlayer(
            name = it.name,
            number = it.number,
            score = it.score
        )
    } ?: throw PlayerException("Player has not been found", gameId, playerId)
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
        throw GameException("Game has not been started", gameId)
    }
    val result = game!!.calculateResult()
    result.forEach { (playerId, playerResult) ->
        players[playerId]!!.score.add(playerResult.score)
    }
    game = null
}

fun Lobby.createGame(gameParams: GameParams): Game {
    val gamePlayers = players.values.map {
        Player(
            id = it.id,
            number = it.number,
            coins = gameParams.coinsMap[players.size] ?: gameParams.defaultCoinsCount
        )
    }.shuffled().toList()
    for (i: Int in 1 until gamePlayers.size) {
        gamePlayers[i - 1].nextPlayer = gamePlayers[i]
    }
    gamePlayers.last().nextPlayer = gamePlayers[0]
    return Game(
        currentPlayer = gamePlayers[0],
        id = this.gameId,
        inviteCode = inviteCode,
        deck = createDeck(this.params),
        playerCount = gamePlayers.size
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

fun Lobby.getPlayer(playerId: UUID): LobbyPlayer {
    return players[playerId] ?: throw PlayerException("Player has not been found", gameId, playerId)
}

fun Lobby.changePlayerName(playerId: UUID, newName: String) {
    getPlayer(playerId).name = newName
}