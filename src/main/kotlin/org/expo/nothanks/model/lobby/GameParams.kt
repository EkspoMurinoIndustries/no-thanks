package org.expo.nothanks.model.lobby

data class GameParams(
    var defaultCoinsCount: Int,
    var minCard: Int,
    var maxCard: Int,
    var extraCards: Int,
    var maxPlayerNumber: Int,
    var coinsMap: Map<Int, Int>
)