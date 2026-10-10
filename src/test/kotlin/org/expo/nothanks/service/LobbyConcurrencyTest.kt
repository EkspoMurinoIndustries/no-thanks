package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.exception.GameException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

internal class LobbyConcurrencyTest {
    private val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }), 1000)

    @Test
    fun `busy lobby does not block another lobby while same lobby stays serialized`() {
        val first = games.createLobby(UUID.randomUUID())
        val second = games.createLobby(UUID.randomUUID())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val waiting = CountDownLatch(1)
        val sameLobbyEntered = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(3)
        try {
            val holder = executor.submit {
                games.changeGameWithLock(first.gameId) {
                    entered.countDown()
                    check(release.await(10, SECONDS))
                }
            }
            assertTrue(entered.await(5, SECONDS))
            val sameLobby = executor.submit {
                waiting.countDown()
                games.changeGameWithLock(first.gameId) { sameLobbyEntered.countDown() }
            }
            assertTrue(waiting.await(5, SECONDS))
            executor.submit { games.changeGameWithLock(second.gameId) { it.round++ } }.get(5, SECONDS)
            assertEquals(1, second.round)
            assertEquals(1L, sameLobbyEntered.count)
            release.countDown()
            holder.get(5, SECONDS)
            sameLobby.get(5, SECONDS)
            assertEquals(0L, sameLobbyEntered.count)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `simultaneous joins claim only one lobby`() {
        val lobbies = List(2) { games.createLobby(UUID.randomUUID()) }
        val player = UUID.randomUUID()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val joins = lobbies.map { lobby ->
                executor.submit<Boolean> {
                    check(start.await(5, SECONDS))
                    try {
                        games.addPlayerToLobby(lobby.gameId, player, "Guest") { _, _ -> }
                        true
                    } catch (expected: GameException) {
                        false
                    }
                }
            }
            start.countDown()
            assertEquals(1, joins.count { it.get(5, SECONDS) })
            val joined = lobbies.single { player in it.players }
            assertEquals(joined.gameId, games.gameIdByPlayerId(player))
        } finally {
            executor.shutdownNow()
        }
    }
}
