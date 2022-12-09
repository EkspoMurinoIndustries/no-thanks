package org.expo.nothanks.service

import org.expo.nothanks.exception.GameHasNotBeenFound
import org.expo.nothanks.exception.InviteHasNotBeenFound
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.utils.*
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class GamesService {

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()
    private val writeLock: Lock = readWriteLock.writeLock()
    private val readLock: Lock = readWriteLock.readLock()

    private val gameIdToLobby: MutableMap<UUID, Lobby> = mutableMapOf()
    private val inviteCodeToLobby: MutableMap<String, Lobby> = mutableMapOf()
    private val lobbyByUserId: MutableMap<UUID, Lobby> = mutableMapOf()

    fun putCoin(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().putCoin(playerId)
            operation.invoke(lobby)
        }
    }

    fun takeCard(gameId: UUID, playerId: UUID, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().takeCard(playerId)
            if (lobby.getGame().isRoundEnded()) {
                lobby.finishRound()
            }
            operation.invoke(lobby)
        }
    }

    fun createLobby(creator: UUID): Lobby {
        writeLock.lock()
        try {
            var inviteCode = createInviteCode()
            //Check on duplicates
            while (inviteCodeToLobby.containsKey(inviteCode)) {
                inviteCode = createInviteCode()
            }
            val lobby = Lobby(
                creator = creator,
                inviteCode = inviteCode,
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
            it.startGame()
            operation.invoke(it)
        }
    }

    fun changeParams(gameId: UUID, playerId: UUID, params: NewParams, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) {
            checkOnLobbyChange(it, playerId)
            it.updateParams(params)
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

    private fun checkOnLobbyChange(lobby: Lobby, playerId: UUID) {
        if (lobby.isGameStarted()) {
            throw IllegalStateException("Game has been already started")
        }
        if (lobby.creator != playerId) {
            throw IllegalStateException("Player is not creator")
        }
    }

    fun addPlayerToLobby(gameId: UUID, playerId: UUID, name: String, operation: (Lobby) -> (Unit)) {
        changeGameWithLock(gameId) { lobby ->
            if (lobby.isGameStarted()) {
                lobby.connectPlayer(playerId)
            } else {
                lobby.addPlayer(playerId, name)
            }
            lobbyByUserId[playerId] = lobby
            operation.invoke(lobby)
        }
    }

    fun disconnectPlayerFromLobby(playerId: UUID) {
        val gameId = gameIdByPlayerId(playerId)
        changeGameWithLock(gameId) { lobby ->
            lobby.disconnectPlayer(playerId)
            if (lobby.isGameStarted()) {
                lobbyByUserId[playerId] = lobby
            } else {
                lobbyByUserId.remove(playerId)
                if (lobby.creator == playerId) {
                    deleteLobby(lobby)
                }
            }
            if (lobby.shouldBeDeleted()) {
                deleteLobby(lobby)
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
            }
        }
    }

    fun <T> readGameWithLock(gameId: UUID, operation: (Lobby) -> T): T {
        readLock.lock()
        try {
            val game = gameIdToLobby[gameId] ?: throw GameHasNotBeenFound(gameId)
            return operation.invoke(game)
        } finally {
            readLock.unlock()
        }
    }

    fun lobbyExist(gameId: UUID): Boolean {
        readLock.lock()
        try {
            return gameIdToLobby.containsKey(gameId)
        } finally {
            readLock.unlock()
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

}