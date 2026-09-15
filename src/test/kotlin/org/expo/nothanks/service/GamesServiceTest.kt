package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.utils.isGameStarted
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GamesServiceTest {

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
        defaultCoinsCount = 11,
        minCard = 3,
        maxCard = 35,
        extraCards = 9,
        minPlayerNumber = 2,
        maxPlayerNumber = 8,
        coinsMap = emptyMap()
    )
}
