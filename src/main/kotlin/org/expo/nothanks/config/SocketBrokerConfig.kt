package org.expo.nothanks.config

import org.expo.nothanks.security.NoThanksHandshakeHandler
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class SocketBrokerConfig(
    val noThanksHandshakeHandler: NoThanksHandshakeHandler
): WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/lobby")
        config.setApplicationDestinationPrefixes("/app")
        config.setUserDestinationPrefix("/players")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/no-thanks")
            .setHandshakeHandler(noThanksHandshakeHandler)
            .withSockJS()
    }
}