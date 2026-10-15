package org.expo.nothanks.security

import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.getPlayerId
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(val gameService: GamesService, val notificationService: NotificationService) {

    @EventListener
    fun afterConnectionClosed(event: SessionDisconnectEvent) {
        val principal = event.user as? StompPrincipal ?: return
        val playerId = principal.getPlayerId()
        gameService.disconnectPlayerFromLobby(playerId, event.sessionId) { lobby, disconnectedPlayer ->
            notificationService.playerDisconnected(lobby, disconnectedPlayer)
        }
    }

}
