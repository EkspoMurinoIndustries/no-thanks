package org.expo.nothanks.model.lobby

import org.expo.nothanks.config.properties.GameLimitsProperties

data class GameParams(
    var initialCoinsCount: Int,
    var minCard: Int,
    var maxCard: Int,
    var removedCards: Int,
    var maxPlayerNumber: Int,
    var useDefaultTokens: Boolean = false,
    val limits: GameLimitsProperties
)
