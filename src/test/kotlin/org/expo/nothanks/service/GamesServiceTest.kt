package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.model.event.input.NewParams
import org.expo.nothanks.utils.getGame
import org.expo.nothanks.utils.playerSequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.expo.nothanks.utils.isGameStarted
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GamesServiceTest {

    @Test
    fun `token defaults must cover every supported player count`() {
        assertThrows(IllegalArgumentException::class.java) {
            defaultGameProperties().copy(coinsMap = mapOf(2 to 11))
        }
    }

    @Test
    fun `settings are host only validated atomically and persist across rounds`() {
        val service = GamesService(defaultGameProperties())
        val host = UUID.randomUUID()
        val guest = UUID.randomUUID()
        val lobby = service.createLobby(host)
        service.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        service.addPlayerToLobby(lobby.gameId, guest, "Guest") { _, _ -> }
        val settings = NewParams(minCard = 5, maxCard = 55, removedCards = 2, defaultCoinsCount = 4)
        assertThrows(GameException::class.java) {
            service.changeParams(lobby.gameId, guest, settings) { }
        }
        service.changeParams(lobby.gameId, host, settings) { }
        val saved = lobby.params.copy()
        for (invalid in listOf(
            NewParams(maxCard = 34), NewParams(maxCard = 56),
            NewParams(minCard = 0), NewParams(minCard = 56),
            NewParams(minCard = 54),
            NewParams(removedCards = -1), NewParams(removedCards = 51),
            NewParams(maxCard = 40, defaultCoinsCount = -1)
        )) {
            assertThrows(GameException::class.java) {
                service.changeParams(lobby.gameId, host, invalid) { }
            }
            assertEquals(saved, lobby.params)
        }
        repeat(2) {
            service.startNewRound(lobby.gameId, host) { }
            assertTrue(lobby.getGame().playerSequence().all { it.coins == 4 })
            assertEquals(2, lobby.getGame().deck.removedCards.size)
            assertEquals(49, lobby.getGame().deck.cards.size)
            assertEquals((5..55).toSet(), (lobby.getGame().deck.cards + lobby.getGame().deck.removedCards).toSet())
            assertThrows(GameException::class.java) {
                service.changeParams(lobby.gameId, host, NewParams(defaultCoinsCount = 9)) { }
            }
            assertEquals(saved, lobby.params)
            service.abortRound(lobby.gameId, host) { }
        }
    }

    @Test
    fun `automatic tokens follow player count and reset clears manual overrides`() {
        val service = GamesService(defaultGameProperties())
        val host = UUID.randomUUID()
        val lobby = service.createLobby(host)
        service.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        for (count in 2..8) {
            val guest = UUID.randomUUID()
            service.addPlayerToLobby(lobby.gameId, guest, "Guest$count") { _, _ -> }
            val expected = when (count) {
                6 -> 9
                7 -> 7
                8 -> 6
                else -> 11
            }
            // Editing cards alone must preserve automatic tokens.
            service.changeParams(lobby.gameId, host, NewParams(minCard = 5, maxCard = 55)) { }
            service.startNewRound(lobby.gameId, host) { }
            assertTrue(lobby.getGame().playerSequence().all { it.coins == expected })
            assertThrows(GameException::class.java) {
                service.changeParams(lobby.gameId, host, NewParams(resetToDefaults = true)) { }
            }
            service.abortRound(lobby.gameId, host) { }
            service.changeParams(lobby.gameId, host, NewParams(defaultCoinsCount = 0, removedCards = 1)) { }
            service.startNewRound(lobby.gameId, host) { }
            assertTrue(lobby.getGame().playerSequence().all { it.coins == 0 })
            service.abortRound(lobby.gameId, host) { }
            assertThrows(GameException::class.java) {
                service.changeParams(lobby.gameId, guest, NewParams(resetToDefaults = true)) { }
            }
            service.changeParams(lobby.gameId, host, NewParams(resetToDefaults = true)) { }
            assertEquals(35, lobby.params.maxCard)
            assertEquals(3, lobby.params.minCard)
            assertEquals(9, lobby.params.removedCards)
            assertTrue(lobby.params.useDefaultTokens)
            service.startNewRound(lobby.gameId, host) { }
            assertTrue(lobby.getGame().playerSequence().all { it.coins == expected })
            service.abortRound(lobby.gameId, host) { }
        }
    }

    @Test
    fun `only the host can abort an active round`() {
        val gamesService = GamesService(defaultGameProperties())
        val hostId = UUID.randomUUID()
        val guestId = UUID.randomUUID()
        val lobby = gamesService.createLobby(hostId)
        gamesService.addPlayerToLobby(lobby.gameId, hostId, "Host") { _, _ -> }
        gamesService.addPlayerToLobby(lobby.gameId, guestId, "Guest") { _, _ -> }
        gamesService.startNewRound(lobby.gameId, hostId) { }

        assertThrows(GameException::class.java) {
            gamesService.abortRound(lobby.gameId, guestId) { }
        }
        assertTrue(lobby.isGameStarted())

        gamesService.abortRound(lobby.gameId, hostId) { }

        assertFalse(lobby.isGameStarted())
    }

    private fun defaultGameProperties() = DefaultGameProperties(
        minCard = 3,
        maxCard = 35,
        removedCards = 9,
        minPlayerNumber = 2,
        maxPlayerNumber = 8,
        coinsMap = mapOf(1 to 11, 2 to 11, 3 to 11, 4 to 11, 5 to 11, 6 to 9, 7 to 7, 8 to 6)
    )
}
