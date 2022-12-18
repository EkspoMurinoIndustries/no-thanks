package org.expo.nothanks.controller


import mu.KLogging
import org.expo.nothanks.exception.NoThanksException
import org.expo.nothanks.model.event.input.GameChangingMessage
import org.expo.nothanks.model.event.input.PlayerTurnMessage
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
    fun round(@DestinationVariable gameId: UUID, @Payload message: GameChangingMessage, principal: Principal) {
        try {
            if (message.newParams != null) {
                gamesService.changeParams(gameId, principal.getPlayerId(), message.newParams) {
                    notificationService.paramsChanged(it)
                }
            }
            if (message.scoreReset) {
                gamesService.resetHistory(gameId, principal.getPlayerId()) {
                    notificationService.scoreReset(it)
                }
            }
            if (message.wantToStart) {
                gamesService.startNewRound(gameId, principal.getPlayerId()) {
                    notificationService.gameStarted(it)
                }
            }
        } catch (e: NoThanksException) {
            logger.error("startNewRound error", e)
            notificationService.sendErrorToUser(
                gameId,
                principal.getPlayerId(),
                e.publicMessage
            )
        }
    }

    @MessageMapping("/lobby/input/{gameId}/turn")
    fun turn(@DestinationVariable gameId: UUID, @Payload message: PlayerTurnMessage, principal: Principal) {
        val playerId = principal.getPlayerId()
        try {
            if (message.action == "putCoin") {
                gamesService.putCoin(gameId, playerId) {
                    notificationService.putCoin(it)
                    notificationService.updateInfoForPrevious(it)
                }
            } else {
                gamesService.takeCard(gameId, playerId) {
                    if (it.isGameStarted()) {
                        notificationService.takeCard(it)
                        notificationService.updateInfo(it)
                    } else {
                        notificationService.endRound(it)
                    }
                }
            }
        } catch (e: NoThanksException) {
            logger.error("Action error", e)
            notificationService.sendErrorToUser(gameId, playerId, e.publicMessage)
        }
    }

    private companion object : KLogging()
}