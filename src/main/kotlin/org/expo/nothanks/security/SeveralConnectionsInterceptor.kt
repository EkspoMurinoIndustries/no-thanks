package org.expo.nothanks.security

import org.expo.nothanks.utils.getCookies
import org.expo.nothanks.utils.isUserExist
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.springframework.context.annotation.Lazy

@Component
class SeveralConnectionsInterceptor(
    @Value("\${no-thanks.cookies.id}") val tokenName: String,
    @Lazy val simpUserRegistry: SimpUserRegistry
) : HandshakeInterceptor {


    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val token = request.getCookies()[tokenName] ?: return false
        return !simpUserRegistry.isUserExist(token)
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
    }
}