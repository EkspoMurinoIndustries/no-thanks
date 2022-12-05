package org.expo.nothanks.service

import org.expo.nothanks.exception.GameHasNotBeenFound
import org.expo.nothanks.exception.InviteHasNotBeenFound
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

    fun <T> putCoin(gameId: UUID, playerId: UUID, operation: (Lobby) -> T): T {
        var response: T? = null
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().putCoin(playerId)
            response = operation.invoke(lobby)
        }
        return response!!
    }

    fun <T> takeCard(gameId: UUID, playerId: UUID, operation: (Lobby) -> T): T {
        var response: T? = null
        changeGameWithLock(gameId) { lobby ->
            lobby.getGame().takeCard(playerId)
            if (lobby.getGame().isRoundEnded()) {
                lobby.finishRound()
            }
            response = operation.invoke(lobby)
        }
        return response!!
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

    fun startNewRound(gameId: UUID, playerId: UUID) {
        changeGameWithLock(gameId) {
            if (it.isGameStarted()) {
                throw IllegalStateException("Game has been already started")
            } else {
                it.startGame()
            }
        }
    }

    fun addPlayerToLobby(inviteCode: String, playerId: UUID, name: String) {
        val gameId = gameIdByInviteCode(inviteCode) ?: throw InviteHasNotBeenFound(inviteCode)
        changeGameWithLock(gameId) { lobby ->
            if (lobby.isGameStarted()) {
                lobby.connectPlayer(playerId)
            } else {
                lobby.addPlayer(playerId, name)
            }
            lobbyByUserId[playerId] = lobby
        }
    }

    fun disconnectPlayerFromLobby(playerId: UUID) {
        val gameId = gameIdByPlayerId(playerId) ?: throw IllegalStateException()
        changeGameWithLock(gameId) { lobby ->
            lobby.disconnectPlayer(playerId)
            lobbyByUserId[playerId] = lobby
        }
    }

    fun getPlayersNames(gameId: UUID): List<String> {
        return readGameWithLock(gameId) {
            it.playerNames()
        } ?: throw GameHasNotBeenFound(gameId)
    }

    fun <T> readGameWithLock(gameId: UUID, operation: (Lobby) -> T): T? {
        readLock.lock()
        try {
            val game = gameIdToLobby[gameId] ?: return null
            return operation.invoke(game)
        } finally {
            readLock.unlock()
        }
    }

    fun gameIdByInviteCode(inviteCode: String): UUID? {
        readLock.lock()
        try {
            return inviteCodeToLobby[inviteCode]?.gameId ?: return null
        } finally {
            readLock.unlock()
        }
    }

    fun gameIdByPlayerId(playerId: UUID): UUID? {
        readLock.lock()
        try {
            return lobbyByUserId[playerId]?.gameId
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