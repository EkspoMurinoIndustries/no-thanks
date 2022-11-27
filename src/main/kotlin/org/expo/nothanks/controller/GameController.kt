package org.expo.nothanks.controller


import mu.KLogging
import org.expo.nothanks.model.event.input.StartNewRoundMessage
import org.expo.nothanks.model.event.output.ErrorMessage
import org.expo.nothanks.model.event.output.PlayerTurnMessage
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
            val playerNumber = gamesService.getGame(gameId).currentPlayerNumber()
            if (message.action == "putCoin") {
                val newPlayerNumber = gamesService.putCoin(gameId, playerId).currentPlayerNumber()
                notificationService.putCoin(gameId, playerNumber, newPlayerNumber)
            } else {
                val game = gamesService.takeCard(gameId, playerId)
                if (game.roundEnded()) {
                    notificationService.endRound(gameId, game.getResult())
                } else {
                    notificationService.takeCard(gameId, playerNumber, game.currentCardNumber())
                }
            }
        } catch (e: Exception) {
            logger.error("action error", e)
            notificationService.messageToUser(gameId, playerId, ErrorMessage(e.message ?: "Unexpected error"))
        }
    }

    private companion object : KLogging()
}