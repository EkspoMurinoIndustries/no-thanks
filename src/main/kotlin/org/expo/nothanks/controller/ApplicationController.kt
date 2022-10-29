package org.expo.nothanks.controller

import org.expo.nothanks.service.GameService
import org.expo.nothanks.model.AuthorizationRequest
import org.expo.nothanks.model.CreateGameRequest
import org.expo.nothanks.model.CreateGameResponse
import org.expo.nothanks.model.UserInfo
import org.expo.nothanks.model.event.input.ConnectRequest
import org.expo.nothanks.model.event.output.UserConnectedStatus
import org.expo.nothanks.service.NotificationService
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/")
class ApplicationController(
    val gameService: GameService,
    val notificationService: NotificationService
    ) {

    @PostMapping("/game/create")
    fun createGame(
        @RequestBody createGameRequest: CreateGameRequest,
        @CookieValue(value = "no-thanks-token") token: UUID,
        @CookieValue(value = "no-thanks-name") name: String,
    ): CreateGameResponse {
        val game = gameService.addGame(createGameRequest, token, name)
        return CreateGameResponse(gameId = game.id, inviteCode = game.inviteCode)
    }

    @PostMapping("/game/connect")
    fun connectGame(
        @RequestBody connectRequest: ConnectRequest,
        @CookieValue(value = "no-thanks-token") token: UUID,
        @CookieValue(value = "no-thanks-name") name: String,
    ): UserConnectedStatus {
        val gameId = gameService.getGameIdByInviteCode(connectRequest.inviteCode)
            ?: return UserConnectedStatus(
                status = "FAILED",
                errorMessage = "inviteCode does not exist"
            )
        gameService.connect(gameId = gameId, playerId = token, userName = name)
        notificationService.connectionNotify(gameId, name)
        return UserConnectedStatus(
            status = "SUCCESS",
            gameId = gameId,
            isCreator = gameService.isCreator(gameId, token),
            allPlayers = gameService.getPlayersNames(gameId)
        )
    }

    @PostMapping("/authorization")
    fun authorization(
        @RequestBody authorizationRequest: AuthorizationRequest,
        response: HttpServletResponse
    ): UserInfo {
        if (authorizationRequest.name.isBlank()) {
            throw IllegalArgumentException("Name is blank")
        }
        val token = UUID.randomUUID()
        val c1 = Cookie("no-thanks-token", token.toString()).also {
            it.maxAge = Int.MAX_VALUE
            it.path = "/"
        }
        val c2 = Cookie("no-thanks-name", authorizationRequest.name).also {
            it.maxAge = Int.MAX_VALUE
            it.path = "/"
        }
        response.addCookie(c1)
        response.addCookie(c2)
        return UserInfo(token = token)
    }

}