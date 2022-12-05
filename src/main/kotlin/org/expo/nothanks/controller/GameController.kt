package org.expo.nothanks.controller


import mu.KLogging
import org.expo.nothanks.model.event.input.StartNewRoundMessage
import org.expo.nothanks.model.event.output.ErrorMessage
import org.expo.nothanks.model.event.input.PlayerTurnMessage
import org.expo.nothanks.model.event.output.Score
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.*
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.*


@Controller
class GameController(
    val gamesService: GamesService,
    val notificationService: NotificationService
) {
    @MessageMapping("/lobby/input/{gameId}/round")
    fun round(@DestinationVariable gameId: UUID, @Payload message: StartNewRoundMessage, principal: Principal) {
        try {
            gamesService.startNewRound(gameId, principal.getPlayerId())
            notificationService.gameStarted(gameId)
        } catch (e: Exception) {
            logger.error("startNewRound error", e)
            notificationService.messageToUser(
                gameId,
                principal.getPlayerId(),
                ErrorMessage(e.message ?: "Unexpected error")
            )
        }
    }

    @MessageMapping("/lobby/input/{gameId}/turn")
    fun turn(@DestinationVariable gameId: UUID, @Payload message: PlayerTurnMessage, principal: Principal) {
        val playerId = principal.getPlayerId()
        try {
            if (message.action == "putCoin") {
                var newPlayerNumber: Int = -1
                var playerNumber: Int = -1
                gamesService.putCoin(gameId, playerId) {
                    newPlayerNumber = it.getGame().currentPlayerNumber()
                    playerNumber = it.getGame().previousPlayer().number
                }
                notificationService.putCoin(gameId, playerNumber, newPlayerNumber)
            } else {
                var ended = false
                var playerNumber: Int = -1
                var card: Int = -1
                var result: Map<Int, Score>? = null
                gamesService.takeCard(gameId, playerId) {
                    ended = it.isGameStarted()
                    if (it.isGameStarted()) {
                        playerNumber = it.getGame().currentPlayerNumber()
                        card = it.getGame().currentCardNumber()
                    } else {
                        result = it.getResult()
                    }
                }
                if (ended) {
                    notificationService.endRound(gameId, result!!)
                } else {
                    notificationService.takeCard(gameId, playerNumber, card)
                }
            }
        } catch (e: Exception) {
            logger.error("Action error", e)
            notificationService.messageToUser(gameId, playerId, ErrorMessage(e.message ?: "Unexpected error"))
        }
    }

    private companion object : KLogging()
}