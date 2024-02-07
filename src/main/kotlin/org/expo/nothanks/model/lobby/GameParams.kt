package org.expo.nothanks.model.lobby

data class GameParams(
    var initialCoinsCount: Int,
    var minCard: Int,
    var maxCard: Int,
    var extraCards: Int,
    var maxPlayerNumber: Int
)