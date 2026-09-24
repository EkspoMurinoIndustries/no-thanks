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
        require((minPlayerNumber..maxPlayerNumber).all { coinsMap[it] in 0..10000 }) {
            "coins-map must define a token count between 0 and 10000 for every supported player count"
        }
    }
}
