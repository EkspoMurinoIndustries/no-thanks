package org.expo.nothanks.model.event.output

import org.expo.nothanks.model.lobby.GameParams

data class ParamsChangedMessage(
    val newParams: GameParams
) : OutputMessage
