package org.expo.nothanks.exception

import java.util.*

class GameHasNotBeenFound(gameId: UUID) :
    NoThanksException(
        "Game has not been found: $gameId",
        "Game has not been found"
    )