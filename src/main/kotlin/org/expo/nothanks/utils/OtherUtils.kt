package org.expo.nothanks.utils

import org.expo.nothanks.model.game.Deck
import org.expo.nothanks.model.lobby.Params
import org.springframework.messaging.simp.user.SimpUserRegistry
import java.util.*

private val charPool = ('A'..'Z')

fun createDeck(params: Params): Deck {
    val cardCount = params.maxCard - params.minCard + 1
    return Deck(
        cards = (params.minCard..params.maxCard)
            .toMutableList()
            .shuffled()
            .subList(0, cardCount - params.extraCards)
    )
}

fun createInviteCode(): String {
    return (1..5)
        .map { charPool.random() }
        .joinToString("")
}

fun SimpUserRegistry.isUserExist(id: UUID): Boolean {
    return isUserExist(id.toString())
}

fun SimpUserRegistry.isUserExist(id: String): Boolean {
    return users.any { it.name == id }
}