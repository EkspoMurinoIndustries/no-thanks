package org.expo.nothanks.model.event.output

import java.util.*

data class UserConnectedStatus(
    val status: String,
    val gameId: UUID? = null,
    val allPlayers: List<String> = emptyList(),
    val isCreator: Boolean = false,
    val errorMessage: String? = null
)