package org.expo.nothanks.model.lobby

import org.expo.nothanks.model.game.Game
import java.util.*

data class Lobby(
    val creator: UUID,
    val disconnectedPLayers: MutableSet<UUID> = mutableSetOf(),
    val players: MutableMap<UUID, LobbyPlayer> = mutableMapOf(),
    val inviteCode: String,
    val params: Params = Params(),
    val gameId: UUID = UUID.randomUUID(),
    var game: Game? = null,
    var round: Int = 0
)
