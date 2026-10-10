package org.expo.nothanks.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("no-thanks.game-limits")
@ConstructorBinding
data class GameLimitsProperties(
    val minCard: Int,
    val minBiggestCard: Int,
    val maxCard: Int,
    val maxTokens: Int
) {
    init {
        require(minCard > 0 && minBiggestCard >= minCard && maxCard >= minBiggestCard) {
            "game-limits must satisfy 0 < min-card <= min-biggest-card <= max-card"
        }
        require(maxTokens >= 0) { "game-limits.max-tokens must be non-negative" }
    }
}
