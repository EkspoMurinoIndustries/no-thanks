package org.expo.nothanks.controller

import org.expo.nothanks.config.properties.DefaultGameProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.expo.nothanks.security.RestExceptionHandler
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.avatarUrl
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import javax.servlet.http.Cookie

internal class AvatarControllerTest {
    @Test
    fun `only the sending player changes and avatar is included in lobby and round snapshots`() {
        val games = GamesService(DefaultGameProperties(3, 35, 9, 2, 8, (2..8).associateWith { 11 }), 300000)
        val notifications = mock(NotificationService::class.java)
        val http = MockMvcBuilders.standaloneSetup(AvatarImageController(games, notifications))
            .setControllerAdvice(RestExceptionHandler()).build()
        val host = UUID.randomUUID()
        val guest = UUID.randomUUID()
        val lobby = games.createLobby(host)
        games.addPlayerToLobby(lobby.gameId, host, "Host") { _, _ -> }
        games.addPlayerToLobby(lobby.gameId, guest, "Guest") { _, _ -> }
        val bytes = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB), "jpeg", bytes)
        val avatar = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray())
        val endpoint = "/api/lobbies/${lobby.gameId}/avatar"
        val payload = jacksonObjectMapper().writeValueAsString(mapOf("avatar" to avatar))
        http.perform(put(endpoint).contentType("application/json").content(payload))
            .andExpect(status().isBadRequest)
        http.perform(put(endpoint).cookie(Cookie("no-thanks-token", UUID.randomUUID().toString()))
            .contentType("application/json").content(payload)).andExpect(status().isBadRequest)
        assertNull(lobby.players[guest]!!.avatar)
        http.perform(put(endpoint).cookie(Cookie("no-thanks-token", guest.toString()))
            .contentType("application/json").content(payload))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.playerNumber").value(1))
        assertNull(lobby.players[host]!!.avatar)
        assertEquals(lobby.players[guest]!!.avatarUrl(lobby.gameId), lobby.getPlayersInLobby().first { it.number == lobby.players[guest]!!.number }.avatar)
        verify(notifications).updateAvatar(lobby, guest, lobby.players[guest]!!.avatarUrl(lobby.gameId))
        val url = lobby.players[guest]!!.avatarUrl(lobby.gameId)!!
        http.perform(get(url))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(bytes.toByteArray()))
            .andExpect(header().string("Cache-Control", "max-age=31536000, private, immutable"))
        http.perform(get(url.replace(lobby.gameId.toString(), UUID.randomUUID().toString())))
            .andExpect(status().isNotFound)
        http.perform(get(url.replace("/1/", "/999/"))).andExpect(status().isNotFound)
        http.perform(get(url.replace(".jpg", "bad.jpg"))).andExpect(status().isNotFound)

        http.perform(put(endpoint).cookie(Cookie("no-thanks-token", guest.toString()))
            .contentType("application/json").content("""{"avatar":"invalid"}"""))
            .andExpect(status().isBadRequest)
        assertEquals(avatar, lobby.players[guest]!!.avatar)

        games.startNewRound(lobby.gameId, host) { }
        assertEquals(lobby.players[guest]!!.avatarUrl(lobby.gameId), lobby.getPlayersInGame().first { it.number == lobby.players[guest]!!.number }.avatar)
        http.perform(put(endpoint).cookie(Cookie("no-thanks-token", guest.toString()))
            .contentType("application/json").content("""{"avatar":null}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.avatar").doesNotExist())
        assertNull(lobby.players[guest]!!.avatar)
        verify(notifications).updateAvatar(lobby, guest, null)
        http.perform(get(url)).andExpect(status().isNotFound)
    }
}
