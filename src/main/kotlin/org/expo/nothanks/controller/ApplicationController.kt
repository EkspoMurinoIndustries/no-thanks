package org.expo.nothanks.controller

import mu.KLogging
import org.expo.nothanks.exception.SomethingWentWrong
import org.expo.nothanks.model.*
import org.expo.nothanks.service.GamesService
import org.expo.nothanks.service.NotificationService
import org.expo.nothanks.utils.gameStatusOrNull
import org.expo.nothanks.utils.getSafeLobbyPlayers
import org.expo.nothanks.utils.isGameStarted
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
        gamesService.addPlayerToLobby(lobby.gameId, token, name) {}
        return CreateGameResponse(gameId = lobby.gameId, inviteCode = lobby.inviteCode)
    }

    @PostMapping("/game/connect")
    fun connectGame(
        @RequestBody connectRequest: ConnectRequest,
        @CookieValue(value = "\${no-thanks.cookies.id}") token: UUID,
        @CookieValue(value = "\${no-thanks.cookies.name}") name: String,
    ): UserConnectedResponse {
        val gameId = gamesService.gameIdByInviteCode(connectRequest.inviteCode)
        lateinit var response: UserConnectedResponse
        gamesService.addPlayerToLobby(gameId, token, name) { lobby ->
            if (lobby.isGameStarted()) {
                notificationService.gameConnection(lobby, token)
            } else {
                notificationService.lobbyConnection(lobby, token)
            }
            response = UserConnectedResponse(
                isStarted = lobby.isGameStarted(),
                gameId = lobby.gameId,
                isCreator = lobby.creator == token,
                allPlayers = lobby.getSafeLobbyPlayers(),
                playerNumber = lobby.players[token]!!.number,
                gameStatus = lobby.gameStatusOrNull(token),
                params = lobby.params
            )
        }
        return response
    }

    @PostMapping("/authorization")
    fun authorization(
        @RequestBody authorizationRequest: AuthorizationRequest,
        response: HttpServletResponse
    ): UserInfo {
        if (authorizationRequest.name.isBlank()) {
            throw SomethingWentWrong("Name is blank")
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

    private companion object : KLogging()
}