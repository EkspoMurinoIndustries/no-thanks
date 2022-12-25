package org.expo.nothanks.model.event.output

data class PlayerReconnectedMessage(
    val player: SafeLobbyPlayer,
    val gameStarted: Boolean,
    val gameInfo: SafeGamePLayer?
) : OutputMessage
