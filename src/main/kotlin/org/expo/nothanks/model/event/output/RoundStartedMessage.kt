package org.expo.nothanks.model.event.output

data class RoundStartedMessage(
    val currentPlayerNumber: Int,
    val players: List<SafeGamePLayer>,
    val currentCard: Int,
    val leftNumberCards: Int
): OutputMessage

data class SafeGamePLayer(
    val number: Int,
    val name: String,
    val cards: Set<Int> = setOf(),
    var coins: Int
)