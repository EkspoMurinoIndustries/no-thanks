package org.expo.nothanks.model.event.output

import org.expo.nothanks.model.SafeLobbyPlayer

data class LobbyConnectedMessage(
    val newPlayer: SafeLobbyPlayer,
    val allPlayers: List<SafeLobbyPlayer>
): OutputMessage
