package org.expo.nothanks.utils

import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.GameStatus
import org.expo.nothanks.model.game.Player
import org.expo.nothanks.model.game.PlayerResult
import java.util.*

fun Game.takeCard(playerId: UUID) {
    if (this.isValid()) {
        this.checkPlayerCurrent(playerId)
        val currentPlayer = currentPlayer()
        currentPlayer.cards.add(currentCardNumber())
        currentCardCoins = 0
        deck.skip += 1
    } else {
        throw IllegalStateException("Wrong game status")
    }
}

fun Game.putCoin(playerId: UUID) {
    if (this.isValid()) {
        this.checkPlayerCurrent(playerId)
        val currentPlayer = currentPlayer()
        if (currentPlayer.coins < 1) {
            throw IllegalStateException("Player does not have enough coins")
        }
        currentPlayer.coins = currentPlayer.coins - 1
        currentCardCoins += 1
        setNextPlayer()
    } else {
        throw IllegalStateException("Wrong game status")
    }
}

fun Game.setNextPlayer() {
    currentPlayer = currentPlayer.nextPlayer!!
}

fun Game.previousPlayer(): Player {
    return playerSequence().find { it.nextPlayer == currentPlayer } ?: throw IllegalStateException()
}

fun Game.calculateResult(): Map<UUID, PlayerResult> {
    return this.playerSequence()
        .associate { it.id to PlayerResult(calculateCards(it.cards) - it.coins) }
}

fun Game.continueGame() {
    this.status = GameStatus.STARTED
}

fun Game.playerSequence(): Sequence<Player> {
    return sequence {
        var p = currentPlayer
        for (i: Int in 0 until playerCount) {
            yield(p)
            p = p.nextPlayer!!
        }
    }
}

fun calculateCards(cards: Set<Int>): Int {
    var lastCard = -1
    var score = 0
    for (card in cards.toList().sorted()) {
        if (lastCard != card - 1) {
            score += card
        }
        lastCard = card
    }
    return score
}

fun Game.pause() {
    status = GameStatus.PAUSED
}

fun Game.currentCardNumber() = deck.cards[deck.skip]

fun Game.checkPlayerCurrent(playerId: UUID) {
    if (currentPlayer.id != playerId) {
        throw IllegalStateException("Player is not current")
    }
}

fun Game.isRoundEnded(): Boolean = deck.skip > deck.cards.size

fun Game.currentPlayer() = currentPlayer

fun Game.currentPlayerNumber() = currentPlayer.number

fun Game.isValid() = this.status != GameStatus.PAUSED