package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.exception.GameHasNotBeenFound
import org.expo.nothanks.exception.InviteHasNotBeenFound
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.model.event.output.SafeLobbyPlayer
import org.expo.nothanks.model.lobby.GameParams
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.utils.*
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import kotlin.concurrent.withLock
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class GamesService(
    private val gameProperties: DefaultGameProperties,
    @Value("\${no-thanks.reconnect-grace-ms:300000}") private val reconnectGraceMillis: Long = 300000
) {

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()
    private val writeLock: Lock = readWriteLock.writeLock()
    private val readLock: Lock = readWriteLock.readLock()

    private val gameIdToLobby: MutableMap<UUID, Lobby> = mutableMapOf()
    private val inviteCodeToLobby: MutableMap<String, Lobby> = mutableMapOf()
    private val lobbyByUserId: MutableMap<UUID, Lobby> = mutableMapOf()
    private val sessionByUserId: MutableMap<UUID, String> = mutableMapOf()
    private val disconnectDeadline: MutableMap<UUID, Long> = mutableMapOf()

    fun putCoin(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().putCoin(playerId)
            operation.invoke(lobby)
        }
    }

    fun takeCard(gameId: UUID, playerId: UUID, operation: (Lobby, List<Int>?) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().takeCard(playerId)
            val removedCards = if (lobby.getGame().isRoundEnded()) {
                lobby.finishRound()
            } else {
                null
            }
            operation.invoke(lobby, removedCards)
        }
    }

    fun createLobby(creator: UUID): Lobby {
        writeLock.lock()
        try {
            lobbyByUserId[creator]?.let { return it }
            var inviteCode = createInviteCode()
            //Check on duplicates
            while (inviteCodeToLobby.containsKey(inviteCode)) {
                inviteCode = createInviteCode()
            }
            val lobby = Lobby(
                creator = creator,
                inviteCode = inviteCode,
                params = getDefaultGameParams()
            )
            gameIdToLobby[lobby.gameId] = lobby
            inviteCodeToLobby[lobby.inviteCode] = lobby
            return lobby
        } finally {
            writeLock.unlock()
        }
    }

    fun startNewRound(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) {
            checkOnLobbyChange(it, playerId)
            if (it.disconnectedPLayers.isNotEmpty()) {
                throw GameException("Wait for disconnected players to return before starting a round", gameId)
            }
            if (it.players.size < gameProperties.minPlayerNumber) {
                throw GameException("Sorry, minimum number of players is ${gameProperties.minPlayerNumber}", gameId)
            }
            if (it.params.useDefaultTokens) {
                it.params.initialCoinsCount = gameProperties.coinsMap.getValue(it.players.size)
            }
            it.createNewGame()
            operation.invoke(it)
        }
    }

    fun changeParams(gameId: UUID, playerId: UUID, params: NewParams, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) {
            checkOnLobbyChange(it, playerId)
            if (params.resetToDefaults) {
                val defaults = getDefaultGameParams()
                it.updateParams(NewParams(
                    minCard = defaults.minCard,
                    maxCard = defaults.maxCard,
                    removedCards = defaults.removedCards,
                    defaultCoinsCount = defaults.initialCoinsCount,
                    useDefaultTokens = true
                ))
            } else {
                it.updateParams(params)
            }
            operation.invoke(it)
        }
    }

    fun resetHistory(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) {
            checkOnLobbyChange(it, playerId)
            it.reset()
            operation.invoke(it)
        }
    }

    fun abortRound(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) {
            if (it.creator != playerId) {
                throw GameException("Only the host can end the round", gameId)
            }
            if (!it.isGameStarted()) {
                throw GameException("Game has not been started", gameId)
            }
            it.discardCurrentRound()
            operation.invoke(it)
        }
    }

    private fun checkOnLobbyChange(lobby: Lobby, playerId: UUID) {
        if (lobby.isGameStarted()) {
            throw GameException("Settings and lobby controls are unavailable during a round", lobby.gameId)
        }
        if (lobby.creator != playerId) {
            throw GameException("Only the host can change the lobby", lobby.gameId)
        }
    }

    fun addPlayerToLobby(gameId: UUID, playerId: UUID, name: String, sessionId: String? = null, operation: (Lobby, Boolean) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            val previousLobby = lobbyByUserId[playerId]
            if (previousLobby != null && previousLobby !== lobby) {
                throw GameException("You are already in another lobby", gameId)
            }
            val newPlayer = !lobby.playerAlreadyInGame(playerId)
            if (!newPlayer) {
                lobby.connectPlayer(playerId)
            } else {
                if (lobby.isGameStarted()) throw GameException("Wait until the round ends to join", gameId)
                if (lobby.players.size >= gameProperties.maxPlayerNumber) throw GameException("Lobby is full", gameId)
                lobby.addPlayer(playerId, name)
            }
            lobbyByUserId[playerId] = lobby
            disconnectDeadline.remove(playerId)
            if (sessionId != null) sessionByUserId[playerId] = sessionId
            operation.invoke(lobby, newPlayer)
        }
    }

    fun disconnectPlayerFromLobby(playerId: UUID, sessionId: String? = null, operation: (Lobby, SafeLobbyPlayer) -> (Unit)) {
        writeLock.withLock {
            val lobby = lobbyByUserId[playerId] ?: return
            // Ignore duplicate events and late closes from an older connection.
            if (sessionId != null && sessionByUserId[playerId] != sessionId) return
            if (lobby.canBeReconnected(playerId)) return
            val disconnectedPlayer = lobby.getPlayerInLobby(playerId)
            lobby.disconnectPlayer(playerId)
            sessionByUserId.remove(playerId)
            disconnectDeadline[playerId] = System.currentTimeMillis() + reconnectGraceMillis
            operation.invoke(lobby, disconnectedPlayer)
        }
    }

    fun expireDisconnectedPlayers(
        now: Long = System.currentTimeMillis(),
        onClosed: (UUID) -> Unit,
        onPlayerLeft: (Lobby, SafeLobbyPlayer) -> Unit
    ) {
        writeLock.withLock {
            for (lobby in gameIdToLobby.values.toList()) {
                val hostDeadline = disconnectDeadline[lobby.creator]
                if (hostDeadline != null && now >= hostDeadline) {
                    lobby.setNotActive()
                    deleteLobby(lobby)
                    onClosed(lobby.gameId)
                    continue
                }
                // Preserve active-round participants and scores; a pre-game guest's seat can expire.
                if (!lobby.isGameStarted()) {
                    for (playerId in lobby.disconnectedPLayers.toList()) {
                        val deadline = disconnectDeadline[playerId] ?: continue
                        if (now < deadline || lobby.players[playerId]!!.score.isNotEmpty()) continue
                        val player = lobby.getPlayerInLobby(playerId)
                        lobby.removePlayer(playerId)
                        lobby.disconnectedPLayers.remove(playerId)
                        lobbyByUserId.remove(playerId)
                        disconnectDeadline.remove(playerId)
                        onPlayerLeft(lobby, player)
                    }
                }
            }
        }
    }

    private fun deleteLobby(lobby: Lobby) {
        gameIdToLobby.remove(lobby.gameId)
        inviteCodeToLobby.remove(lobby.inviteCode)
        lobby.players.keys.forEach {
            val lobbyOfPlayer = lobbyByUserId[it]
            if (lobbyOfPlayer != null && lobbyOfPlayer.gameId == lobby.gameId) {
                lobbyByUserId.remove(it)
                sessionByUserId.remove(it)
                disconnectDeadline.remove(it)
            }
        }
    }

    fun gameIdByInviteCode(inviteCode: String): UUID {
        readLock.lock()
        try {
            return inviteCodeToLobby[inviteCode]?.gameId ?: throw InviteHasNotBeenFound(inviteCode)
        } finally {
            readLock.unlock()
        }
    }

    fun gameIdByPlayerId(playerId: UUID): UUID {
        readLock.lock()
        try {
            return lobbyByUserId[playerId]?.gameId ?: throw IllegalStateException()
        } finally {
            readLock.unlock()
        }
    }

    fun changeGameWithLock(gameId: UUID, operation: (Lobby) -> Unit) {
        writeLock.lock()
        try {
            val lobby = gameIdToLobby[gameId] ?: throw GameHasNotBeenFound(gameId)
            operation.invoke(lobby)
        } finally {
            writeLock.unlock()
        }
    }

    private fun getDefaultGameParams() : GameParams = GameParams(
        initialCoinsCount = gameProperties.coinsMap.getValue(gameProperties.minPlayerNumber),
        minCard = gameProperties.minCard,
        maxCard = gameProperties.maxCard,
        removedCards = gameProperties.removedCards,
        maxPlayerNumber = gameProperties.maxPlayerNumber,
        useDefaultTokens = true
    )
}
