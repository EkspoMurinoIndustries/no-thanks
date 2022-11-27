package org.expo.nothanks.service

import org.expo.nothanks.exception.GameHasNotBeenFound
import org.expo.nothanks.model.CreateGameRequest
import org.expo.nothanks.model.game.*
import org.expo.nothanks.utils.*
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


private const val defaultCoinsCount = 9

@Service
class GamesService {

    private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()
    private val writeLock: Lock = readWriteLock.writeLock()
    private val readLock: Lock = readWriteLock.readLock()
    private val activeGames: HashMap<UUID, Game> = HashMap()

    private val charPool = ('A'..'Z')

    fun addGame(createGameRequest: CreateGameRequest, creator: UUID, creatorName: String): Game {
        val inviteCode = (1..5)
            .map { charPool.random() }
            .joinToString("")
        val id = UUID.randomUUID()
        val deck = createDeck()
        val game = Game(
            creator = creator,
            currentPlayer = null,
            id = id,
            inviteCode = inviteCode,
            deck = deck,
            players = mutableListOf(createPlayer(creator, creatorName))
        )
        writeLock.lock()
        try {
            activeGames[id] = game
        } finally {
            writeLock.unlock()
        }
        return game
    }

    fun getGameIdByInviteCode(inviteCode: String): UUID? {
        return activeGames.values
            .find { it.inviteCode == inviteCode }
            ?.id
    }

    fun connect(gameId: UUID, playerId: UUID, userName: String) {
        val game = getGame(gameId)
        if (!game.players.any { it.id == playerId}) {
            game.players.add(createPlayer(playerId, userName))
        }
    }

    fun getPlayersNames(gameId: UUID): List<String> {
        val game = getGame(gameId)
        return game.players.map { it.name }
    }

    fun isCreator(gameId: UUID, playerId: UUID): Boolean {
        val game = getGame(gameId)
        return game.creator == playerId
    }

    fun startNewRound(gameId: UUID, playerId: UUID): Game {
        val game = getGame(gameId)
        writeLock.lock()
        if (game.creator != playerId) {
            throw IllegalStateException("Player is not creator")
        }
        try {
            if (game.canBeStarted()) {
                game.startGame(defaultCoinsCount)
                return game
            } else {
                throw IllegalStateException("Game has been already started")
            }
        } finally {
            writeLock.unlock()
        }
    }

    fun putCoin(gameId: UUID, playerId: UUID): Game {
        val game = getGame(gameId)
        writeLock.lock()
        try {
            if (game.isStarted()) {
                if (game.isPLayerCurrent(playerId)) {
                    throw IllegalStateException("Player is not current")
                }
                if (!game.putCoin()) {
                    throw IllegalStateException("Player does not have enough coins")
                }
            }
        } finally {
            writeLock.unlock()
        }
        return game
    }

    fun takeCard(gameId: UUID, playerId: UUID): Game {
        val game = getGame(gameId)
        writeLock.lock()
        try {
            if (game.isStarted()) {
                if (!game.isPLayerCurrent(playerId)) {
                    throw IllegalStateException("Player is not current")
                }
                game.takeCard()
            }
        } finally {
            writeLock.unlock()
        }
        return game
    }

    fun getGame(gameId: UUID): Game {
        readLock.lock()
        try {
            return activeGames[gameId] ?: throw GameHasNotBeenFound(gameId)
        } finally {
            readLock.unlock()
        }
    }

    private fun createDeck(): Deck {
        return Deck(
            cards = (3..35).toMutableList().shuffled().subList(0, 28)
        )
    }

    private fun createPlayer(id: UUID, name: String): Player {
        return Player(
            id = id,
            coins = defaultCoinsCount,
            name = name
        )
    }

}