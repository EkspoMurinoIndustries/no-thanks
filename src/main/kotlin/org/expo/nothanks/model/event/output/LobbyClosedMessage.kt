package org.expo.nothanks.model.event.output

data class LobbyClosedMessage(
    val closed: Boolean = true
) : OutputMessage
