package org.expo.nothanks.controller


import mu.KLogging
import org.expo.nothanks.exception.NoThanksException
import org.expo.nothanks.model.event.input.ChangeNameMessage
import org.expo.nothanks.model.event.output.UserConnectedMessage
import org.expo.nothanks.model.event.input.ConnectToGameMessage
import org.expo.nothanks.model.event.input.GameChangingMessage
import org.expo.nothanks.model.event.input.PlayerTurnMessage
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.*
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.*


@Controller
class GameController(
    val gamesService: GamesService,
    val notificationService: NotificationService
) {

    @MessageMapping("/lobby/input/connect")
    @SendToUser("/lobby/info")
    fun gameManager(@Payload message: ConnectToGameMessage, principal: Principal): Any {
        val token = principal.getPlayerId()
        val gameId = if (message.createGame) {
            gamesService.createLobby(token).gameId
        } else {
            gamesService.gameIdByInviteCode(message.inviteCode ?: throw NoThanksException("Empty invite code"))
        }
        lateinit var response: UserConnectedMessage
        gamesService.addPlayerToLobby(gameId, token, message.name) { lobby, newPlayer ->
            if (newPlayer) {
                notificationService.lobbyConnection(lobby, token)
            } else {
                notificationService.reconnect(lobby, token)
            }
            response = UserConnectedMessage(
                isStarted = lobby.isGameStarted(),
                gameId = lobby.gameId,
                isCreator = lobby.creator == token,
                players = lobby.getSafeLobbyPlayers(),
                playerNumber = lobby.players[token]!!.number,
                gameStatus = lobby.gameStatusOrNull(token),
                params = lobby.params,
                inviteCode = lobby.inviteCode
            )
        }
        return response
    }

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
                    notificationService.updateInfo(it)
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

    @MessageMapping("/lobby/input/name")
    fun name(@Payload message: ChangeNameMessage, principal: Principal) {
        val playerId = principal.getPlayerId()
        val gameId = gamesService.gameIdByPlayerId(playerId)
        gamesService.changeGameWithLock(gameId) { lobby ->
            var newPlayerName = message.newName
            if (newPlayerName.length > 12) {
                newPlayerName = newPlayerName.substring(0, 12)
            }
            lobby.changePlayerName(playerId, newPlayerName)
            notificationService.updatePlayerName(lobby, playerId, newPlayerName)
            notificationService.updatePersonalPlayerName(lobby, playerId, newPlayerName)
        }
    }

    private companion object : KLogging()
}