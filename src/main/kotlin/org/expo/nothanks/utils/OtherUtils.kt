package org.expo.nothanks.utils

import org.expo.nothanks.model.game.Deck
import org.expo.nothanks.model.lobby.GameParams
import org.springframework.messaging.simp.user.SimpUserRegistry
import java.util.*

private val charPool = ('A'..'Z')

fun createDeck(params: GameParams): Deck {
    val cardCount = params.maxCard - params.minCard + 1
    val shuffledCards = (params.minCard..params.maxCard).shuffled()
    val playableCardCount = cardCount - params.extraCards
    return Deck(
        cards = shuffledCards.take(playableCardCount),
        removedCards = shuffledCards.drop(playableCardCount).sorted()
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
