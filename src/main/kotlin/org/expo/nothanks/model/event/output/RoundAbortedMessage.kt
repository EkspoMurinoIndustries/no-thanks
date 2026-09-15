package org.expo.nothanks.model.event.output

data class RoundAbortedMessage(
    val round: Int,
    val result: Map<Int, Score>
) : OutputMessage
