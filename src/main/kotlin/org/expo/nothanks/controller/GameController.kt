package org.expo.nothanks.controller


import org.expo.nothanks.service.GameService
import org.springframework.stereotype.Controller


@Controller
class GameController(
    val gameService: GameService
) {

}