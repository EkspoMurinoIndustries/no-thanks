package org.expo.nothanks.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LobbyCleanup(private val games: GamesService, private val notifications: NotificationService) {
    @Scheduled(fixedDelay = 5000)
    fun expireDisconnectedPlayers() {
        val now = System.currentTimeMillis()
        games.removeExpiredLobbies(now).forEach(notifications::lobbyClosed)
        games.removeExpiredPlayers(now).forEach { (gameId, message) ->
            notifications.playerLeft(gameId, message)
        }
    }
}
