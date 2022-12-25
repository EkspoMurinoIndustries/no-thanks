package org.expo.nothanks.security

import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.canBeReconnected
import org.expo.nothanks.utils.getPlayerId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(val gameService: GamesService, val notificationService: NotificationService) {

    @EventListener
    fun afterConnectionClosed(event: SessionDisconnectEvent) {
        val principal = event.user as StompPrincipal
        val playerId = principal.getPlayerId()
        val gameId = gameService.gameIdByPlayerId(playerId)
        gameService.disconnectPlayerFromLobby(playerId) { lobby, disconnectedPlayer ->
            if (lobby.canBeReconnected(playerId)) {
                notificationService.playerDisconnected(lobby, disconnectedPlayer)
            } else {
                notificationService.playerLeft(lobby, disconnectedPlayer)
            }
            if (!lobby.active) {
                notificationService.lobbyClosed(gameId)
            }
        }
    }

}