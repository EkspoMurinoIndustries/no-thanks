package org.expo.nothanks.utils

import org.expo.nothanks.model.game.Deck
import org.expo.nothanks.model.lobby.Params

private val charPool = ('A'..'Z')

fun createDeck(params: Params): Deck {
    return Deck(
        cards = (params.maxCard..params.maxCard)
            .toMutableList()
            .shuffled()
            .subList(0, params.maxCard - params.extraCards)
    )
}

fun coins(params: Params): Int {
    return params.defaultCoinsCount
}

fun createInviteCode(): String {
    return (1..5)
        .map { charPool.random() }
        .joinToString("")
}