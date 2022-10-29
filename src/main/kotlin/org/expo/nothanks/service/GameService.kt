package org.expo.nothanks.service

import org.expo.nothanks.model.CreateGameRequest
import org.expo.nothanks.model.game.Deck
import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.Player
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class GameService {

    private val activeGames: ConcurrentHashMap<UUID, Game> = ConcurrentHashMap()

    private val charPool = ('A'..'Z')

    fun addGame(createGameRequest: CreateGameRequest, creator: UUID, creatorName: String): Game {
        val inviteCode = (1..5)
            .map { charPool.random() }
            .joinToString("")
        val id = UUID.randomUUID()
        val game = Game(
            creator = creator,
            currentPlayer = null,
            currentCard = null,
            id = id,
            inviteCode = inviteCode,
            deck = createDeck(),
            players = mutableListOf(createPlayer(creator, creatorName))
        )
        activeGames[id] = game
        return game
    }

    fun getGameIdByInviteCode(inviteCode: String): UUID? {
        return activeGames.values
            .find { it.inviteCode == inviteCode }
            ?.id
    }

    fun connect(gameId: UUID, playerId: UUID, userName: String): Boolean {
        val game = activeGames[gameId] ?: return false
        if (!game.players.any { it.id == playerId}) {
            game.players.add(createPlayer(playerId, userName))
        }
        return true
    }

    fun getPlayersNames(gameId: UUID): List<String> {
        val game = activeGames[gameId] ?: return emptyList()
        return game.players.map { it.name }
    }

    fun isCreator(gameId: UUID, playerId: UUID): Boolean {
        val game = activeGames[gameId] ?: return false
        return game.creator == playerId
    }

    private fun createDeck(): Deck {
        return Deck(
            cards = (3..35).toMutableList().shuffled().subList(0, 28)
        )
    }

    private fun createPlayer(id: UUID, name: String): Player {
        return Player(
            id = id,
            coins = 9,
            name = name
        )
    }

}