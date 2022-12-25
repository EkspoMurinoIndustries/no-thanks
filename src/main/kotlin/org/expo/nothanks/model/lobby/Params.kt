package org.expo.nothanks.model.lobby

data class Params(
    var defaultCoinsCount: Int = 9,
    var minCard: Int = 3,
    var maxCard: Int = 35,
    var extraCards: Int = 30,
    var maxPlayerNumber: Int = 7
)