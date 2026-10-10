package org.expo.nothanks.service

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.model.event.output.PlayerLeftMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

internal class LobbyCleanupTest {
    @Test
    fun `departure notification is sent after releasing the lobby lock`() {
        val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }), -1)
        val host = UUID.randomUUID()
        val guest = UUID.randomUUID()
        val lobby = games.createLobby(host)
        games.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        games.addPlayerToLobby(lobby.gameId, guest, "Guest") { _, _ -> }
        games.disconnectPlayerFromLobby(guest) { _, _ -> }
        val executor = Executors.newSingleThreadExecutor()
        val sent = mutableListOf<PlayerLeftMessage>()
        val channel = object : MessageChannel {
            override fun send(message: Message<*>): Boolean = send(message, -1)

            override fun send(message: Message<*>, timeout: Long): Boolean {
                executor.submit {
                    games.changeGameWithLock(lobby.gameId) {
                        assertFalse(guest in it.players)
                    }
                }.get(5, SECONDS)
                sent.add(message.payload as PlayerLeftMessage)
                return true
            }
        }
        try {
            val cleanup = LobbyCleanup(games, NotificationService(SimpMessagingTemplate(channel)))
            cleanup.expireDisconnectedPlayers()
            cleanup.expireDisconnectedPlayers()
            assertEquals(1, sent.size)
            assertEquals(1, sent.single().remainingPlayersNumber)
        } finally {
            executor.shutdownNow()
        }
    }
}
