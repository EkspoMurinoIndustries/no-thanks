package org.expo.nothanks.model.event.output

data class PutCoinMessage (
    val playerNumber: Int,
    val newCurrentPlayerNumber: Int,
    val currentCardCoins: Int
): OutputMessage
