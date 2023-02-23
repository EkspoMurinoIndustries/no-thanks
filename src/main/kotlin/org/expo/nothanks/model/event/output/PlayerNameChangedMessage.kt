package org.expo.nothanks.model.event.output

data class PlayerNameChangedMessage (
    val newName: String,
    val playerNumber: Int
): OutputMessage