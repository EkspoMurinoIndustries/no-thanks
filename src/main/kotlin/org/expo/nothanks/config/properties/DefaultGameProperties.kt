package org.expo.nothanks.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("no-thanks.default-game-params")
@ConstructorBinding
data class DefaultGameProperties (
    val defaultCoinsCount: Int,
    val minCard: Int,
    val maxCard: Int,
    val extraCards: Int,
    val minPlayerNumber: Int,
    val maxPlayerNumber: Int,
    val coinsMap: Map<Int, Int>
)
