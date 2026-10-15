package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.expo.nothanks.exception.InviteHasNotBeenFound
import org.expo.nothanks.utils.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

internal class LobbyReconnectTest {
    private val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }), 1000, org.expo.nothanks.testGameLimits())
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
        assertTrue(games.removeExpiredLobbies(Long.MAX_VALUE).isEmpty())
        assertTrue(games.removeExpiredPlayers(Long.MAX_VALUE).isEmpty())
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
        closures += games.removeExpiredLobbies(System.currentTimeMillis() + 2000).size
        assertEquals(1, closures)
        assertThrows(InviteHasNotBeenFound::class.java) { games.gameIdByInviteCode(lobby.inviteCode) }
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> fail("Deleted lobby must be ignored") }
        closures += games.removeExpiredLobbies(Long.MAX_VALUE).size
        assertEquals(1, closures)
    }

    @Test
    fun `guest expiry frees a pregame seat without closing the lobby`() {
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> }
        assertTrue(games.removeExpiredLobbies(Long.MAX_VALUE).isEmpty())
        val departures = games.removeExpiredPlayers(Long.MAX_VALUE).size
        assertEquals(1, departures)
        assertEquals(1, lobby.players.size)
        assertEquals(lobby.gameId, games.gameIdByInviteCode(lobby.inviteCode))
    }

    @Test
    fun `cleanup preserves grace period active rounds and scored players`() {
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> }
        assertTrue(games.removeExpiredPlayers(0).isEmpty())
        games.addPlayerToLobby(lobby.gameId, guest, "Guest", "guest-2") { _, _ -> }
        games.startNewRound(lobby.gameId, host) { }
        games.disconnectPlayerFromLobby(guest, "guest-2") { _, _ -> }
        assertTrue(games.removeExpiredPlayers(Long.MAX_VALUE).isEmpty())
        games.abortRound(lobby.gameId, host) { }
        games.changeGameWithLock(lobby.gameId) { it.players.getValue(guest).score.add(12) }
        assertTrue(games.removeExpiredPlayers(Long.MAX_VALUE).isEmpty())
        assertEquals(2, lobby.players.size)
    }

    @Test
    fun `departure snapshot survives subsequent joins and expired player can join elsewhere`() {
        games.disconnectPlayerFromLobby(guest, "guest-1") { _, _ -> }
        val (gameId, message) = games.removeExpiredPlayers(Long.MAX_VALUE).single()
        games.addPlayerToLobby(lobby.gameId, UUID.randomUUID(), "New guest") { _, _ -> }
        val other = games.createLobby(UUID.randomUUID())
        games.addPlayerToLobby(other.gameId, guest, "Guest", "guest-new") { _, _ -> }
        assertEquals(lobby.gameId, gameId)
        assertEquals(1, message.remainingPlayersNumber)
        assertFalse(message.isGameStarted)
        assertEquals("Guest", message.player.name)
        assertEquals(other.gameId, games.gameIdByPlayerId(guest))
        assertTrue(games.removeExpiredPlayers(Long.MAX_VALUE).isEmpty())
    }
}
