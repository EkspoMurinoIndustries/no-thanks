package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.config.properties.GameLimitsProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.exception.GameHasNotBeenFound
import org.expo.nothanks.exception.InviteHasNotBeenFound
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.model.event.output.PlayerLeftMessage
import org.expo.nothanks.model.event.output.SafeLobbyPlayer
import org.expo.nothanks.model.lobby.GameParams
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.utils.*
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import kotlin.concurrent.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Service
class GamesService(
    private val gameProperties: DefaultGameProperties,
    @Value("\${no-thanks.reconnect-grace-ms:300000}") private val reconnectGraceMillis: Long,
    private val gameLimits: GameLimitsProperties
) {
    init {
        require(gameProperties.maxCard in gameLimits.minBiggestCard..gameLimits.maxCard &&
            gameProperties.minCard in gameLimits.minCard..gameProperties.maxCard) {
            "Default card range must fit within game-limits"
        }
        require(gameProperties.removedCards in 0 until (gameProperties.maxCard - gameProperties.minCard + 1)) {
            "Default removed-cards must leave at least one card"
        }
        require((gameProperties.minPlayerNumber..gameProperties.maxPlayerNumber).all {
            gameProperties.coinsMap[it] in 0..gameLimits.maxTokens
        }) { "Default coins-map must fit within game-limits.max-tokens" }
    }

    private class LobbyEntry(val lobby: Lobby, val lock: ReentrantLock = ReentrantLock())

    private val creationLock = ReentrantLock()
    private val gameIdToLobby = ConcurrentHashMap<UUID, LobbyEntry>()
    private val inviteCodeToLobby = ConcurrentHashMap<String, Lobby>()
    private val lobbyByUserId = ConcurrentHashMap<UUID, Lobby>()
    private val sessionByUserId = ConcurrentHashMap<UUID, String>()
    private val disconnectDeadline = ConcurrentHashMap<UUID, Long>()

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
        creationLock.lock()
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
            gameIdToLobby[lobby.gameId] = LobbyEntry(lobby)
            inviteCodeToLobby[lobby.inviteCode] = lobby
            return lobby
        } finally {
            creationLock.unlock()
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
            val newPlayer = !lobby.playerAlreadyInGame(playerId)
            if (newPlayer) {
                if (lobby.isGameStarted()) throw GameException("Wait until the round ends to join", gameId)
                if (lobby.players.size >= gameProperties.maxPlayerNumber) throw GameException("Lobby is full", gameId)
            }
            // Atomic across lobbies: one player cannot claim two seats concurrently.
            val existingLobby = lobbyByUserId.putIfAbsent(playerId, lobby)
            if (existingLobby != null && existingLobby !== lobby) {
                throw GameException("You are already in another lobby", gameId)
            }
            if (newPlayer) lobby.addPlayer(playerId, name) else lobby.connectPlayer(playerId)
            disconnectDeadline.remove(playerId)
            if (sessionId != null) sessionByUserId[playerId] = sessionId
            operation.invoke(lobby, newPlayer)
        }
    }

    fun disconnectPlayerFromLobby(playerId: UUID, sessionId: String? = null, operation: (Lobby, SafeLobbyPlayer) -> (Unit)) {
        val lobby = lobbyByUserId[playerId] ?: return
        changeGameWithLock(lobby.gameId, ignoreMissing = true) { current ->
            if (lobbyByUserId[playerId] !== current) return@changeGameWithLock
            // Ignore duplicate events and late closes from an older connection.
            if (sessionId != null && sessionByUserId[playerId] != sessionId) return@changeGameWithLock
            if (current.canBeReconnected(playerId)) return@changeGameWithLock
            val disconnectedPlayer = current.getPlayerInLobby(playerId)
            current.disconnectPlayer(playerId)
            sessionByUserId.remove(playerId)
            disconnectDeadline[playerId] = System.currentTimeMillis() + reconnectGraceMillis
            operation.invoke(current, disconnectedPlayer)
        }
    }

    fun removeExpiredLobbies(now: Long = System.currentTimeMillis()): List<UUID> {
        val closed = mutableListOf<UUID>()
        for (gameId in gameIdToLobby.keys.toList()) {
            changeGameWithLock(gameId, ignoreMissing = true) { lobby ->
                val deadline = disconnectDeadline[lobby.creator]
                if (deadline != null && now >= deadline) {
                    deleteLobby(lobby)
                    closed.add(gameId)
                }
            }
        }
        return closed
    }

    fun removeExpiredPlayers(now: Long = System.currentTimeMillis()): List<Pair<UUID, PlayerLeftMessage>> {
        val departed = mutableListOf<Pair<UUID, PlayerLeftMessage>>()
        for (gameId in gameIdToLobby.keys.toList()) {
            changeGameWithLock(gameId, ignoreMissing = true) { lobby ->
                // Preserve active-round participants and scores; only pre-game guests expire.
                if (lobby.isGameStarted()) return@changeGameWithLock
                for (playerId in lobby.disconnectedPLayers.toList()) {
                    if (playerId == lobby.creator) continue
                    val deadline = disconnectDeadline[playerId] ?: continue
                    if (now < deadline || lobby.players.getValue(playerId).score.isNotEmpty()) continue
                    val player = lobby.getPlayerInLobby(playerId)
                    lobby.removePlayer(playerId)
                    lobby.disconnectedPLayers.remove(playerId)
                    removePlayerIndexes(lobby, playerId)
                    departed.add(gameId to PlayerLeftMessage(false, player, lobby.players.size))
                }
            }
        }
        return departed
    }

    // Release membership last so a new lobby can safely claim the player.
    private fun removePlayerIndexes(lobby: Lobby, playerId: UUID) {
        if (lobbyByUserId[playerId] === lobby) {
            sessionByUserId.remove(playerId)
            disconnectDeadline.remove(playerId)
            lobbyByUserId.remove(playerId, lobby)
        }
    }

    private fun deleteLobby(lobby: Lobby) {
        gameIdToLobby.remove(lobby.gameId)
        inviteCodeToLobby.remove(lobby.inviteCode, lobby)
        lobby.players.keys.forEach { removePlayerIndexes(lobby, it) }
    }

    fun gameIdByInviteCode(inviteCode: String): UUID =
        inviteCodeToLobby[inviteCode]?.gameId ?: throw InviteHasNotBeenFound(inviteCode)

    fun gameIdByPlayerId(playerId: UUID): UUID =
        lobbyByUserId[playerId]?.gameId ?: throw IllegalStateException()

    fun changeGameWithLock(gameId: UUID, ignoreMissing: Boolean = false, operation: (Lobby) -> Unit) {
        val entry = gameIdToLobby[gameId]
        if (entry == null) {
            if (ignoreMissing) return
            throw GameHasNotBeenFound(gameId)
        }
        entry.lock.withLock {
            // A waiting operation must not mutate a lobby removed by cleanup.
            if (gameIdToLobby[gameId] !== entry) {
                if (ignoreMissing) return
                throw GameHasNotBeenFound(gameId)
            }
            operation(entry.lobby)
        }
    }

    private fun getDefaultGameParams() : GameParams = GameParams(
        initialCoinsCount = gameProperties.coinsMap.getValue(gameProperties.minPlayerNumber),
        minCard = gameProperties.minCard,
        maxCard = gameProperties.maxCard,
        removedCards = gameProperties.removedCards,
        maxPlayerNumber = gameProperties.maxPlayerNumber,
        useDefaultTokens = true,
        limits = gameLimits
    )
}
