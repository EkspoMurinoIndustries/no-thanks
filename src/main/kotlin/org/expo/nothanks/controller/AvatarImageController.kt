package org.expo.nothanks.controller

import org.expo.nothanks.service.GamesService
import org.expo.nothanks.utils.avatarVersion
import org.expo.nothanks.utils.avatarUrl
import org.expo.nothanks.utils.getPlayer
import org.expo.nothanks.utils.validatedAvatar
import org.expo.nothanks.model.event.input.ChangeAvatarMessage
import org.expo.nothanks.model.event.output.PlayerAvatarChangedMessage
import org.expo.nothanks.service.NotificationService
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody
import java.util.Base64
import java.util.UUID

@RestController
class AvatarImageController(private val gamesService: GamesService,
                            private val notifications: NotificationService) {
    @PutMapping("/api/lobbies/{gameId}/avatar", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun upload(@PathVariable gameId: UUID,
               @CookieValue("no-thanks-token") playerId: UUID,
               @RequestBody message: ChangeAvatarMessage): PlayerAvatarChangedMessage {
        val avatar = message.avatar.validatedAvatar()
        lateinit var response: PlayerAvatarChangedMessage
        gamesService.changeGameWithLock(gameId) { lobby ->
            val player = lobby.getPlayer(playerId)
            player.avatar = avatar
            val url = player.avatarUrl(gameId)
            response = PlayerAvatarChangedMessage(player.number, url)
            notifications.updateAvatar(lobby, playerId, url)
        }
        return response
    }

    @GetMapping("/avatars/{gameId}/{playerNumber}/{version}.jpg")
    fun image(@PathVariable gameId: UUID, @PathVariable playerNumber: Int,
              @PathVariable version: String): ResponseEntity<ByteArray> {
        var avatar: String? = null
        gamesService.changeGameWithLock(gameId, ignoreMissing = true) { lobby ->
            avatar = lobby.players.values.firstOrNull { it.number == playerNumber }?.avatar
                ?.takeIf { it.avatarVersion() == version }
        }
        val data = avatar ?: return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .header("Cache-Control", "max-age=31536000, private, immutable")
            .body(Base64.getDecoder().decode(data.substringAfter(',')))
    }
}
