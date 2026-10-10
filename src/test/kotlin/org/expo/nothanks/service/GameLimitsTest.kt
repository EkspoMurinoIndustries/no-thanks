package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.config.properties.GameLimitsProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.model.event.input.NewParams
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GameLimitsTest {
    private val defaults = DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 })

    @Test
    fun `custom limits control validation and are exposed with lobby settings`() {
        val limits = GameLimitsProperties(2, 30, 60, 20)
        val games = GamesService(defaults, 300000, limits)
        val host = UUID.randomUUID()
        val lobby = games.createLobby(host)
        games.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        assertEquals(limits, lobby.params.limits)
        games.changeParams(lobby.gameId, host, NewParams(minCard = 2, maxCard = 60, defaultCoinsCount = 20)) { }
        assertEquals(60, lobby.params.maxCard)
        val before = lobby.params.copy()
        for (invalid in listOf(NewParams(minCard = 1), NewParams(maxCard = 29),
            NewParams(maxCard = 61), NewParams(defaultCoinsCount = 21))) {
            assertThrows(GameException::class.java) { games.changeParams(lobby.gameId, host, invalid) { } }
            assertEquals(before, lobby.params)
        }
        games.changeParams(lobby.gameId, host, NewParams(maxCard = 30, defaultCoinsCount = 0)) { }
        games.changeParams(lobby.gameId, host, NewParams(resetToDefaults = true)) { }
        assertEquals(35, lobby.params.maxCard)
        assertEquals(limits, lobby.params.limits)
    }

    @Test
    fun `invalid limits and incompatible defaults fail immediately`() {
        assertThrows(IllegalArgumentException::class.java) { GameLimitsProperties(0, 30, 60, 20) }
        assertThrows(IllegalArgumentException::class.java) { GameLimitsProperties(2, 1, 60, 20) }
        assertThrows(IllegalArgumentException::class.java) { GameLimitsProperties(2, 30, 29, 20) }
        assertThrows(IllegalArgumentException::class.java) { GameLimitsProperties(2, 30, 60, -1) }
        assertThrows(IllegalArgumentException::class.java) { GamesService(defaults, 300000, GameLimitsProperties(4, 30, 60, 20)) }
        assertThrows(IllegalArgumentException::class.java) { GamesService(defaults, 300000, GameLimitsProperties(2, 30, 60, 10)) }
    }
}
