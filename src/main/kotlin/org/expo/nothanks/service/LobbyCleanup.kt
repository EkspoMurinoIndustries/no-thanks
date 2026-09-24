package org.expo.nothanks.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LobbyCleanup(private val games: GamesService, private val notifications: NotificationService) {
    @Scheduled(fixedDelay = 5000)
    fun expireDisconnectedPlayers() {
        games.expireDisconnectedPlayers(
            onClosed = notifications::lobbyClosed,
            onPlayerLeft = notifications::playerLeft
        )
    }
}
