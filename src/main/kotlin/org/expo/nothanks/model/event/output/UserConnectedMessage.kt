package org.expo.nothanks.model.event.output

data class UserConnectedMessage(
    val newPlayerName: String,
    val allPlayers: List<String>
)
