package org.expo.nothanks.exception

import java.util.*

class PlayerException(text: String, gameId: UUID, playerId: UUID) :
    NoThanksException(
        "$text. GameId: $gameId, playerId: $playerId",
        text
    )