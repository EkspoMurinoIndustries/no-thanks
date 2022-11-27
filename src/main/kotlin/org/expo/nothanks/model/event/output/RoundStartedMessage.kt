package org.expo.nothanks.model.event.output

data class RoundStartedMessage(
    val eachPlayerCoinCount: Int,
    val currentPlayerNumber: Int
): OutputMessage