package org.expo.nothanks.controller

import mu.KLogging
import org.expo.nothanks.exception.SomethingWentWrong
import org.expo.nothanks.model.*
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/api/")
class ApplicationController() {

    @PostMapping("/authorization")
    fun authorization(
        @RequestBody authorizationRequest: AuthorizationRequest,
        response: HttpServletResponse
    ): UserInfo {
        var playerName = authorizationRequest.name
        if (playerName.isBlank()) {
            throw SomethingWentWrong("Name is blank")
        }
        val token = UUID.randomUUID()
        val c1 = Cookie("no-thanks-token", token.toString()).also {
            it.maxAge = Int.MAX_VALUE
            it.path = "/"
        }

        if (playerName.length > 12) {
            playerName = playerName.substring(0, 12)
        }
        val c2 = Cookie("no-thanks-name", playerName).also {
            it.maxAge = Int.MAX_VALUE
            it.path = "/"
        }
        response.addCookie(c1)
        response.addCookie(c2)
        return UserInfo(token = token)
    }

    private companion object : KLogging()
}