package org.expo.nothanks.model

import java.util.*

data class CreateGameResponse(
    val gameId: UUID,
    val inviteCode: String
)