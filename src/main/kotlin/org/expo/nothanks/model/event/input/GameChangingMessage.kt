package org.expo.nothanks.model.event.input

data class GameChangingMessage(
    val wantToStart: Boolean = false,
    val newParams: NewParams? = null,
    val scoreReset: Boolean = false,
    val wantToAbort: Boolean = false
)

data class NewParams(
    val defaultCoinsCount: Int? = null,
    val minCard: Int? = null,
    val maxCard: Int? = null,
    val removedCards: Int? = null,
    val useDefaultTokens: Boolean? = null,
    val resetToDefaults: Boolean = false
)
