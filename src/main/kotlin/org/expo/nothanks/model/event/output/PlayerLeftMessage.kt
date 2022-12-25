package org.expo.nothanks.model.event.output

data class PlayerLeftMessage(
    val isGameStarted: Boolean,
    val player: SafeLobbyPlayer,
    val remainingPlayersNumber: Int
): OutputMessage
