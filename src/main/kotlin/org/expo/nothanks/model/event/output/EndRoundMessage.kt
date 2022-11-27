package org.expo.nothanks.model.event.output

data class EndRoundMessage(
    val result: Map<Int, Score>
) : OutputMessage

data class Score(
    val playerName: String,
    val rounds: List<Int>,
    val totalScore: Int,
    val lastRoundScore: Int
)



