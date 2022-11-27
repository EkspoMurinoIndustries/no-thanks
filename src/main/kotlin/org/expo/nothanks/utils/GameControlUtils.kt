package org.expo.nothanks.utils

import org.expo.nothanks.model.event.output.Score
import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.GameStatus
import java.util.*

fun Game.takeCard() {
    val currentPlayer = currentPlayer()
    currentPlayer.cards.add(currentCardNumber())
    currentCardCoins = 0
    deck.skip += 1
    if (roundEnded()) {
        status = GameStatus.END_OF_ROUND
        calculateResult()
    }
}

fun Game.putCoin(): Boolean {
    val currentPlayer = currentPlayer()
    if (currentPlayer.coins < 1) {
        return false
    }
    currentPlayer.coins = currentPlayer.coins - 1
    currentCardCoins += 1
    setNextPlayer()
    return true
}

fun Game.setNextPlayer() {
    val newPlayerNumber = (currentPlayerNumber() + 1) / players.size
    currentPlayer = players[newPlayerNumber].id
}

fun Game.startGame(defaultCoinsCount: Int) {
    status = GameStatus.STARTED
    round += 1
    players.forEach {
        it.cards.clear()
        it.coins = defaultCoinsCount
    }
}

fun Game.calculateResult() {
    players.forEach {
        val score = calculateCards(it.cards) - it.coins
        it.score.add(score)
    }
}

fun calculateCards(cards: Set<Int>): Int {
    var lastCard = -1
    var score = 0
    for(card in cards.toList().sorted()) {
        if (lastCard != card - 1) {
            score += card
        }
        lastCard = card
    }
    return score
}

fun Game.getResult(): Map<Int, Score> {
    return players.associate {
        numberById(it.id) to Score(
            playerName = it.name,
            rounds = it.score,
            totalScore = it.score.sum(),
            lastRoundScore = it.score.last()
        )
    }
}

fun Game.numberById(playerId: UUID): Int = players.indexOfFirst { it.id == playerId }

fun Game.currentCardNumber() = deck.cards[deck.skip]

fun Game.isPLayerCurrent(playerId: UUID): Boolean = currentPlayer().id == playerId

fun Game.isStarted(): Boolean = status == GameStatus.STARTED

fun Game.canBeStarted(): Boolean = status == GameStatus.CREATED || status == GameStatus.END_OF_ROUND

fun Game.roundEnded(): Boolean = deck.skip > deck.cards.size

fun Game.currentPlayer() = players.first { player -> player.id == currentPlayer }

fun Game.currentPlayerNumber() = numberById(currentPlayer().id)