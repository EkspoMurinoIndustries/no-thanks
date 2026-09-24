package org.expo.nothanks.model.lobby

data class GameParams(
    var initialCoinsCount: Int,
    var minCard: Int,
    var maxCard: Int,
    var removedCards: Int,
    var maxPlayerNumber: Int,
    var useDefaultTokens: Boolean = false
)
