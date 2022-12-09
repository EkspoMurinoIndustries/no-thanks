package org.expo.nothanks.model.event.output

data class ScoreResetMessage(
    val all: Boolean = true
): OutputMessage
