package org.expo.nothanks.model.event.output

data class ScoreResetMessage(
    val round: Int,
    val result: Map<Int, Score>,
    val all: Boolean = true
): OutputMessage
