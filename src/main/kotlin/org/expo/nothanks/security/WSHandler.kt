package org.expo.nothanks.security

import org.expo.nothanks.service.GamesService
import org.expo.nothanks.utils.getPlayerId
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession

@Component
class WSHandler(val gameService: GamesService): WebSocketHandler {

    override fun afterConnectionEstablished(session: WebSocketSession) {
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        val principal = session.principal as StompPrincipal
        val playerId = principal.getPlayerId()
        gameService.disconnectPlayerFromLobby(playerId)
    }

    override fun supportsPartialMessages(): Boolean {
        return true
    }
}