package org.expo.nothanks.model.event.output

data class EndRoundMessage(
    val result: Map<Int, Score>,
    val round: Int,
    val removedCards: List<Int>
) : OutputMessage

data class Score(
    val playerName: String,
    val rounds: List<Int>,
    val totalScore: Int,
    val lastRoundScore: Int
)



