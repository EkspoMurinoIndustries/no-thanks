package org.expo.nothanks.utils

import org.expo.nothanks.model.lobby.GameParams
import org.expo.nothanks.model.lobby.Lobby
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class LobbyRoundUtilsTest {

    @Test
    fun `new games advance the round number`() {
        val lobby = lobbyWithTwoPlayers()

        lobby.createNewGame()
        assertEquals(1, lobby.round)
        assertNotNull(lobby.game)

        lobby.game = null
        lobby.createNewGame()
        assertEquals(2, lobby.round)
    }

    @Test
    fun `reset clears scores and makes the next game round one`() {
        val lobby = lobbyWithTwoPlayers()
        lobby.players.values.forEach { it.score.addAll(listOf(12, 8)) }
        lobby.round = 2

        lobby.reset()

        assertEquals(0, lobby.round)
        lobby.players.values.forEach { assertEquals(emptyList<Int>(), it.score) }

        lobby.createNewGame()
        assertEquals(1, lobby.round)
    }

    @Test
    fun `results are available before any round is complete`() {
        val lobby = lobbyWithTwoPlayers()

        val result = lobby.getResult()

        assertEquals(2, result.size)
        result.values.forEach {
            assertEquals(emptyList<Int>(), it.rounds)
            assertEquals(0, it.totalScore)
            assertEquals(0, it.lastRoundScore)
        }
    }

    @Test
    fun `deck records the cards omitted from the round`() {
        val lobby = lobbyWithTwoPlayers()

        lobby.createNewGame()
        val deck = lobby.getGame().deck

        assertEquals(1, deck.removedCards.size)
        assertEquals((3..5).toSet(), (deck.cards + deck.removedCards).toSet())
    }

    @Test
    fun `discarding a round preserves completed scores and reuses its number`() {
        val lobby = lobbyWithTwoPlayers()
        lobby.players.values.forEach { it.score.add(7) }
        lobby.round = 1
        lobby.createNewGame()
        lobby.getGame().currentPlayer.cards.add(35)

        lobby.discardCurrentRound()

        assertNull(lobby.game)
        assertEquals(1, lobby.round)
        lobby.players.values.forEach { assertEquals(listOf(7), it.score) }

        lobby.createNewGame()
        assertEquals(2, lobby.round)
    }

    private fun lobbyWithTwoPlayers(): Lobby {
        val creator = UUID.randomUUID()
        return Lobby(
            creator = creator,
            inviteCode = "ABCDE",
            params = GameParams(
                initialCoinsCount = 11,
                minCard = 3,
                maxCard = 5,
                extraCards = 1,
                maxPlayerNumber = 8
            )
        ).also {
            it.addPlayer(creator, "Creator")
            it.addPlayer(UUID.randomUUID(), "Guest")
        }
    }
}
