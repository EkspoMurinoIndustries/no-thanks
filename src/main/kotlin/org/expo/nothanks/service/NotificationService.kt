package org.expo.nothanks.service

import mu.KLogging
import org.expo.nothanks.model.event.output.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class NotificationService(
    val simpMessagingTemplate: SimpMessagingTemplate,
    val gamesService: GamesService
) {

    fun connectionNotify(gameId: UUID, playerName: String) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            UserConnectedMessage(
                newPlayerName = playerName,
                allPlayers = gamesService.getPlayersNames(gameId)
            )
        )
    }

    fun gameStarted(gameId: UUID) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            RoundStartedMessage(
                eachPlayerCoinCount = 9,
                currentPlayerNumber = 0
            )
        )
    }

    fun putCoin(gameId: UUID, playerNumber: Int, newPlayerNumber: Int) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            PutCoinMessage(
                playerNumber = playerNumber,
                newCurrentPlayerNumber = newPlayerNumber
            )
        )
    }

    fun takeCard(gameId: UUID, playerNumber: Int, newCardNumber: Int) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            TakeCardMessage(
                playerNumber = playerNumber,
                newCardNumber = newCardNumber
            )
        )
    }

    fun endRound(gameId: UUID, result: Map<Int, Score>) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            EndRoundMessage(
                result = result
            )
        )
    }


    fun messageToUser(gameId: UUID, playerId: UUID, message: OutputMessage) {
        logger.info { "Send message with type ${message.type} to $playerId" }
        simpMessagingTemplate.convertAndSendToUser(playerId.toString(), "/lobby/${gameId}/player", message)
    }

    private companion object : KLogging()

}