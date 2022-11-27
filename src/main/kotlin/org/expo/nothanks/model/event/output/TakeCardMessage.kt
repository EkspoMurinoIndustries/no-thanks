package org.expo.nothanks.model.event.output

data class TakeCardMessage(
    val playerNumber: Int,
    val newCardNumber: Int
): OutputMessage
