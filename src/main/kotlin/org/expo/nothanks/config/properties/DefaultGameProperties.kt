package org.expo.nothanks.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("no-thanks.default-game-params")
@ConstructorBinding
data class DefaultGameProperties (
    val minCard: Int,
    val maxCard: Int,
    val removedCards: Int,
    val minPlayerNumber: Int,
    val maxPlayerNumber: Int,
    val coinsMap: Map<Int, Int>
) {
    init {
        require((minPlayerNumber..maxPlayerNumber).all { (coinsMap[it] ?: -1) >= 0 }) {
            "coins-map must define a non-negative token count for every supported player count"
        }
    }
}
