package org.expo.nothanks.exception

import java.util.UUID

class GameHasNotBeenFound(gameId: UUID): IllegalStateException("Game has not been found by id $gameId") {
}