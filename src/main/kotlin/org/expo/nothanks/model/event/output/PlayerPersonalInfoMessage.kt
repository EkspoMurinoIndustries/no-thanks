package org.expo.nothanks.model.event.output

data class PlayerPersonalInfoMessage(
    val coins: Int,
    val cards: Set<Int>,
    val isCurrentPlayer: Boolean
): OutputMessage