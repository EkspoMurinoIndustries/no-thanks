package org.expo.nothanks.model.event.output

data class PlayerReconnectedMessage(
    val playerName: String,
    val playerNumber: Int,
    val canContinue: Boolean
) : OutputMessage
