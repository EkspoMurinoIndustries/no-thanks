package org.expo.nothanks.exception

import java.util.*

class GameException(text: String, gameId: UUID) :
    NoThanksException(
        "$text. GameId: $gameId",
        text
    )