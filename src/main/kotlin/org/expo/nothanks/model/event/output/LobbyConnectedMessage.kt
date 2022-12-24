package org.expo.nothanks.model.event.output

data class LobbyConnectedMessage(
    val newPlayer: SafeLobbyPlayer,
    val allPlayers: List<SafeLobbyPlayer>
): OutputMessage
