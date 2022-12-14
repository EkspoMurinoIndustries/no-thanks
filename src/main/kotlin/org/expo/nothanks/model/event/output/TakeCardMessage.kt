package org.expo.nothanks.model.event.output

data class TakeCardMessage(
    val playerNumber: Int,
    val takenCard: Int,
    val newCardNumber: Int,
    val allPlayerCards: Set<Int>,
    val remainingNumberCards: Int
): OutputMessage
