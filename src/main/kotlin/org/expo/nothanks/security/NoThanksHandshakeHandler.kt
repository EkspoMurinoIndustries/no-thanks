package org.expo.nothanks.security

import org.expo.nothanks.utils.getCookies
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.*

@Component
class NoThanksHandshakeHandler(@Value("\${no-thanks.cookies.id}") val tokenName: String) : DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal {
        val token = request.getCookies()[tokenName] ?: UUID.randomUUID().toString()
        return StompPrincipal(token)
    }
}