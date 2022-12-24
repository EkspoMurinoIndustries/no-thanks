package org.expo.nothanks.model.event.input

data class ConnectToGameMessage(
    val name: String,
    val inviteCode: String?,
    val createGame: Boolean = false
)