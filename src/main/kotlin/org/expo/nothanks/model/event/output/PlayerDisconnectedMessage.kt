package org.expo.nothanks.model.event.output

data class PlayerDisconnectedMessage(
    val isGameStarted: Boolean,
    val playerName: String,
    val playerNumber: Int
): OutputMessage
