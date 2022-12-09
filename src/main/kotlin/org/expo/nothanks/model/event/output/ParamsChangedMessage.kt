package org.expo.nothanks.model.event.output

import org.expo.nothanks.model.lobby.Params

data class ParamsChangedMessage(
    val newParams: Params
) : OutputMessage
