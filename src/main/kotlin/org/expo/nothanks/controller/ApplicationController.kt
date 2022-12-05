package org.expo.nothanks.controller

import mu.KLogging
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.model.AuthorizationRequest
import org.expo.nothanks.model.CreateGameResponse
import org.expo.nothanks.model.UserInfo
import org.expo.nothanks.model.ConnectRequest
import org.expo.nothanks.model.UserConnectedStatus
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.playerNames
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/")
class ApplicationController(
    val gamesService: GamesService,
    val notificationService: NotificationService
    ) {

    @PostMapping("/game/create")
    fun createGame(
        @CookieValue(value = "\${no-thanks.cookies.id}") token: UUID,
        @CookieValue(value = "\${no-thanks.cookies.name}") name: String,
    ): CreateGameResponse {
        val lobby = gamesService.createLobby(token)
        gamesService.addPlayerToLobby(lobby.inviteCode, token, name)
        return CreateGameResponse(gameId = lobby.gameId, inviteCode = lobby.inviteCode)
    }

    @PostMapping("/game/connect")
    fun connectGame(
        @RequestBody connectRequest: ConnectRequest,
        @CookieValue(value = "\${no-thanks.cookies.id}") token: UUID,
        @CookieValue(value = "\${no-thanks.cookies.name}") name: String,
    ): UserConnectedStatus {
        try {
            gamesService.addPlayerToLobby(connectRequest.inviteCode, token, name)
        } catch (e: Exception) {
            logger.error("Add player error", e)
            return inviteCodeError()
        }
        val gameId = gamesService.gameIdByInviteCode(connectRequest.inviteCode) ?: return inviteCodeError()
        return gamesService.readGameWithLock(gameId) { lobby ->
            notificationService.connectionNotify(lobby.gameId, name)
            UserConnectedStatus(
                status = "SUCCESS",
                gameId = lobby.gameId,
                isCreator = lobby.creator == token,
                allPlayers = lobby.playerNames()
            )
        } ?: inviteCodeError()
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

    private fun inviteCodeError(): UserConnectedStatus {
        return UserConnectedStatus(
            status = "FAILED",
            errorMessage = "inviteCode does not exist"
        )
    }

    private companion object : KLogging()
}