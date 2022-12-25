package org.expo.nothanks.model.event.output

data class RoundStartedMessage(
    val currentPlayerNumber: Int,
    val players: List<SafeGamePlayer>,
    val currentCard: Int,
    val remainingNumberCards: Int
): OutputMessage

data class SafeGamePlayer(
    val number: Int,
    val name: String,
    val cards: Set<Int> = setOf(),
    var coins: Int
)