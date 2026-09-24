package org.expo.nothanks.controller

import org.expo.nothanks.config.properties.DefaultGameProperties
import org.expo.nothanks.model.event.input.ChangeAvatarMessage
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.getPlayersInGame
import org.expo.nothanks.utils.getPlayersInLobby
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

internal class AvatarControllerTest {
    @Test
    fun `only the sending player changes and avatar is included in lobby and round snapshots`() {
        val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }))
        val notifications = mock(NotificationService::class.java)
        val controller = GameController(games, notifications)
        val host = UUID.randomUUID()
        val guest = UUID.randomUUID()
        val lobby = games.createLobby(host)
        games.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        games.addPlayerToLobby(lobby.gameId, guest, "Guest") { _, _ -> }
        val bytes = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB), "jpeg", bytes)
        val avatar = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray())
        controller.avatar(ChangeAvatarMessage(avatar)) { guest.toString() }
        assertNull(lobby.players[host]!!.avatar)
        assertEquals(avatar, lobby.getPlayersInLobby().first { it.number == lobby.players[guest]!!.number }.avatar)
        verify(notifications).updateAvatar(lobby, guest, avatar)

        controller.avatar(ChangeAvatarMessage("invalid")) { guest.toString() }
        assertEquals(avatar, lobby.players[guest]!!.avatar)
        val errors = mockingDetails(notifications).invocations.filter { it.method.name == "sendErrorToUser" }
        assertEquals(1, errors.size)
        assertEquals(lobby.gameId, errors.single().arguments[0])
        assertEquals(guest, errors.single().arguments[1])

        games.startNewRound(lobby.gameId, host) { }
        assertEquals(avatar, lobby.getPlayersInGame().first { it.number == lobby.players[guest]!!.number }.avatar)
        controller.avatar(ChangeAvatarMessage(null)) { guest.toString() }
        assertNull(lobby.players[guest]!!.avatar)
        verify(notifications).updateAvatar(lobby, guest, null)
    }
}
