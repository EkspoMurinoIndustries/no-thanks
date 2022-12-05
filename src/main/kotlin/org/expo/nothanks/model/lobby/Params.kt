package org.expo.nothanks.model.lobby

data class Params(
    val defaultCoinsCount: Int = 9,
    val minCard: Int = 3,
    val maxCard: Int = 35,
    val extraCards: Int = 4
)