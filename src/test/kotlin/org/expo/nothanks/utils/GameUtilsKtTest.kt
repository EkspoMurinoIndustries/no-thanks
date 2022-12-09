package org.expo.nothanks.utils

import org.expo.nothanks.model.game.Deck
import org.expo.nothanks.model.game.Game
import org.expo.nothanks.model.game.Player
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*

internal class GameUtilsKtTest {

    @Test
    fun playerSequence() {
        val player1 = generatePlayer(1)
        val player2 = generatePlayer(2)
        val player3 = generatePlayer(3)
        player1.nextPlayer = player2
        player2.nextPlayer = player3
        player3.nextPlayer = player1
        val game = Game(
            playerCount = 3,
            currentPlayer = player1,
            deck = Deck(emptyList()),
            inviteCode = "",
            id = UUID.randomUUID()
        )
        val players = game.playerSequence().map { it.number }.toList()
        assertEquals(3, players.size)
        assertEquals(1, players[0])
        assertEquals(2, players[1])
        assertEquals(3, players[2])
    }

    private fun generatePlayer(i: Int): Player {
        return Player(
            number = i,
            coins = 10,
            id = UUID.randomUUID()
        )
    }
}

