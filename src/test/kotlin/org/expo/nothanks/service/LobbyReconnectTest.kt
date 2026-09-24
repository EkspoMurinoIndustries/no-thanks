package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.exception.InviteHasNotBeenFound
import org.expo.nothanks.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class LobbyReconnectTest {
    private val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }), 1000)
    private val host = UUID.randomUUID()
    private val guest = UUID.randomUUID()
    private val lobby = games.createLobby(host).also {
        games.addPlayerToLobby(it.gameId, host, "Host", "host-1") { _, _ -> }
        games.addPlayerToLobby(it.gameId, guest, "Guest", "guest-1") { _, _ -> }
    }

    @Test
    fun `both host and guest keep pregame seats and settings during grace`() {
        lobby.params.maxCard = 55
        lobby.players[host]!!.avatar = "saved avatar"
        for ((player, session) in listOf(host to "host-1", guest to "guest-1")) {
            games.disconnectPlayerFromLobby(player, session) { _, _ -> }
            assertEquals(lobby.gameId, games.gameIdByInviteCode(lobby.inviteCode))
            assertEquals(2, lobby.players.size)
            assertTrue(lobby.canBeReconnected(player))
            assertTrue(lobby.getPlayersInLobby().first { it.number == lobby.players[player]!!.number }.disconnected)
            assertThrows(GameException::class.java) { games.startNewRound(lobby.gameId, host) { } }
            games.addPlayerToLobby(lobby.gameId, player, "Ignored", "$session-new") { _, newPlayer -> assertFalse(newPlayer) }
            assertFalse(lobby.canBeReconnected(player))
        }
        games.expireDisconnectedPlayers(System.currentTimeMillis() + 2000, { fail("Reconnected lobby expired") }, { _, _ -> fail("Player expired") })
        assertEquals(55, lobby.params.maxCard)
        assertEquals("saved avatar", lobby.players[host]!!.avatar)
    }

    @Test
    fun `all players may disconnect during a round and resume the same game`() {
        games.startNewRound(lobby.gameId, host) { }
        val game = lobby.game
        games.disconnectPlayerFromLobby(host, "host-1") { _, _ -> }
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> }
        assertEquals(lobby.gameId, games.gameIdByInviteCode(lobby.inviteCode))
        games.addPlayerToLobby(lobby.gameId, host, "Host", "host-2") { _, _ -> }
        games.addPlayerToLobby(lobby.gameId, guest, "Guest", "guest-2") { _, _ -> }
        assertSame(game, lobby.game)
        assertEquals(1, lobby.round)
    }

    @Test
    fun `unknown duplicate and stale session closes are harmless`() {
        var events = 0
        games.disconnectPlayerFromLobby(UUID.randomUUID(), "unknown") { _, _ -> events++ }
        games.disconnectPlayerFromLobby(host, "failed-handshake") { _, _ -> events++ }
        games.disconnectPlayerFromLobby(host, "host-1") { _, _ -> events++ }
        games.disconnectPlayerFromLobby(host, "host-1") { _, _ -> events++ }
        assertEquals(1, events)
        games.addPlayerToLobby(lobby.gameId, host, "Host", "host-2") { _, _ -> }
        games.disconnectPlayerFromLobby(host, "host-1") { _, _ -> events++ }
        assertFalse(lobby.canBeReconnected(host))
        assertEquals(1, events)
    }

    @Test
    fun `host expiry closes lobby once and safely ignores later guest disconnects`() {
        var closures = 0
        games.disconnectPlayerFromLobby(host, "host-1") { _, _ -> }
        games.expireDisconnectedPlayers(System.currentTimeMillis() + 2000, { closures++ }, { _, _ -> })
        assertFalse(lobby.active)
        assertEquals(1, closures)
        assertThrows(InviteHasNotBeenFound::class.java) { games.gameIdByInviteCode(lobby.inviteCode) }
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> fail("Deleted lobby must be ignored") }
        games.expireDisconnectedPlayers(Long.MAX_VALUE, { closures++ }, { _, _ -> })
        assertEquals(1, closures)
    }

    @Test
    fun `guest expiry frees a pregame seat without closing the lobby`() {
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> }
        var departures = 0
        games.expireDisconnectedPlayers(System.currentTimeMillis() + 2000, { fail("Guest must not close lobby") }, { _, _ -> departures++ })
        assertEquals(1, departures)
        assertEquals(1, lobby.players.size)
        assertEquals(lobby.gameId, games.gameIdByInviteCode(lobby.inviteCode))
    }
}
